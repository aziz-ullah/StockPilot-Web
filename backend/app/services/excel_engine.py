import io
from datetime import datetime, date
from decimal import Decimal
from typing import Dict, Any, List
import openpyxl
from openpyxl.styles import Font, PatternFill, Alignment, Border, Side
from sqlalchemy.orm import Session
from app.models.domain import Category, Product, ProductVariant, Sale, SaleItem, Purchase, PurchaseItem
from app.services.inventory_service import generate_sku, record_stock_movement

def generate_standard_excel_template() -> bytes:
    wb = openpyxl.Workbook()
    ws = wb.active
    ws.title = "Inventory Upload"
    
    headers = [
        "Category", "Product Name", "Color", "Size",
        "Buying Price ($)", "Selling Price ($)", "Stock Quantity",
        "Min Stock Alert", "Is Sold Row (TRUE/FALSE)", "Sale Date (YYYY-MM-DD)"
    ]
    
    header_fill = PatternFill(start_color="1E293B", end_color="1E293B", fill_type="solid")
    header_font = Font(name="Calibri", size=11, bold=True, color="FFFFFF")
    
    ws.append(headers)
    for col_idx, h in enumerate(headers, 1):
        cell = ws.cell(row=1, column=col_idx)
        cell.fill = header_fill
        cell.font = header_font
        cell.alignment = Alignment(horizontal="center", vertical="center")
        
    sample_rows = [
        ["Hoodies", "Black Oversized Hoodie", "Black", "XS", 36.00, 70.00, 10, 5, False, ""],
        ["Hoodies", "Black Oversized Hoodie", "Black", "S", 36.00, 70.00, 15, 5, False, ""],
        ["Pants", "Light Oatmeal Cargo Pant", "Light Oatmeal", "M", 36.00, 65.00, 8, 3, False, ""],
        ["Jackets", "Moose Knuckle Jacket", "Black", "L", 145.00, 240.00, 0, 2, True, "2026-09-01"]
    ]
    
    for row in sample_rows:
        ws.append(row)
        
    for col in ws.columns:
        max_len = max(len(str(cell.value or '')) for cell in col)
        col_letter = openpyxl.utils.get_column_letter(col[0].column)
        ws.column_dimensions[col_letter].width = max(max_len + 3, 15)
        
    output = io.BytesIO()
    wb.save(output)
    output.seek(0)
    return output.getvalue()

def process_excel_import(file_bytes: bytes, db: Session, user_id: int, org_id: int = 1) -> Dict[str, Any]:
    wb = openpyxl.load_workbook(filename=io.BytesIO(file_bytes), data_only=True)
    
    summary = {
        "categories_created": 0,
        "products_created": 0,
        "variants_created": 0,
        "stock_units_added": 0,
        "historical_sales_imported": 0,
        "total_imported_rows": 0,
        "errors": []
    }

    # Category map cache
    category_cache = {c.name.lower(): c for c in db.query(Category).filter(Category.organization_id == org_id).all()}
    product_cache = {p.name.lower(): p for p in db.query(Product).filter(Product.organization_id == org_id).all()}
    variant_cache = {v.sku: v for v in db.query(ProductVariant).filter(ProductVariant.organization_id == org_id).all()}

    def get_or_create_category(cat_name: str) -> Category:
        key = cat_name.strip().lower()
        if key in category_cache:
            return category_cache[key]
        c = Category(organization_id=org_id, name=cat_name.strip())
        db.add(c)
        db.flush()
        category_cache[key] = c
        summary["categories_created"] += 1
        return c

    def get_or_create_product(prod_name: str, category_id: int) -> Product:
        key = prod_name.strip().lower()
        if key in product_cache:
            return product_cache[key]
        p = Product(organization_id=org_id, category_id=category_id, name=prod_name.strip())
        db.add(p)
        db.flush()
        product_cache[key] = p
        summary["products_created"] += 1
        return p

    def parse_decimal(val, default="0.00") -> Decimal:
        if val is None or str(val).strip() == "":
            return Decimal(default)
        try:
            clean = str(val).replace("$", "").replace(",", "").strip()
            return Decimal(clean)
        except Exception:
            return Decimal(default)

    # Process all sheets in the workbook
    for sheet_name in wb.sheetnames:
        ws = wb[sheet_name]
        if ws.max_row < 2:
            continue
            
        # Detect header row (could be row 1 or row 2)
        header_row_idx = 1
        col_map = {}
        
        for r_idx in range(1, min(5, ws.max_row + 1)):
            row_vals = [str(ws.cell(r_idx, c).value or '').strip().lower() for c in range(1, ws.max_column + 1)]
            if any("color" in v or "colour" in v or "product" in v or "size" in v for v in row_vals):
                header_row_idx = r_idx
                for c_idx, val in enumerate(row_vals, 1):
                    if "cat" in val:
                        col_map["category"] = c_idx
                    elif "color" in val or "colour" in val or "product" in val or "item" in val:
                        col_map["product"] = c_idx
                    elif "size" in val:
                        col_map["size"] = c_idx
                    elif "buy" in val or "buying" in val or "cost" in val or "price" in val and "sold" not in val and "selling" not in val:
                        col_map["buying_price"] = c_idx
                    elif "sell" in val or "selling" in val or "sold price" in val:
                        col_map["selling_price"] = c_idx
                    elif "date" in val:
                        col_map["date"] = c_idx
                    elif "qty" in val or "quantity" in val or "stock" in val:
                        col_map["qty"] = c_idx
                break
                
        # Default fallback column mapping if header match incomplete
        if "product" not in col_map:
            col_map["product"] = 2
        if "size" not in col_map:
            col_map["size"] = 3
        if "buying_price" not in col_map:
            col_map["buying_price"] = 4
        if "selling_price" not in col_map:
            col_map["selling_price"] = 5
        if "date" not in col_map:
            col_map["date"] = 7
            
        # Determine category based on sheet name if not in column
        sheet_cat_name = "Tops"
        sn_lower = sheet_name.lower()
        if "hoodie" in sn_lower:
            sheet_cat_name = "Hoodies"
        elif "pant" in sn_lower:
            sheet_cat_name = "Pants"
        elif "jacket" in sn_lower or "moose" in sn_lower:
            sheet_cat_name = "Jackets"

        # Iterate over data rows
        for row_idx in range(header_row_idx + 1, ws.max_row + 1):
            row_vals = [ws.cell(row_idx, c).value for c in range(1, ws.max_column + 1)]
            if not any(row_vals):
                continue
                
            prod_val = str(ws.cell(row_idx, col_map["product"]).value or '').strip()
            if not prod_val or prod_val.lower() in ["total", "s.no", "no", "sno"]:
                continue

            size_val = str(ws.cell(row_idx, col_map["size"]).value or 'M').strip().upper()
            buying_val = parse_decimal(ws.cell(row_idx, col_map["buying_price"]).value)
            selling_val = parse_decimal(ws.cell(row_idx, col_map.get("selling_price", 0)).value) if "selling_price" in col_map else Decimal("0.00")
            date_val = ws.cell(row_idx, col_map.get("date", 0)).value if "date" in col_map else None

            # Color extraction from product name (e.g. "Black Hoodie" -> Color: Black, Product: Hoodie)
            color_val = sheet_name.title()
            name_parts = prod_val.split()
            if len(name_parts) > 1 and name_parts[0].lower() in ["black", "light", "dark", "oatmeal", "navy", "white", "red", "blue", "grey"]:
                color_val = name_parts[0].title()
                if len(name_parts) > 2 and name_parts[1].lower() in ["oatmeal", "oatmale"]:
                    color_val = f"{name_parts[0].title()} Oatmeal"
                    
            cat_obj = get_or_create_category(sheet_cat_name)
            prod_obj = get_or_create_product(prod_val, cat_obj.id)

            # Check if variant exists
            base_sku = generate_sku(prod_obj.name, color_val, size_val, db, org_id)
            variant = db.query(ProductVariant).filter(
                ProductVariant.product_id == prod_obj.id,
                ProductVariant.color == color_val,
                ProductVariant.size == size_val,
                ProductVariant.organization_id == org_id
            ).first()

            if not variant:
                variant = ProductVariant(
                    organization_id=org_id,
                    product_id=prod_obj.id,
                    sku=base_sku,
                    color=color_val,
                    size=size_val,
                    cost_price=buying_val,
                    selling_price=selling_val if selling_val > Decimal("0.00") else buying_val * Decimal("2.0"),
                    stock_quantity=0,
                    min_stock_alert=5
                )
                db.add(variant)
                db.flush()
                summary["variants_created"] += 1

            # Check if this row represents a SOLD item or UNSOLD stock item
            is_sold = False
            if selling_val > Decimal("0.00") and date_val is not None and not str(date_val).startswith("=IF"):
                is_sold = True

            if is_sold:
                # Import historical sale transaction
                sale_datetime = date_val if isinstance(date_val, datetime) else datetime.now()
                inv_num = f"INV-HIST-{int(datetime.now().timestamp())}-{summary['historical_sales_imported']+1}"
                
                cogs = buying_val
                revenue = selling_val
                profit = revenue - cogs
                
                sale = Sale(
                    organization_id=org_id,
                    invoice_number=inv_num,
                    customer_name="Historical Customer",
                    sale_date=sale_datetime,
                    payment_method="Cash",
                    status="Completed",
                    subtotal=revenue,
                    discount=Decimal("0.00"),
                    tax=Decimal("0.00"),
                    total_amount=revenue,
                    total_cogs=cogs,
                    gross_profit=profit,
                    notes=f"Imported from Excel sheet {sheet_name}",
                    created_by_user_id=user_id
                )
                db.add(sale)
                db.flush()
                
                sale_item = SaleItem(
                    sale_id=sale.id,
                    variant_id=variant.id,
                    quantity=1,
                    unit_cost=cogs,
                    unit_price=revenue,
                    total_cogs=cogs,
                    total_price=revenue
                )
                db.add(sale_item)
                summary["historical_sales_imported"] += 1
            else:
                # Add unsold item to stock
                record_stock_movement(
                    db=db,
                    variant_id=variant.id,
                    movement_type="IMPORT",
                    qty_change=1,
                    reference_id=f"EXCEL-{sheet_name}",
                    notes=f"Imported unsold piece from {sheet_name}",
                    user_id=user_id,
                    org_id=org_id
                )
                summary["stock_units_added"] += 1

            summary["total_imported_rows"] += 1

    db.commit()
    return summary
