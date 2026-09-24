import React, { useState, useEffect } from 'react';
import { expenseService } from '../../services/api';
import { Expense } from '../../types';
import { Receipt, Plus, X, Filter, DollarSign, Calendar } from 'lucide-react';

export const ExpensesView: React.FC = () => {
  const [expenses, setExpenses] = useState<Expense[]>([]);
  const [categories, setCategories] = useState<string[]>([]);
  const [selectedCat, setSelectedCat] = useState<string>('');
  const [loading, setLoading] = useState<boolean>(true);

  const [showAddModal, setShowAddModal] = useState<boolean>(false);
  const [category, setCategory] = useState<string>('Rent');
  const [description, setDescription] = useState<string>('');
  const [amount, setAmount] = useState<number>(100.00);

  const loadData = async () => {
    setLoading(true);
    try {
      const [eList, catList] = await Promise.all([
        expenseService.listExpenses(selectedCat || undefined),
        expenseService.getCategories()
      ]);
      setExpenses(eList);
      setCategories(catList);
      if (catList.length > 0 && !category) setCategory(catList[0]);
    } catch (err) {
      console.error("Error loading expenses:", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [selectedCat]);

  const handleCreateExpense = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await expenseService.createExpense({ category, description, amount });
      setShowAddModal(false);
      setDescription('');
      loadData();
    } catch (err: any) {
      alert(err.response?.data?.detail || "Failed to log expense");
    }
  };

  const totalExpenseAmount = expenses.reduce((sum, e) => sum + Number(e.amount), 0);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-wrap items-center justify-between gap-4 border-b border-slate-200 pb-4">
        <div>
          <h2 className="text-xl font-bold text-slate-900 tracking-tight flex items-center gap-2">
            <Receipt className="w-6 h-6 text-amber-600" />
            <span>Business Operating Expenses</span>
          </h2>
          <p className="text-xs text-slate-500">Log monthly rent, utilities, shipping, packaging, salaries, and marketing costs</p>
        </div>

        <button
          onClick={() => setShowAddModal(true)}
          className="px-4 py-2.5 bg-amber-600 hover:bg-amber-500 text-white rounded-lg text-xs font-bold transition-all shadow-md flex items-center gap-2"
        >
          <Plus className="w-4 h-4" />
          <span>Log Operating Expense</span>
        </button>
      </div>

      {/* Filter Bar & Summary */}
      <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm flex flex-wrap items-center justify-between gap-4">
        <div className="flex items-center gap-2">
          <Filter className="w-4 h-4 text-slate-400" />
          <span className="text-xs font-bold text-slate-700">Filter Category:</span>
          <select
            value={selectedCat}
            onChange={(e) => setSelectedCat(e.target.value)}
            className="border border-slate-300 rounded-lg px-3 py-1.5 text-xs font-semibold focus:ring-2 focus:ring-amber-500 outline-none bg-white"
          >
            <option value="">All Categories</option>
            {categories.map((c) => (
              <option key={c} value={c}>{c}</option>
            ))}
          </select>
        </div>

        <div className="bg-amber-50 border border-amber-200 px-4 py-1.5 rounded-lg flex items-center gap-2">
          <span className="text-xs font-bold text-amber-900">Total Filtered Expenses:</span>
          <span className="text-sm font-black text-amber-700">${totalExpenseAmount.toFixed(2)}</span>
        </div>
      </div>

      {/* Expense Table */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs text-slate-600">
            <thead className="bg-slate-50 text-slate-700 uppercase font-semibold border-b border-slate-200">
              <tr>
                <th className="py-3.5 px-4">Date</th>
                <th className="py-3.5 px-4">Category</th>
                <th className="py-3.5 px-4">Description</th>
                <th className="py-3.5 px-4 text-right">Amount ($)</th>
                <th className="py-3.5 px-4">Logged By</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200">
              {expenses.length === 0 ? (
                <tr>
                  <td colSpan={5} className="py-8 text-center text-slate-400">
                    No operating expenses logged yet.
                  </td>
                </tr>
              ) : (
                expenses.map((e) => (
                  <tr key={e.id} className="hover:bg-slate-50">
                    <td className="py-3 px-4 text-slate-500">{new Date(e.expense_date).toLocaleDateString()}</td>
                    <td className="py-3 px-4">
                      <span className="px-2.5 py-1 bg-amber-50 text-amber-800 rounded-md font-bold text-[11px] border border-amber-200">
                        {e.category}
                      </span>
                    </td>
                    <td className="py-3 px-4 font-semibold text-slate-900">{e.description}</td>
                    <td className="py-3 px-4 text-right font-black text-amber-700">${Number(e.amount).toFixed(2)}</td>
                    <td className="py-3 px-4 text-slate-600 font-medium">{e.created_by_name || 'System'}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Add Expense Modal */}
      {showAddModal && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl border border-slate-200 max-w-md w-full p-6 shadow-2xl space-y-4">
            <div className="flex items-center justify-between border-b border-slate-200 pb-3">
              <h3 className="text-base font-bold text-slate-900 flex items-center gap-2">
                <Receipt className="w-5 h-5 text-amber-600" />
                <span>Log Operating Expense</span>
              </h3>
              <button onClick={() => setShowAddModal(false)} className="text-slate-400 hover:text-slate-600">
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleCreateExpense} className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">Expense Category</label>
                <select
                  value={category}
                  onChange={(e) => setCategory(e.target.value)}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg text-xs font-semibold focus:ring-2 focus:ring-amber-500 outline-none bg-white"
                >
                  {categories.map((c) => (
                    <option key={c} value={c}>{c}</option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">Description / Memo</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Monthly store rent payment"
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg text-xs focus:ring-2 focus:ring-amber-500 outline-none"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">Amount ($)</label>
                <input
                  type="number"
                  step="0.01"
                  required
                  value={amount}
                  onChange={(e) => setAmount(Number(e.target.value))}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg text-xs font-bold text-amber-700 focus:ring-2 focus:ring-amber-500 outline-none"
                />
              </div>

              <div className="pt-3 flex justify-end gap-3">
                <button
                  type="button"
                  onClick={() => setShowAddModal(false)}
                  className="px-4 py-2 border border-slate-300 rounded-lg text-xs font-semibold text-slate-600 hover:bg-slate-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-amber-600 hover:bg-amber-500 text-white rounded-lg text-xs font-bold shadow-md"
                >
                  Save Expense
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
