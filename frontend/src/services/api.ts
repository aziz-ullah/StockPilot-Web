import axios from 'axios';
import {
  User, Category, Product, Variant, StockMovement, Supplier,
  Purchase, Sale, Expense, DashboardKPIs, TrendChartPoint,
  CategorySalesPoint, LowStockItem, ImportSummary
} from '../types';

const API_BASE = '/api/v1';

export const api = axios.create({
  baseURL: API_BASE,
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('stockpilot_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

const triggerBlobDownload = (blobData: any, filename: string) => {
  const url = window.URL.createObjectURL(new Blob([blobData]));
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', filename);
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
};

export const authService = {
  login: async (username_or_email: string, password: string) => {
    const res = await api.post('/auth/login', { username_or_email, password });
    return res.data;
  },
  getMe: async (): Promise<User> => {
    const res = await api.get('/auth/me');
    return res.data;
  },
  listUsers: async (): Promise<User[]> => {
    const res = await api.get('/auth/users');
    return res.data;
  },
  createUser: async (data: { email: string; username: string; password: string; full_name: string; role: string }): Promise<User> => {
    const res = await api.post('/auth/users', data);
    return res.data;
  },
  updateUserStatus: async (userId: number, role?: string, is_active?: boolean): Promise<User> => {
    const res = await api.put(`/auth/users/${userId}`, null, { params: { role, is_active } });
    return res.data;
  }
};

export const categoryService = {
  list: async (): Promise<Category[]> => {
    const res = await api.get('/categories');
    return res.data;
  },
  create: async (name: string, description?: string): Promise<Category> => {
    const res = await api.post('/categories', { name, description });
    return res.data;
  }
};

export const productService = {
  listProducts: async (category_id?: number, search?: string): Promise<Product[]> => {
    const res = await api.get('/products', { params: { category_id, search } });
    return res.data;
  },
  listVariants: async (search?: string, low_stock_only?: boolean): Promise<Variant[]> => {
    const res = await api.get('/products/variants', { params: { search, low_stock_only } });
    return res.data;
  },
  createProduct: async (data: { category_id: number; name: string; description?: string; variants: any[] }): Promise<Product> => {
    const res = await api.post('/products', data);
    return res.data;
  },
  updateVariant: async (variantId: number, data: Partial<Variant>): Promise<Variant> => {
    const res = await api.put(`/products/variants/${variantId}`, data);
    return res.data;
  },
  listMovements: async (): Promise<StockMovement[]> => {
    const res = await api.get('/products/movements');
    return res.data;
  }
};

export const purchaseService = {
  listSuppliers: async (): Promise<Supplier[]> => {
    const res = await api.get('/purchases/suppliers');
    return res.data;
  },
  createSupplier: async (data: Partial<Supplier>): Promise<Supplier> => {
    const res = await api.post('/purchases/suppliers', data);
    return res.data;
  },
  listPurchases: async (): Promise<Purchase[]> => {
    const res = await api.get('/purchases');
    return res.data;
  },
  createPurchase: async (data: { supplier_name?: string; payment_status: string; notes?: string; items: any[] }): Promise<Purchase> => {
    const res = await api.post('/purchases', data);
    return res.data;
  },
  cancelPurchase: async (id: number): Promise<Purchase> => {
    const res = await api.post(`/purchases/${id}/cancel`);
    return res.data;
  }
};

export const saleService = {
  listSales: async (): Promise<Sale[]> => {
    const res = await api.get('/sales');
    return res.data;
  },
  createSale: async (data: { customer_name?: string; customer_phone?: string; payment_method: string; discount?: number; tax?: number; items: any[] }): Promise<Sale> => {
    const res = await api.post('/sales', data);
    return res.data;
  },
  voidSale: async (id: number): Promise<Sale> => {
    const res = await api.post(`/sales/${id}/void`);
    return res.data;
  },
  downloadPdf: async (saleId: number, invoiceNum: string) => {
    const res = await api.get(`/sales/${saleId}/pdf`, { responseType: 'blob' });
    triggerBlobDownload(res.data, `Invoice_${invoiceNum}.pdf`);
  }
};

export const expenseService = {
  getCategories: async (): Promise<string[]> => {
    const res = await api.get('/expenses/categories');
    return res.data;
  },
  listExpenses: async (category?: string, start_date?: string, end_date?: string): Promise<Expense[]> => {
    const res = await api.get('/expenses', { params: { category, start_date, end_date } });
    return res.data;
  },
  createExpense: async (data: { category: string; description: string; amount: number; expense_date?: string }): Promise<Expense> => {
    const res = await api.post('/expenses', data);
    return res.data;
  }
};

export const dashboardService = {
  getKPIs: async (preset: string = 'all_time', custom_start?: string, custom_end?: string): Promise<DashboardKPIs> => {
    const res = await api.get('/dashboard/kpis', { params: { preset, custom_start, custom_end } });
    return res.data;
  },
  getTrends: async (preset: string = 'all_time', custom_start?: string, custom_end?: string): Promise<TrendChartPoint[]> => {
    const res = await api.get('/dashboard/charts/trends', { params: { preset, custom_start, custom_end } });
    return res.data;
  },
  getCategorySales: async (preset: string = 'all_time', custom_start?: string, custom_end?: string): Promise<CategorySalesPoint[]> => {
    const res = await api.get('/dashboard/charts/categories', { params: { preset, custom_start, custom_end } });
    return res.data;
  },
  getLowStock: async (): Promise<LowStockItem[]> => {
    const res = await api.get('/dashboard/low-stock');
    return res.data;
  }
};

export const excelService = {
  downloadTemplate: async () => {
    const res = await api.get('/excel/template', { responseType: 'blob' });
    triggerBlobDownload(res.data, 'StockPilot_Inventory_Import_Template.xlsx');
  },
  importExcel: async (file: File): Promise<{ message: string; summary: ImportSummary }> => {
    const formData = new FormData();
    formData.append('file', file);
    const res = await api.post('/excel/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
    return res.data;
  }
};

export const reportService = {
  exportExcel: async (type: string) => {
    const res = await api.get('/reports/export/excel', {
      params: { report_type: type },
      responseType: 'blob'
    });
    triggerBlobDownload(res.data, `StockPilot_${type.toUpperCase()}_Report.xlsx`);
  },
  getPnL: async (start_date?: string, end_date?: string) => {
    const res = await api.get('/reports/pnl', { params: { start_date, end_date } });
    return res.data;
  }
};
