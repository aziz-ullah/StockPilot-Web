export interface User {
  id: number;
  email: string;
  username: string;
  full_name: string;
  role: 'Admin' | 'Manager' | 'Staff';
  is_active: boolean;
  created_at: string;
}

export interface Category {
  id: number;
  name: string;
  description?: string;
}

export interface Variant {
  id: number;
  product_id: number;
  product_name?: string;
  category_name?: string;
  sku: string;
  color: string;
  size: string;
  cost_price: number;
  selling_price: number;
  stock_quantity: number;
  min_stock_alert: number;
  created_at: string;
}

export interface Product {
  id: number;
  category_id: number;
  category_name?: string;
  name: string;
  description?: string;
  variants: Variant[];
  created_at: string;
}

export interface StockMovement {
  id: number;
  variant_id: number;
  sku?: string;
  product_name?: string;
  color?: string;
  size?: string;
  movement_type: string;
  quantity: number;
  previous_stock: number;
  new_stock: number;
  reference_id?: string;
  notes?: string;
  created_at: string;
  created_by_name?: string;
}

export interface Supplier {
  id: number;
  name: string;
  contact_person?: string;
  phone?: string;
  email?: string;
  address?: string;
}

export interface PurchaseItem {
  id: number;
  variant_id: number;
  product_name?: string;
  color?: string;
  size?: string;
  sku?: string;
  quantity: number;
  unit_cost: number;
  total_cost: number;
}

export interface Purchase {
  id: number;
  purchase_number: string;
  supplier_id?: number;
  supplier_name?: string;
  purchase_date: string;
  status: 'Completed' | 'Cancelled';
  payment_status: 'Paid' | 'Pending';
  total_amount: number;
  notes?: string;
  items: PurchaseItem[];
  created_at: string;
  created_by_name?: string;
}

export interface SaleItem {
  id: number;
  variant_id: number;
  product_name?: string;
  color?: string;
  size?: string;
  sku?: string;
  quantity: number;
  unit_cost: number;
  unit_price: number;
  total_cogs: number;
  total_price: number;
}

export interface Sale {
  id: number;
  invoice_number: string;
  customer_name?: string;
  customer_phone?: string;
  sale_date: string;
  payment_method: string;
  status: 'Completed' | 'Voided';
  subtotal: number;
  discount: number;
  tax: number;
  total_amount: number;
  total_cogs: number;
  gross_profit: number;
  notes?: string;
  items: SaleItem[];
  created_at: string;
  created_by_name?: string;
}

export interface Expense {
  id: number;
  category: string;
  description: string;
  amount: number;
  expense_date: string;
  created_at: string;
  created_by_name?: string;
}

export interface DashboardKPIs {
  total_sales_revenue: number;
  total_cogs: number;
  gross_profit: number;
  total_expenses: number;
  net_profit: number;
  current_stock_valuation: number;
  low_stock_items_count: number;
}

export interface TrendChartPoint {
  date: string;
  sales: number;
  gross_profit: number;
  net_profit: number;
}

export interface CategorySalesPoint {
  category_name: string;
  sales: number;
  quantity_sold: number;
}

export interface LowStockItem {
  variant_id: number;
  product_name: string;
  category_name: string;
  sku: string;
  color: string;
  size: string;
  stock_quantity: number;
  min_stock_alert: number;
  status_badge: 'OUT_OF_STOCK' | 'LOW_STOCK';
}

export interface ImportSummary {
  categories_created: number;
  products_created: number;
  variants_created: number;
  stock_units_added: number;
  historical_sales_imported: number;
  total_imported_rows: number;
  errors: string[];
}
