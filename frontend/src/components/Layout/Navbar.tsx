import React from 'react';
import { useAuth } from '../../context/AuthContext';
import { LogOut, User as UserIcon, Shield } from 'lucide-react';

export const Navbar: React.FC = () => {
  const { user, logout } = useAuth();

  return (
    <header className="h-16 bg-white border-b border-slate-200 px-6 flex items-center justify-between shadow-sm sticky top-0 z-10">
      <div className="flex items-center gap-3">
        <h2 className="text-xl font-bold text-slate-800 tracking-tight">StockPilot Web</h2>
        <span className="text-xs bg-slate-100 text-slate-600 font-semibold px-2.5 py-1 rounded-full border border-slate-200">
          StockPilot Apparel Org
        </span>
      </div>

      <div className="flex items-center gap-4">
        {user && (
          <div className="flex items-center gap-3 bg-slate-50 border border-slate-200 px-3.5 py-1.5 rounded-full">
            <div className="w-8 h-8 rounded-full bg-emerald-100 text-emerald-700 flex items-center justify-center font-bold text-sm">
              {user.full_name.charAt(0).toUpperCase()}
            </div>
            <div className="text-left">
              <p className="text-xs font-bold text-slate-800 leading-none">{user.full_name}</p>
              <div className="flex items-center gap-1 mt-0.5">
                <Shield className="w-3 h-3 text-slate-400" />
                <span className="text-[10px] font-semibold text-slate-500 uppercase">{user.role}</span>
              </div>
            </div>
          </div>
        )}

        <button
          onClick={logout}
          title="Log Out"
          className="p-2 text-slate-500 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition-colors border border-slate-200 hover:border-rose-200"
        >
          <LogOut className="w-5 h-5" />
        </button>
      </div>
    </header>
  );
};
