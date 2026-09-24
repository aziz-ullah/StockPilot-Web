import React, { useState, useEffect } from 'react';
import { purchaseService, productService } from '../../services/api';
import { Purchase, Variant, Supplier } from '../../types';
import { ShoppingBag, Plus, X, Ban, Truck, DollarSign, Calendar } from 'lucide-react';

export const PurchasesView: React.FC = () => {
  const [purchases, setPurchases] = useState<Purchase[]>([]);
  const [variants, setVariants] = useState<Variant[]>([]);
  const [suppliers, setSuppliers] = useState<Supplier[]>([]);
  const [loading, setLoading] = useState<boolean>(true);

  const [showAddModal, setShowAddModal] = useState<boolean>(false);
  const [supplierName, setSupplierName] = useState<string>('Primary Apparel Supplier');
  const [paymentStatus, setPaymentStatus] = useState<string>('Paid');
  const [notes, setNotes] = useState<string>('');
  const [items, setItems] = useState<Array<{ variant_id: number; quantity: number; unit_cost: number }>>([]);

  const loadData = async () => {
    setLoading(true);
    try {
      const [pList, vList, sList] = await Promise.all([
        purchaseService.listPurchases(),
        productService.listVariants(),
        purchaseService.listSuppliers()
      ]);
      setPurchases(pList);
      setVariants(vList);
      setSuppliers(sList);
      if (vList.length > 0 && items.length === 0) {
        setItems([{ variant_id: vList[0].id, quantity: 10, unit_cost: vList[0].cost_price }]);
      }
    } catch (err) {
      console.error("Error loading purchases:", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleAddItemRow = () => {
    if (variants.length > 0) {
      setItems([...items, { variant_id: variants[0].id, quantity: 10, unit_cost: variants[0].cost_price }]);
    }
  };

  const handleRemoveItemRow = (idx: number) => {
    setItems(items.filter((_, i) => i !== idx));
  };

  const handleItemChange = (idx: number, field: string, val: any) => {
    const updated = [...items];
    if (field === 'variant_id') {
      const selectedV = variants.find(v => v.id === Number(val));
      updated[idx].variant_id = Number(val);
      if (selectedV) updated[idx].unit_cost = selectedV.cost_price;
    } else if (field === 'quantity') {
      updated[idx].quantity = Number(val);
    } else if (field === 'unit_cost') {
      updated[idx].unit_cost = Number(val);
    }
    setItems(updated);
  };

  const handleCreatePurchase = async (e: React.FormEvent) => {
    e.preventDefault();
    if (items.length === 0) {
      alert("Please add at least one item to the purchase order");
      return;
    }
    try {
      await purchaseService.createPurchase({
        supplier_name: supplierName,
        payment_status: paymentStatus,
        notes,
        items
      });
      setShowAddModal(false);
      setNotes('');
      loadData();
    } catch (err: any) {
      alert(err.response?.data?.detail || "Failed to record purchase");
    }
  };

  const handleCancelPurchase = async (id: number) => {
    if (!confirm("Are you sure you want to cancel this purchase order? This will deduct the purchased stock from inventory.")) return;
    try {
      await purchaseService.cancelPurchase(id);
      loadData();
    } catch (err: any) {
      alert(err.response?.data?.detail || "Failed to cancel purchase");
    }
  };

  const totalPOAmount = items.reduce((sum, item) => sum + (item.quantity * item.unit_cost), 0);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-wrap items-center justify-between gap-4 border-b border-slate-200 pb-4">
        <div>
          <h2 className="text-xl font-bold text-slate-900 tracking-tight flex items-center gap-2">
            <ShoppingBag className="w-6 h-6 text-emerald-600" />
            <span>Stock Purchases & Supplier Stock In</span>
          </h2>
          <p className="text-xs text-slate-500">Record inventory Restock orders from suppliers with automatic stock adjustment</p>
        </div>

        <button
          onClick={() => setShowAddModal(true)}
          className="px-4 py-2.5 bg-emerald-600 hover:bg-emerald-500 text-white rounded-lg text-xs font-bold transition-all shadow-md shadow-emerald-900/10 flex items-center gap-2"
        >
          <Plus className="w-4 h-4" />
          <span>Record Stock Purchase (Stock In)</span>
        </button>
      </div>

      {/* Purchase Orders Table */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs text-slate-600">
            <thead className="bg-slate-50 text-slate-700 uppercase font-semibold border-b border-slate-200">
              <tr>
                <th className="py-3.5 px-4">PO Number</th>
                <th className="py-3.5 px-4">Supplier Name</th>
                <th className="py-3.5 px-4">Date</th>
                <th className="py-3.5 px-4">Payment Status</th>
                <th className="py-3.5 px-4 text-center">Status</th>
                <th className="py-3.5 px-4 text-right">Total Amount ($)</th>
                <th className="py-3.5 px-4 text-center">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200">
              {purchases.length === 0 ? (
                <tr>
                  <td colSpan={7} className="py-8 text-center text-slate-400">
                    No purchase orders recorded yet.
                  </td>
                </tr>
              ) : (
                purchases.map((p) => (
                  <tr key={p.id} className="hover:bg-slate-50 transition-colors">
                    <td className="py-3 px-4 font-mono font-bold text-slate-900">{p.purchase_number}</td>
                    <td className="py-3 px-4 font-semibold text-slate-800">{p.supplier_name || 'N/A'}</td>
                    <td className="py-3 px-4 text-slate-500">{new Date(p.purchase_date).toLocaleDateString()}</td>
                    <td className="py-3 px-4">
                      <span className={`px-2 py-0.5 rounded text-[10px] font-bold border ${
                        p.payment_status === 'Paid' ? 'bg-emerald-50 text-emerald-700 border-emerald-200' : 'bg-amber-50 text-amber-700 border-amber-200'
                      }`}>
                        {p.payment_status}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-center">
                      <span className={`px-2.5 py-1 rounded-full text-[10px] font-bold uppercase border ${
                        p.status === 'Completed' ? 'bg-emerald-100 text-emerald-700 border-emerald-200' : 'bg-rose-100 text-rose-700 border-rose-200'
                      }`}>
                        {p.status}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-right font-black text-slate-900">${Number(p.total_amount).toFixed(2)}</td>
                    <td className="py-3 px-4 text-center">
                      {p.status === 'Completed' && (
                        <button
                          onClick={() => handleCancelPurchase(p.id)}
                          className="px-2.5 py-1 text-rose-600 hover:bg-rose-50 rounded-md transition-colors border border-rose-200 text-[10px] font-bold inline-flex items-center gap-1"
                        >
                          <Ban className="w-3 h-3" />
                          <span>Cancel PO</span>
                        </button>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Record Stock Purchase Modal */}
      {showAddModal && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl border border-slate-200 max-w-2xl w-full p-6 shadow-2xl space-y-4">
            <div className="flex items-center justify-between border-b border-slate-200 pb-3">
              <h3 className="text-lg font-bold text-slate-900 flex items-center gap-2">
                <Truck className="w-5 h-5 text-emerald-600" />
                <span>Record New Stock Purchase Order (Stock In)</span>
              </h3>
              <button onClick={() => setShowAddModal(false)} className="text-slate-400 hover:text-slate-600">
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleCreatePurchase} className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">Supplier Name</label>
                  <input
                    type="text"
                    required
                    value={supplierName}
                    onChange={(e) => setSupplierName(e.target.value)}
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg text-xs focus:ring-2 focus:ring-emerald-500 outline-none"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">Payment Status</label>
                  <select
                    value={paymentStatus}
                    onChange={(e) => setPaymentStatus(e.target.value)}
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg text-xs focus:ring-2 focus:ring-emerald-500 outline-none bg-white font-medium"
                  >
                    <option value="Paid">Paid</option>
                    <option value="Pending">Pending / Credit</option>
                  </select>
                </div>
              </div>

              {/* Items Table */}
              <div className="space-y-2 border border-slate-200 rounded-xl p-3 bg-slate-50">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-slate-800 uppercase">Purchase Line Items</span>
                  <button
                    type="button"
                    onClick={handleAddItemRow}
                    className="text-xs text-emerald-600 font-bold hover:underline flex items-center gap-1"
                  >
                    <Plus className="w-3.5 h-3.5" />
                    <span>Add Item Line</span>
                  </button>
                </div>

                {items.map((item, idx) => (
                  <div key={idx} className="grid grid-cols-12 gap-2 items-center bg-white p-2.5 rounded-lg border border-slate-200">
                    <div className="col-span-6">
                      <select
                        value={item.variant_id}
                        onChange={(e) => handleItemChange(idx, 'variant_id', e.target.value)}
                        className="w-full border border-slate-300 rounded px-2 py-1 text-xs focus:ring-1 focus:ring-emerald-500 outline-none font-medium"
                      >
                        {variants.map((v) => (
                          <option key={v.id} value={v.id}>
                            {v.product_name} ({v.color}, {v.size}) - {v.sku}
                          </option>
                        ))}
                      </select>
                    </div>

                    <div className="col-span-2">
                      <input
                        type="number"
                        min="1"
                        placeholder="Qty"
                        value={item.quantity}
                        onChange={(e) => handleItemChange(idx, 'quantity', e.target.value)}
                        className="w-full border border-slate-300 rounded px-2 py-1 text-xs text-center font-bold"
                      />
                    </div>

                    <div className="col-span-3">
                      <input
                        type="number"
                        step="0.01"
                        placeholder="Unit Cost ($)"
                        value={item.unit_cost}
                        onChange={(e) => handleItemChange(idx, 'unit_cost', e.target.value)}
                        className="w-full border border-slate-300 rounded px-2 py-1 text-xs text-right font-medium"
                      />
                    </div>

                    <div className="col-span-1 text-center">
                      {items.length > 1 && (
                        <button
                          type="button"
                          onClick={() => handleRemoveItemRow(idx)}
                          className="text-rose-500 hover:text-rose-700"
                        >
                          <X className="w-4 h-4" />
                        </button>
                      )}
                    </div>
                  </div>
                ))}
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">Notes / Delivery Reference</label>
                <input
                  type="text"
                  placeholder="e.g. Batch #240 shipment from manufacturer"
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg text-xs focus:ring-2 focus:ring-emerald-500 outline-none"
                />
              </div>

              <div className="flex items-center justify-between pt-3 border-t border-slate-200">
                <div>
                  <span className="text-xs text-slate-500 block">Total Order Cost</span>
                  <span className="text-lg font-black text-slate-900">${totalPOAmount.toFixed(2)}</span>
                </div>

                <div className="flex gap-3">
                  <button
                    type="button"
                    onClick={() => setShowAddModal(false)}
                    className="px-4 py-2 border border-slate-300 rounded-lg text-xs font-semibold text-slate-600 hover:bg-slate-50"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-lg text-xs font-bold shadow-md"
                  >
                    Confirm Purchase Order
                  </button>
                </div>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
