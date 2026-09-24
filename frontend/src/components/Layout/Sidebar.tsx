import React from 'react';
import { useAuth } from '../../context/AuthContext';
import {
  LayoutDashboard,
  Package,
  ShoppingBag,
  ShoppingCart,
  Receipt,
  FileSpreadsheet,
  Users,
  Upload,
  Shirt
} from 'lucide-react';

interface SidebarProps {
  activeTab: string;
  setActiveTab: (tab: string) => void;
  openExcelModal: () => void;
}

export const Sidebar: React.FC<SidebarProps> = ({ activeTab, setActiveTab, openExcelModal }) => {
  const { user } = useAuth();

  const navItems = [
    { id: 'dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { id: 'products', label: 'Products & Inventory', icon: Package },
    { id: 'purchases', label: 'Stock Purchases', icon: ShoppingBag },
    { id: 'pos', label: 'Sales & POS', icon: ShoppingCart },
    { id: 'expenses', label: 'Operating Expenses', icon: Receipt },
    { id: 'reports', label: 'Reports & Exports', icon: FileSpreadsheet },
  ];

  if (user?.role === 'Admin') {
    navItems.push({ id: 'users', label: 'User Accounts', icon: Users });
  }

  return (
    <aside className="w-64 bg-slate-900 text-slate-300 flex flex-col min-h-screen border-r border-slate-800 shadow-xl select-none shrink-0">
      {/* Brand Header */}
      <div className="h-16 px-6 flex items-center gap-3 border-b border-slate-800 bg-slate-950">
        <div className="w-10 h-10 rounded-xl bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 flex items-center justify-center font-bold">
          <Shirt className="w-6 h-6" />
        </div>
        <div>
          <h1 className="font-bold text-white text-lg tracking-tight leading-tight">StockPilot</h1>
          <p className="text-xs text-slate-400 font-medium">Clothing ERP / POS</p>
        </div>
      </div>

      {/* Excel Upload Action Button */}
      <div className="p-4">
        <button
          onClick={openExcelModal}
          className="w-full py-2.5 px-4 bg-emerald-600 hover:bg-emerald-500 text-white rounded-lg font-medium text-sm transition-all duration-200 shadow-lg shadow-emerald-950/50 flex items-center justify-center gap-2 border border-emerald-500/30"
        >
          <Upload className="w-4 h-4" />
          <span>Import Excel Sheet</span>
        </button>
      </div>

      {/* Navigation List */}
      <nav className="flex-1 px-3 py-2 space-y-1">
        {navItems.map((item) => {
          const Icon = item.icon;
          const isActive = activeTab === item.id;
          return (
            <button
              key={item.id}
              onClick={() => setActiveTab(item.id)}
              className={`w-full flex items-center gap-3 px-3.5 py-3 rounded-lg text-sm font-medium transition-all duration-150 ${
                isActive
                  ? 'bg-emerald-500/15 text-emerald-400 border border-emerald-500/30 shadow-inner'
                  : 'text-slate-400 hover:bg-slate-800/60 hover:text-slate-200'
              }`}
            >
              <Icon className={`w-5 h-5 ${isActive ? 'text-emerald-400' : 'text-slate-400'}`} />
              <span>{item.label}</span>
            </button>
          );
        })}
      </nav>

      {/* Footer info */}
      <div className="p-4 border-t border-slate-800 text-xs text-slate-500 text-center">
        v1.0.0 &bull; PostgreSQL NUMERIC(12,2)
      </div>
    </aside>
  );
};
