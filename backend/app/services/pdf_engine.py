import io
from decimal import Decimal
from reportlab.lib.pagesizes import letter
from reportlab.lib import colors
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, HRFlowable
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from app.models.domain import Sale

def generate_sales_receipt_pdf(sale: Sale) -> bytes:
    buffer = io.BytesIO()
    doc = SimpleDocTemplate(
        buffer,
        pagesize=letter,
        rightMargin=36,
        leftMargin=36,
        topMargin=36,
        bottomMargin=36
    )

    styles = getSampleStyleSheet()
    
    title_style = ParagraphStyle(
        'DocTitle',
        parent=styles['Heading1'],
        fontName='Helvetica-Bold',
        fontSize=20,
        leading=24,
        textColor=colors.HexColor('#0F172A')
    )
    
    subtitle_style = ParagraphStyle(
        'DocSubtitle',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=10,
        leading=14,
        textColor=colors.HexColor('#475569')
    )

    body_bold = ParagraphStyle(
        'BodyBold',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=10,
        leading=14,
        textColor=colors.HexColor('#0F172A')
    )

    body_normal = ParagraphStyle(
        'BodyNormal',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=10,
        leading=14,
        textColor=colors.HexColor('#334155')
    )

    elements = []

    # Header section
    header_data = [
        [
            Paragraph("<b>STOCKPILOT APPAREL</b><br/><font size=9 color='#64748B'>123 Fashion Ave, Suite 500<br/>Phone: (555) 019-2831 | Email: billing@stockpilot.com</font>", title_style),
            Paragraph(f"<font size=16 color='#0F172A'><b>RECEIPT / INVOICE</b></font><br/><br/><b>Invoice #:</b> {sale.invoice_number}<br/><b>Date:</b> {sale.sale_date.strftime('%Y-%m-%d %H:%M')}<br/><b>Status:</b> {sale.status.upper()}", subtitle_style)
        ]
    ]
    header_table = Table(header_data, colWidths=[300, 240])
    header_table.setStyle(TableStyle([
        ('VALIGN', (0, 0), (-1, -1), 'TOP'),
        ('ALIGN', (1, 0), (1, 0), 'RIGHT')
    ]))
    elements.append(header_table)
    elements.append(Spacer(1, 15))
    elements.append(HRFlowable(width="100%", thickness=1, color=colors.HexColor("#E2E8F0"), spaceAfter=15))

    # Customer info
    cust_name = sale.customer_name or "Walk-in Customer"
    cust_phone = sale.customer_phone or "N/A"
    pay_method = sale.payment_method or "Cash"

    cust_data = [
        [
            Paragraph(f"<b>Billed To:</b><br/>{cust_name}<br/>Phone: {cust_phone}", body_normal),
            Paragraph(f"<b>Payment Details:</b><br/>Payment Method: {pay_method}<br/>Issued By: {sale.created_by.full_name if sale.created_by else 'System Administrator'}", body_normal)
        ]
    ]
    cust_table = Table(cust_data, colWidths=[270, 270])
    cust_table.setStyle(TableStyle([('VALIGN', (0, 0), (-1, -1), 'TOP')]))
    elements.append(cust_table)
    elements.append(Spacer(1, 15))

    # Items table
    table_headers = [
        Paragraph("<b>#</b>", body_bold),
        Paragraph("<b>Product Description</b>", body_bold),
        Paragraph("<b>SKU</b>", body_bold),
        Paragraph("<b>Qty</b>", body_bold),
        Paragraph("<b>Unit Price ($)</b>", body_bold),
        Paragraph("<b>Total ($)</b>", body_bold)
    ]
    
    items_data = [table_headers]
    
    for idx, item in enumerate(sale.items, 1):
        v = item.variant
        p_name = v.product.name if v and v.product else "Item"
        color = v.color if v else ""
        size = v.size if v else ""
        sku = v.sku if v else "SKU"
        
        desc = f"{p_name} ({color}, Size {size})"
        items_data.append([
            Paragraph(str(idx), body_normal),
            Paragraph(desc, body_normal),
            Paragraph(sku, body_normal),
            Paragraph(str(item.quantity), body_normal),
            Paragraph(f"${item.unit_price:.2f}", body_normal),
            Paragraph(f"${item.total_price:.2f}", body_normal)
        ])

    items_table = Table(items_data, colWidths=[30, 220, 100, 40, 75, 75])
    items_table.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor('#F8FAFC')),
        ('TEXTCOLOR', (0, 0), (-1, 0), colors.HexColor('#0F172A')),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 8),
        ('TOPPADDING', (0, 0), (-1, -1), 8),
        ('GRID', (0, 0), (-1, -1), 0.5, colors.HexColor('#E2E8F0')),
        ('ALIGN', (3, 0), (-1, -1), 'RIGHT')
    ]))
    elements.append(items_table)
    elements.append(Spacer(1, 15))

    # Summary section
    subtotal = sale.subtotal or Decimal("0.00")
    discount = sale.discount or Decimal("0.00")
    tax = sale.tax or Decimal("0.00")
    total = sale.total_amount or Decimal("0.00")

    summary_data = [
        [Paragraph("Subtotal:", body_normal), Paragraph(f"${subtotal:.2f}", body_normal)],
        [Paragraph("Discount:", body_normal), Paragraph(f"-${discount:.2f}", body_normal)],
        [Paragraph("Tax:", body_normal), Paragraph(f"${tax:.2f}", body_normal)],
        [Paragraph("<b>Grand Total:</b>", body_bold), Paragraph(f"<b>${total:.2f}</b>", body_bold)]
    ]
    
    summary_table = Table(summary_data, colWidths=[120, 100])
    summary_table.setStyle(TableStyle([
        ('ALIGN', (0, 0), (-1, -1), 'RIGHT'),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 4),
        ('TOPPADDING', (0, 0), (-1, -1), 4),
    ]))
    
    wrapper_table = Table([[Paragraph("", body_normal), summary_table]], colWidths=[320, 220])
    elements.append(wrapper_table)
    
    elements.append(Spacer(1, 30))
    elements.append(HRFlowable(width="100%", thickness=1, color=colors.HexColor("#E2E8F0"), spaceAfter=15))
    elements.append(Paragraph("<font size=9 color='#64748B' align='center'>Thank you for shopping with StockPilot Apparel! Please retain this receipt for any returns or exchanges within 30 days.</font>", subtitle_style))

    doc.build(elements)
    buffer.seek(0)
    return buffer.getvalue()
