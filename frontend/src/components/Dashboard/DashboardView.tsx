import React, { useState, useEffect } from 'react';
import { dashboardService } from '../../services/api';
import { DashboardKPIs, TrendChartPoint, CategorySalesPoint, LowStockItem } from '../../types';
import {
  DollarSign, TrendingUp, Receipt, Wallet, PackageCheck, AlertTriangle, Calendar, ShoppingBag
} from 'lucide-react';
import {
  ResponsiveContainer, LineChart, Line, BarChart, Bar, XAxis, YAxis, Tooltip, Legend, CartesianGrid
} from 'recharts';

export const DashboardView: React.FC = () => {
  const [preset, setPreset] = useState<string>('all_time');
  const [customStart, setCustomStart] = useState<string>('');
  const [customEnd, setCustomEnd] = useState<string>('');

  const [kpis, setKpis] = useState<DashboardKPIs | null>(null);
  const [trends, setTrends] = useState<TrendChartPoint[]>([]);
  const [categorySales, setCategorySales] = useState<CategorySalesPoint[]>([]);
  const [lowStockItems, setLowStockItems] = useState<LowStockItem[]>([]);
  const [loading, setLoading] = useState<boolean>(true);

  const fetchDashboardData = async () => {
    setLoading(true);
    try {
      const [kpiData, trendData, catData, lowStockData] = await Promise.all([
        dashboardService.getKPIs(preset, customStart || undefined, customEnd || undefined),
        dashboardService.getTrends(preset, customStart || undefined, customEnd || undefined),
        dashboardService.getCategorySales(preset, customStart || undefined, customEnd || undefined),
        dashboardService.getLowStock()
      ]);
      setKpis(kpiData);
      setTrends(trendData);
      setCategorySales(catData);
      setLowStockItems(lowStockData);
    } catch (err) {
      console.error("Error loading dashboard data:", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDashboardData();
  }, [preset, customStart, customEnd]);

  const presetLabels: Record<string, string> = {
    today: 'Today',
    this_week: 'This Week',
    this_month: 'This Month',
    all_time: 'All Time',
    custom: 'Custom Range'
  };

  return (
    <div className="space-y-6">
      {/* Top Filter Bar */}
      <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm flex flex-wrap items-center justify-between gap-4">
        <div className="flex items-center gap-2">
          <Calendar className="w-5 h-5 text-slate-500" />
          <span className="text-sm font-semibold text-slate-700">Filter Period:</span>
          <div className="inline-flex bg-slate-100 p-1 rounded-lg border border-slate-200">
            {['today', 'this_week', 'this_month', 'all_time', 'custom'].map((key) => (
              <button
                key={key}
                onClick={() => setPreset(key)}
                className={`px-3 py-1.5 rounded-md text-xs font-semibold transition-all ${
                  preset === key
                    ? 'bg-white text-emerald-700 shadow-sm border border-slate-200'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                {presetLabels[key]}
              </button>
            ))}
          </div>
        </div>

        {preset === 'custom' && (
          <div className="flex items-center gap-2">
            <input
              type="date"
              value={customStart}
              onChange={(e) => setCustomStart(e.target.value)}
              className="border border-slate-300 rounded-lg px-3 py-1 text-xs focus:ring-2 focus:ring-emerald-500 outline-none"
            />
            <span className="text-xs text-slate-400">to</span>
            <input
              type="date"
              value={customEnd}
              onChange={(e) => setCustomEnd(e.target.value)}
              className="border border-slate-300 rounded-lg px-3 py-1 text-xs focus:ring-2 focus:ring-emerald-500 outline-none"
            />
          </div>
        )}
      </div>

      {/* KPI Cards Grid */}
      {kpis && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-4">
          {/* Total Revenue */}
          <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm relative overflow-hidden">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-slate-500 uppercase">Total Sales</span>
              <div className="w-8 h-8 rounded-lg bg-blue-50 text-blue-600 flex items-center justify-center">
                <DollarSign className="w-4 h-4" />
              </div>
            </div>
            <p className="text-xl font-bold text-slate-900 mt-2">${Number(kpis.total_sales_revenue).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</p>
            <p className="text-[11px] text-slate-400 mt-1">Gross Revenue</p>
          </div>

          {/* Gross Profit */}
          <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm relative overflow-hidden">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-slate-500 uppercase">Gross Profit</span>
              <div className="w-8 h-8 rounded-lg bg-emerald-50 text-emerald-600 flex items-center justify-center">
                <TrendingUp className="w-4 h-4" />
              </div>
            </div>
            <p className="text-xl font-bold text-emerald-600 mt-2">${Number(kpis.gross_profit).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</p>
            <p className="text-[11px] text-slate-400 mt-1">Revenue - COGS</p>
          </div>

          {/* Operating Expenses */}
          <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm relative overflow-hidden">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-slate-500 uppercase">Operating Expenses</span>
              <div className="w-8 h-8 rounded-lg bg-amber-50 text-amber-600 flex items-center justify-center">
                <Receipt className="w-4 h-4" />
              </div>
            </div>
            <p className="text-xl font-bold text-amber-600 mt-2">${Number(kpis.total_expenses).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</p>
            <p className="text-[11px] text-slate-400 mt-1">Rent, Salaries, Ads, etc.</p>
          </div>

          {/* Net Profit */}
          <div className={`p-4 rounded-xl border shadow-sm relative overflow-hidden ${
            kpis.net_profit >= 0 ? 'bg-emerald-900 text-white border-emerald-800' : 'bg-rose-900 text-white border-rose-800'
          }`}>
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-emerald-200 uppercase">Net Profit</span>
              <div className="w-8 h-8 rounded-lg bg-white/10 flex items-center justify-center text-white">
                <Wallet className="w-4 h-4" />
              </div>
            </div>
            <p className="text-2xl font-black mt-2">${Number(kpis.net_profit).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</p>
            <p className="text-[11px] text-emerald-200/80 mt-1">Gross Profit - Expenses</p>
          </div>

          {/* Stock Valuation */}
          <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm relative overflow-hidden">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-slate-500 uppercase">Stock Valuation</span>
              <div className="w-8 h-8 rounded-lg bg-indigo-50 text-indigo-600 flex items-center justify-center">
                <PackageCheck className="w-4 h-4" />
              </div>
            </div>
            <p className="text-xl font-bold text-slate-900 mt-2">${Number(kpis.current_stock_valuation).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</p>
            <p className="text-[11px] text-slate-400 mt-1">Inventory Asset Value</p>
          </div>

          {/* Low Stock Alerts */}
          <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm relative overflow-hidden">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-slate-500 uppercase">Low Stock Alert</span>
              <div className="w-8 h-8 rounded-lg bg-rose-50 text-rose-600 flex items-center justify-center">
                <AlertTriangle className="w-4 h-4" />
              </div>
            </div>
            <p className="text-xl font-bold text-rose-600 mt-2">{kpis.low_stock_items_count} Items</p>
            <p className="text-[11px] text-slate-400 mt-1">Below Min Threshold</p>
          </div>
        </div>
      )}

      {/* Analytics Charts Row */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Sales & Profit Trends Line Chart */}
        <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-sm">
          <h3 className="font-bold text-slate-800 text-base mb-4 flex items-center gap-2">
            <TrendingUp className="w-5 h-5 text-emerald-600" />
            <span>Sales & Profit Trends Over Time</span>
          </h3>
          <div className="h-72">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={trends}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#E2E8F0" />
                <XAxis dataKey="date" stroke="#64748B" fontSize={11} />
                <YAxis stroke="#64748B" fontSize={11} tickFormatter={(val) => `$${val}`} />
                <Tooltip formatter={(value) => [`$${Number(value).toFixed(2)}`, '']} />
                <Legend />
                <Line type="monotone" dataKey="sales" name="Sales Revenue" stroke="#2563EB" strokeWidth={2.5} dot={{ r: 3 }} />
                <Line type="monotone" dataKey="gross_profit" name="Gross Profit" stroke="#16A34A" strokeWidth={2.5} dot={{ r: 3 }} />
                <Line type="monotone" dataKey="net_profit" name="Net Profit" stroke="#059669" strokeWidth={2} strokeDasharray="4 4" />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Sales by Category Bar Chart */}
        <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-sm">
          <h3 className="font-bold text-slate-800 text-base mb-4 flex items-center gap-2">
            <ShoppingBag className="w-5 h-5 text-blue-600" />
            <span>Sales Breakdown by Category</span>
          </h3>
          <div className="h-72">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={categorySales}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#E2E8F0" />
                <XAxis dataKey="category_name" stroke="#64748B" fontSize={11} />
                <YAxis stroke="#64748B" fontSize={11} tickFormatter={(val) => `$${val}`} />
                <Tooltip formatter={(value) => [`$${Number(value).toFixed(2)}`, 'Sales']} />
                <Bar dataKey="sales" fill="#0EA5E9" radius={[6, 6, 0, 0]} name="Total Sales ($)" />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>

      {/* Low Stock Items Table */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="p-4 border-b border-slate-200 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <AlertTriangle className="w-5 h-5 text-amber-500" />
            <h3 className="font-bold text-slate-800 text-base">Low Stock & Reorder Warning Table</h3>
          </div>
          <span className="text-xs bg-amber-50 text-amber-700 px-3 py-1 rounded-full font-semibold border border-amber-200">
            {lowStockItems.length} Products Needing Attention
          </span>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs text-slate-600">
            <thead className="bg-slate-50 text-slate-700 uppercase font-semibold border-b border-slate-200">
              <tr>
                <th className="py-3 px-4">Product Name</th>
                <th className="py-3 px-4">Category</th>
                <th className="py-3 px-4">SKU</th>
                <th className="py-3 px-4">Color / Size</th>
                <th className="py-3 px-4 text-center">Current Stock</th>
                <th className="py-3 px-4 text-center">Alert Threshold</th>
                <th className="py-3 px-4 text-center">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200">
              {lowStockItems.length === 0 ? (
                <tr>
                  <td colSpan={7} className="py-6 text-center text-slate-400">
                    All products are well stocked above alert thresholds!
                  </td>
                </tr>
              ) : (
                lowStockItems.map((item) => (
                  <tr key={item.variant_id} className="hover:bg-slate-50">
                    <td className="py-3 px-4 font-bold text-slate-900">{item.product_name}</td>
                    <td className="py-3 px-4 text-slate-600">{item.category_name}</td>
                    <td className="py-3 px-4 font-mono font-semibold text-slate-700">{item.sku}</td>
                    <td className="py-3 px-4 text-slate-600">{item.color} - {item.size}</td>
                    <td className="py-3 px-4 text-center font-bold text-rose-600">{item.stock_quantity}</td>
                    <td className="py-3 px-4 text-center text-slate-500">{item.min_stock_alert}</td>
                    <td className="py-3 px-4 text-center">
                      {item.status_badge === 'OUT_OF_STOCK' ? (
                        <span className="px-2.5 py-1 bg-rose-100 text-rose-700 rounded-full font-bold text-[10px] border border-rose-200">
                          OUT OF STOCK
                        </span>
                      ) : (
                        <span className="px-2.5 py-1 bg-amber-100 text-amber-700 rounded-full font-bold text-[10px] border border-amber-200">
                          LOW STOCK
                        </span>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
