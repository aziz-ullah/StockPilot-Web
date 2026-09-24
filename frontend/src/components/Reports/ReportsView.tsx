import React, { useState, useEffect } from 'react';
import { reportService } from '../../services/api';
import { FileSpreadsheet, Download, FileText, TrendingUp, DollarSign, Wallet, Receipt } from 'lucide-react';

export const ReportsView: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'pnl' | 'sales' | 'purchases' | 'inventory' | 'expenses'>('pnl');
  const [pnlData, setPnlData] = useState<any>(null);

  useEffect(() => {
    reportService.getPnL().then(data => setPnlData(data)).catch(console.error);
  }, []);

  const reportTabs = [
    { id: 'pnl', label: 'Profit & Loss Statement' },
    { id: 'sales', label: 'Sales Report' },
    { id: 'purchases', label: 'Purchase Report' },
    { id: 'inventory', label: 'Inventory Valuation Report' },
    { id: 'expenses', label: 'Operating Expenses Report' },
  ];

  return (
    <div className="space-y-6">
      {/* Header & Export Button */}
      <div className="flex flex-wrap items-center justify-between gap-4 border-b border-slate-200 pb-4">
        <div>
          <h2 className="text-xl font-bold text-slate-900 tracking-tight flex items-center gap-2">
            <FileSpreadsheet className="w-6 h-6 text-emerald-600" />
            <span>Financial & Operational Reports</span>
          </h2>
          <p className="text-xs text-slate-500">Comprehensive P&L, Sales, Purchase, Inventory, and Expense Reports with instant Excel exports</p>
        </div>

        <button
          onClick={() => reportService.exportExcel(activeTab)}
          className="px-4 py-2.5 bg-emerald-600 hover:bg-emerald-500 text-white rounded-lg text-xs font-bold transition-all shadow-md shadow-emerald-950/20 flex items-center gap-2"
        >
          <Download className="w-4 h-4" />
          <span>Export {activeTab.toUpperCase()} Report (.xlsx)</span>
        </button>
      </div>

      {/* Report Sub-Tabs */}
      <div className="flex bg-slate-100 p-1 rounded-xl border border-slate-200 overflow-x-auto">
        {reportTabs.map((t) => (
          <button
            key={t.id}
            onClick={() => setActiveTab(t.id as any)}
            className={`px-4 py-2.5 rounded-lg text-xs font-bold transition-all whitespace-nowrap ${
              activeTab === t.id
                ? 'bg-white text-slate-900 shadow-sm border border-slate-200'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>

      {/* P&L Statement Tab Content */}
      {activeTab === 'pnl' && pnlData && (
        <div className="bg-white rounded-xl border border-slate-200 shadow-sm p-6 space-y-6">
          <div className="border-b border-slate-200 pb-4 flex items-center justify-between">
            <div>
              <h3 className="text-lg font-black text-slate-900">PROFIT & LOSS STATEMENT (P&L)</h3>
              <p className="text-xs text-slate-500">Financial Summary adhering to Net Profit = Gross Profit - Operating Expenses</p>
            </div>
            <span className="text-xs bg-emerald-50 text-emerald-700 font-bold px-3 py-1 rounded-full border border-emerald-200">
              Audited NUMERIC(12,2)
            </span>
          </div>

          <div className="space-y-4">
            {/* Revenue */}
            <div className="flex justify-between items-center py-2 border-b border-slate-100">
              <span className="text-sm font-bold text-slate-800">Total Sales Revenue</span>
              <span className="text-base font-black text-blue-600">${Number(pnlData.total_revenue).toFixed(2)}</span>
            </div>

            {/* COGS */}
            <div className="flex justify-between items-center py-2 border-b border-slate-100 text-slate-600">
              <span className="text-sm font-medium">Cost of Goods Sold (COGS)</span>
              <span className="text-base font-bold text-slate-700">-${Number(pnlData.total_cogs).toFixed(2)}</span>
            </div>

            {/* Gross Profit */}
            <div className="flex justify-between items-center py-3 bg-emerald-50 px-4 rounded-xl border border-emerald-200">
              <span className="text-sm font-extrabold text-emerald-900">GROSS PROFIT (Revenue - COGS)</span>
              <span className="text-lg font-black text-emerald-700">${Number(pnlData.gross_profit).toFixed(2)}</span>
            </div>

            {/* Operating Expenses Breakdown */}
            <div className="pt-2 space-y-2">
              <span className="text-xs font-bold text-slate-500 uppercase tracking-wider block mb-1">Operating Expenses Breakdown:</span>
              {Object.entries(pnlData.expense_breakdown || {}).map(([cat, val]: [string, any]) => (
                <div key={cat} className="flex justify-between items-center text-xs text-slate-600 pl-4 py-1 border-l-2 border-slate-200">
                  <span>{cat}</span>
                  <span className="font-semibold text-slate-800">${Number(val).toFixed(2)}</span>
                </div>
              ))}
              <div className="flex justify-between items-center py-2 text-slate-700 font-bold border-t border-slate-100">
                <span>Total Operating Expenses</span>
                <span className="text-amber-600">-${Number(pnlData.total_expenses).toFixed(2)}</span>
              </div>
            </div>

            {/* Net Profit */}
            <div className={`flex justify-between items-center p-4 rounded-xl text-white ${
              pnlData.net_profit >= 0 ? 'bg-slate-900 border-emerald-600' : 'bg-rose-900'
            }`}>
              <div>
                <span className="text-xs font-semibold text-slate-300 block uppercase">NET PROFIT</span>
                <span className="text-xs text-slate-400">Gross Profit - Total Expenses</span>
              </div>
              <span className="text-2xl font-black text-emerald-400">${Number(pnlData.net_profit).toFixed(2)}</span>
            </div>
          </div>
        </div>
      )}

      {/* Generic Export Preview Tab for other report types */}
      {activeTab !== 'pnl' && (
        <div className="bg-white rounded-xl border border-slate-200 shadow-sm p-12 text-center space-y-4">
          <div className="w-16 h-16 bg-emerald-50 text-emerald-600 rounded-2xl flex items-center justify-center mx-auto border border-emerald-200">
            <FileSpreadsheet className="w-8 h-8" />
          </div>
          <div>
            <h3 className="text-lg font-bold text-slate-900">{activeTab.toUpperCase()} REPORT EXPORT</h3>
            <p className="text-xs text-slate-500 max-w-md mx-auto mt-1">
              Click the download button above to generate and export the full multi-sheet formatted Excel workbook for your {activeTab} data.
            </p>
          </div>
          <button
            onClick={() => reportService.exportExcel(activeTab)}
            className="px-6 py-3 bg-emerald-600 hover:bg-emerald-500 text-white rounded-xl text-xs font-bold transition-all shadow-lg shadow-emerald-950/20 inline-flex items-center gap-2"
          >
            <Download className="w-4 h-4" />
            <span>Download {activeTab.toUpperCase()} .xlsx File</span>
          </button>
        </div>
      )}
    </div>
  );
};
