import React, { useState } from 'react';
import { useAuth } from './context/AuthContext';
import { authService } from './services/api';
import { Sidebar } from './components/Layout/Sidebar';
import { Navbar } from './components/Layout/Navbar';
import { DashboardView } from './components/Dashboard/DashboardView';
import { ProductsView } from './components/Inventory/ProductsView';
import { PurchasesView } from './components/Purchases/PurchasesView';
import { POSView } from './components/POS/POSView';
import { ExpensesView } from './components/Expenses/ExpensesView';
import { ReportsView } from './components/Reports/ReportsView';
import { ExcelModal } from './components/ExcelImport/ExcelModal';
import { UsersView } from './components/Users/UsersView';
import { Shirt, Lock, User as UserIcon, Shield, ArrowRight } from 'lucide-react';

export const AppContent: React.FC = () => {
  const { user, login, loading } = useAuth();
  const [activeTab, setActiveTab] = useState<string>('dashboard');
  const [isExcelOpen, setIsExcelOpen] = useState<boolean>(false);

  // Login Form state
  const [username, setUsername] = useState<string>('admin');
  const [password, setPassword] = useState<string>('admin123');
  const [loginError, setLoginError] = useState<string>('');

  const handleLogin = async (e?: React.FormEvent, customUser?: string, customPass?: string) => {
    if (e) e.preventDefault();
    setLoginError('');
    try {
      const res = await authService.login(customUser || username, customPass || password);
      login(res.access_token, res.user);
    } catch (err: any) {
      setLoginError(err.response?.data?.detail || "Invalid username or password");
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-slate-900 flex items-center justify-center text-white text-sm font-semibold">
        Loading StockPilot Web...
      </div>
    );
  }

  // Render Login Screen if unauthenticated
  if (!user) {
    return (
      <div className="min-h-screen bg-slate-900 flex items-center justify-center p-4">
        <div className="max-w-md w-full bg-white rounded-2xl border border-slate-200 p-8 shadow-2xl space-y-6">
          <div className="text-center space-y-2">
            <div className="w-14 h-14 rounded-2xl bg-emerald-500/10 text-emerald-600 border border-emerald-500/20 flex items-center justify-center mx-auto">
              <Shirt className="w-8 h-8" />
            </div>
            <h1 className="text-2xl font-black text-slate-900 tracking-tight">StockPilot Web</h1>
            <p className="text-xs text-slate-500 font-medium">Clothing ERP / Inventory & POS System</p>
          </div>

          {loginError && (
            <div className="bg-rose-50 text-rose-700 p-3 rounded-lg text-xs font-semibold border border-rose-200 text-center">
              {loginError}
            </div>
          )}

          <form onSubmit={handleLogin} className="space-y-4">
            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">Username or Email</label>
              <div className="relative">
                <UserIcon className="w-4 h-4 text-slate-400 absolute left-3 top-3" />
                <input
                  type="text"
                  required
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  className="w-full pl-9 pr-3 py-2 border border-slate-300 rounded-lg text-xs focus:ring-2 focus:ring-emerald-500 outline-none"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">Password</label>
              <div className="relative">
                <Lock className="w-4 h-4 text-slate-400 absolute left-3 top-3" />
                <input
                  type="password"
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="w-full pl-9 pr-3 py-2 border border-slate-300 rounded-lg text-xs focus:ring-2 focus:ring-emerald-500 outline-none"
                />
              </div>
            </div>

            <button
              type="submit"
              className="w-full py-3 bg-emerald-600 hover:bg-emerald-500 text-white rounded-xl text-xs font-extrabold transition-all shadow-lg shadow-emerald-950/20 flex items-center justify-center gap-2"
            >
              <span>Sign In to System</span>
              <ArrowRight className="w-4 h-4" />
            </button>
          </form>

          {/* Quick Demo Credentials */}
          <div className="pt-4 border-t border-slate-100 space-y-2">
            <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider block text-center">Quick Demo Logins</span>
            <div className="grid grid-cols-2 gap-2">
              <button
                onClick={() => handleLogin(undefined, 'admin', 'admin123')}
                className="py-2 px-3 bg-slate-100 hover:bg-slate-200 text-slate-800 rounded-lg text-xs font-bold text-left flex items-center justify-between border border-slate-200"
              >
                <span>Admin User</span>
                <Shield className="w-3.5 h-3.5 text-emerald-600" />
              </button>
              <button
                onClick={() => handleLogin(undefined, 'staff', 'staff123')}
                className="py-2 px-3 bg-slate-100 hover:bg-slate-200 text-slate-800 rounded-lg text-xs font-bold text-left flex items-center justify-between border border-slate-200"
              >
                <span>Staff Cashier</span>
                <UserIcon className="w-3.5 h-3.5 text-blue-600" />
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  // Render App Shell
  return (
    <div className="flex min-h-screen bg-slate-100">
      <Sidebar
        activeTab={activeTab}
        setActiveTab={setActiveTab}
        openExcelModal={() => setIsExcelOpen(true)}
      />

      <div className="flex-1 flex flex-col min-w-0">
        <Navbar />

        <main className="flex-1 p-6 overflow-y-auto max-w-7xl w-full mx-auto">
          {activeTab === 'dashboard' && <DashboardView />}
          {activeTab === 'products' && <ProductsView />}
          {activeTab === 'purchases' && <PurchasesView />}
          {activeTab === 'pos' && <POSView />}
          {activeTab === 'expenses' && <ExpensesView />}
          {activeTab === 'reports' && <ReportsView />}
          {activeTab === 'users' && <UsersView />}
        </main>
      </div>

      <ExcelModal
        isOpen={isExcelOpen}
        onClose={() => setIsExcelOpen(false)}
        onSuccess={() => {}}
      />
    </div>
  );
};
