import React, { useState, useEffect } from 'react';
import { saleService, productService } from '../../services/api';
import { Sale, Variant } from '../../types';
import { ShoppingCart, Plus, Minus, Trash2, Printer, Ban, Search, CheckCircle, FileText, User } from 'lucide-react';

export const POSView: React.FC = () => {
  const [activeSubTab, setActiveSubTab] = useState<'checkout' | 'history'>('checkout');
  const [sales, setSales] = useState<Sale[]>([]);
  const [variants, setVariants] = useState<Variant[]>([]);
  const [search, setSearch] = useState<string>('');
  const [loading, setLoading] = useState<boolean>(true);

  // Cart State
  const [cart, setCart] = useState<Array<{ variant: Variant; quantity: number; unit_price: number }>>([]);
  const [customerName, setCustomerName] = useState<string>('Walk-in Customer');
  const [customerPhone, setCustomerPhone] = useState<string>('');
  const [paymentMethod, setPaymentMethod] = useState<string>('Cash');
  const [discount, setDiscount] = useState<number>(0);
  const [tax, setTax] = useState<number>(0);

  const loadData = async () => {
    setLoading(true);
    try {
      const [sList, vList] = await Promise.all([
        saleService.listSales(),
        productService.listVariants()
      ]);
      setSales(sList);
      setVariants(vList);
    } catch (err) {
      console.error("Error loading POS data:", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleAddToCart = (variant: Variant) => {
    if (variant.stock_quantity <= 0) {
      alert("This item is out of stock!");
      return;
    }

    const existingIdx = cart.findIndex(i => i.variant.id === variant.id);
    if (existingIdx >= 0) {
      const updated = [...cart];
      if (updated[existingIdx].quantity + 1 > variant.stock_quantity) {
        alert("Cannot add more than available stock quantity!");
        return;
      }
      updated[existingIdx].quantity += 1;
      setCart(updated);
    } else {
      setCart([...cart, { variant, quantity: 1, unit_price: variant.selling_price }]);
    }
  };

  const handleUpdateCartQty = (idx: number, delta: number) => {
    const updated = [...cart];
    const newQty = updated[idx].quantity + delta;
    if (newQty <= 0) {
      setCart(cart.filter((_, i) => i !== idx));
    } else if (newQty > updated[idx].variant.stock_quantity) {
      alert("Cannot exceed available stock limit!");
    } else {
      updated[idx].quantity = newQty;
      setCart(updated);
    }
  };

  const handleRemoveFromCart = (idx: number) => {
    setCart(cart.filter((_, i) => i !== idx));
  };

  const handleCompleteSale = async () => {
    if (cart.length === 0) {
      alert("Cart is empty!");
      return;
    }

    try {
      const newSale = await saleService.createSale({
        customer_name: customerName,
        customer_phone: customerPhone,
        payment_method: paymentMethod,
        discount,
        tax,
        items: cart.map(i => ({
          variant_id: i.variant.id,
          quantity: i.quantity,
          unit_price: i.unit_price
        }))
      });

      alert(`Sale completed successfully! Invoice #${newSale.invoice_number}`);
      saleService.downloadPdf(newSale.id, newSale.invoice_number);
      setCart([]);
      setCustomerName('Walk-in Customer');
      setCustomerPhone('');
      loadData();
    } catch (err: any) {
      alert(err.response?.data?.detail || "Failed to complete POS sale");
    }
  };

  const handleVoidSale = async (id: number) => {
    if (!confirm("Are you sure you want to void this invoice? Stock will be restored and logged.")) return;
    try {
      await saleService.voidSale(id);
      loadData();
    } catch (err: any) {
      alert(err.response?.data?.detail || "Failed to void invoice");
    }
  };

  const filteredVariants = variants.filter(v =>
    v.sku.toLowerCase().includes(search.toLowerCase()) ||
    v.color.toLowerCase().includes(search.toLowerCase()) ||
    v.size.toLowerCase().includes(search.toLowerCase()) ||
    (v.product_name && v.product_name.toLowerCase().includes(search.toLowerCase()))
  );

  const subtotal = cart.reduce((sum, item) => sum + (item.quantity * item.unit_price), 0);
  const totalAmount = subtotal - discount + tax;

  return (
    <div className="space-y-6">
      {/* Navigation Sub-Tabs */}
      <div className="flex flex-wrap items-center justify-between gap-4 border-b border-slate-200 pb-4">
        <div className="flex bg-slate-100 p-1 rounded-xl border border-slate-200">
          <button
            onClick={() => setActiveSubTab('checkout')}
            className={`flex items-center gap-2 px-4 py-2 rounded-lg text-xs font-bold transition-all ${
              activeSubTab === 'checkout'
                ? 'bg-white text-slate-900 shadow-sm border border-slate-200'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            <ShoppingCart className="w-4 h-4 text-emerald-600" />
            <span>POS Register & Checkout</span>
          </button>

          <button
            onClick={() => setActiveSubTab('history')}
            className={`flex items-center gap-2 px-4 py-2 rounded-lg text-xs font-bold transition-all ${
              activeSubTab === 'history'
                ? 'bg-white text-slate-900 shadow-sm border border-slate-200'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            <FileText className="w-4 h-4 text-blue-600" />
            <span>Sales History & Invoices</span>
          </button>
        </div>
      </div>

      {activeSubTab === 'checkout' ? (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
          {/* Catalog & Search Column */}
          <div className="lg:col-span-7 space-y-4">
            <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm flex items-center gap-3">
              <Search className="w-4 h-4 text-slate-400" />
              <input
                type="text"
                placeholder="Fast SKU or Product Name search..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="w-full text-xs border-none outline-none focus:ring-0"
              />
            </div>

            {/* Products Grid */}
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 max-h-[600px] overflow-y-auto pr-1">
              {filteredVariants.map((v) => (
                <div
                  key={v.id}
                  onClick={() => handleAddToCart(v)}
                  className={`p-3.5 rounded-xl border transition-all cursor-pointer flex flex-col justify-between ${
                    v.stock_quantity <= 0
                      ? 'bg-slate-50 border-slate-200 opacity-60 cursor-not-allowed'
                      : 'bg-white hover:border-emerald-500 hover:shadow-md border-slate-200'
                  }`}
                >
                  <div>
                    <div className="flex items-center justify-between gap-1 mb-1">
                      <span className="text-[10px] font-mono bg-slate-100 text-slate-700 px-1.5 py-0.5 rounded border border-slate-200">
                        {v.sku}
                      </span>
                      <span className={`text-[10px] font-bold ${v.stock_quantity <= 0 ? 'text-rose-600' : 'text-slate-500'}`}>
                        {v.stock_quantity > 0 ? `${v.stock_quantity} in stock` : 'Out of stock'}
                      </span>
                    </div>
                    <h4 className="font-bold text-slate-900 text-xs line-clamp-1">{v.product_name}</h4>
                    <p className="text-[11px] text-slate-500">{v.color} &bull; Size {v.size}</p>
                  </div>

                  <div className="mt-3 pt-2 border-t border-slate-100 flex items-center justify-between">
                    <span className="font-extrabold text-emerald-700 text-sm">${Number(v.selling_price).toFixed(2)}</span>
                    <button
                      disabled={v.stock_quantity <= 0}
                      className="p-1.5 bg-emerald-50 text-emerald-700 rounded-lg hover:bg-emerald-600 hover:text-white transition-colors"
                    >
                      <Plus className="w-3.5 h-3.5" />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Cart & Customer Register Column */}
          <div className="lg:col-span-5 bg-white p-5 rounded-xl border border-slate-200 shadow-sm flex flex-col justify-between space-y-4">
            <div>
              <div className="flex items-center justify-between border-b border-slate-200 pb-3 mb-4">
                <h3 className="font-bold text-slate-900 text-base flex items-center gap-2">
                  <ShoppingCart className="w-5 h-5 text-emerald-600" />
                  <span>Current POS Cart</span>
                </h3>
                <span className="text-xs bg-emerald-50 text-emerald-700 px-2.5 py-1 rounded-full font-bold border border-emerald-200">
                  {cart.length} Items
                </span>
              </div>

              {/* Customer Inputs */}
              <div className="grid grid-cols-2 gap-3 mb-4">
                <div>
                  <label className="block text-[11px] font-bold text-slate-600 mb-1">Customer Name</label>
                  <input
                    type="text"
                    value={customerName}
                    onChange={(e) => setCustomerName(e.target.value)}
                    className="w-full px-2.5 py-1.5 border border-slate-300 rounded-lg text-xs focus:ring-2 focus:ring-emerald-500 outline-none"
                  />
                </div>

                <div>
                  <label className="block text-[11px] font-bold text-slate-600 mb-1">Customer Phone</label>
                  <input
                    type="text"
                    placeholder="Optional"
                    value={customerPhone}
                    onChange={(e) => setCustomerPhone(e.target.value)}
                    className="w-full px-2.5 py-1.5 border border-slate-300 rounded-lg text-xs focus:ring-2 focus:ring-emerald-500 outline-none"
                  />
                </div>
              </div>

              {/* Cart List */}
              <div className="space-y-2 max-h-60 overflow-y-auto pr-1">
                {cart.length === 0 ? (
                  <div className="py-12 text-center text-slate-400 text-xs">
                    Cart is empty. Click items from catalog to add.
                  </div>
                ) : (
                  cart.map((item, idx) => (
                    <div key={idx} className="flex items-center justify-between bg-slate-50 p-2.5 rounded-lg border border-slate-200 text-xs">
                      <div>
                        <p className="font-bold text-slate-900">{item.variant.product_name}</p>
                        <p className="text-[10px] text-slate-500">{item.variant.color} ({item.variant.size}) &bull; ${item.unit_price.toFixed(2)}</p>
                      </div>

                      <div className="flex items-center gap-3">
                        <div className="flex items-center border border-slate-300 rounded-md bg-white">
                          <button onClick={() => handleUpdateCartQty(idx, -1)} className="px-2 py-0.5 text-slate-600 hover:bg-slate-100">
                            <Minus className="w-3 h-3" />
                          </button>
                          <span className="px-2 py-0.5 font-bold text-slate-900">{item.quantity}</span>
                          <button onClick={() => handleUpdateCartQty(idx, 1)} className="px-2 py-0.5 text-slate-600 hover:bg-slate-100">
                            <Plus className="w-3 h-3" />
                          </button>
                        </div>
                        <span className="font-bold text-slate-900 text-xs w-14 text-right">
                          ${(item.quantity * item.unit_price).toFixed(2)}
                        </span>
                        <button onClick={() => handleRemoveFromCart(idx)} className="text-rose-500 hover:text-rose-700">
                          <Trash2 className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>

            {/* Total & Checkout Section */}
            <div className="border-t border-slate-200 pt-4 space-y-3">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-[11px] font-bold text-slate-600 mb-1">Payment Method</label>
                  <select
                    value={paymentMethod}
                    onChange={(e) => setPaymentMethod(e.target.value)}
                    className="w-full px-2.5 py-1.5 border border-slate-300 rounded-lg text-xs font-semibold focus:ring-2 focus:ring-emerald-500 outline-none bg-white"
                  >
                    <option value="Cash">Cash</option>
                    <option value="Card">Card</option>
                    <option value="Bank Transfer">Bank Transfer</option>
                    <option value="Other">Other</option>
                  </select>
                </div>

                <div>
                  <label className="block text-[11px] font-bold text-slate-600 mb-1">Discount ($)</label>
                  <input
                    type="number"
                    min="0"
                    step="0.01"
                    value={discount}
                    onChange={(e) => setDiscount(Number(e.target.value))}
                    className="w-full px-2.5 py-1.5 border border-slate-300 rounded-lg text-xs focus:ring-2 focus:ring-emerald-500 outline-none"
                  />
                </div>
              </div>

              <div className="flex justify-between items-center text-xs text-slate-600 pt-1">
                <span>Subtotal:</span>
                <span className="font-bold text-slate-800">${subtotal.toFixed(2)}</span>
              </div>

              <div className="flex justify-between items-center text-sm font-bold text-slate-900 border-t border-slate-200 pt-2">
                <span>Total Amount:</span>
                <span className="text-xl font-black text-emerald-600">${totalAmount.toFixed(2)}</span>
              </div>

              <button
                onClick={handleCompleteSale}
                disabled={cart.length === 0}
                className="w-full py-3 bg-emerald-600 hover:bg-emerald-500 disabled:bg-slate-300 text-white rounded-xl font-extrabold text-sm transition-all shadow-lg shadow-emerald-950/20 flex items-center justify-center gap-2"
              >
                <CheckCircle className="w-5 h-5" />
                <span>Complete Sale & Print PDF Receipt</span>
              </button>
            </div>
          </div>
        </div>
      ) : (
        /* Sales History View */
        <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs text-slate-600">
              <thead className="bg-slate-50 text-slate-700 uppercase font-semibold border-b border-slate-200">
                <tr>
                  <th className="py-3.5 px-4">Invoice #</th>
                  <th className="py-3.5 px-4">Customer</th>
                  <th className="py-3.5 px-4">Date</th>
                  <th className="py-3.5 px-4">Payment</th>
                  <th className="py-3.5 px-4 text-center">Status</th>
                  <th className="py-3.5 px-4 text-right">Revenue ($)</th>
                  <th className="py-3.5 px-4 text-right">COGS ($)</th>
                  <th className="py-3.5 px-4 text-right">Gross Profit ($)</th>
                  <th className="py-3.5 px-4 text-center">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200">
                {sales.map((s) => (
                  <tr key={s.id} className="hover:bg-slate-50">
                    <td className="py-3 px-4 font-mono font-bold text-slate-900">{s.invoice_number}</td>
                    <td className="py-3 px-4 font-medium text-slate-800">{s.customer_name}</td>
                    <td className="py-3 px-4 text-slate-500">{new Date(s.sale_date).toLocaleString()}</td>
                    <td className="py-3 px-4 text-slate-700 font-semibold">{s.payment_method}</td>
                    <td className="py-3 px-4 text-center">
                      <span className={`px-2.5 py-1 rounded-full text-[10px] font-bold uppercase border ${
                        s.status === 'Completed' ? 'bg-emerald-100 text-emerald-700 border-emerald-200' : 'bg-rose-100 text-rose-700 border-rose-200'
                      }`}>
                        {s.status}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-right font-bold text-slate-900">${Number(s.total_amount).toFixed(2)}</td>
                    <td className="py-3 px-4 text-right font-medium text-slate-500">${Number(s.total_cogs).toFixed(2)}</td>
                    <td className="py-3 px-4 text-right font-black text-emerald-600">${Number(s.gross_profit).toFixed(2)}</td>
                    <td className="py-3 px-4 text-center flex items-center justify-center gap-2">
                      <button
                        onClick={() => saleService.downloadPdf(s.id, s.invoice_number)}
                        className="p-1.5 text-blue-600 hover:bg-blue-50 rounded border border-blue-200"
                        title="Print PDF Receipt"
                      >
                        <Printer className="w-3.5 h-3.5" />
                      </button>

                      {s.status === 'Completed' && (
                        <button
                          onClick={() => handleVoidSale(s.id)}
                          className="px-2 py-1 text-rose-600 hover:bg-rose-50 rounded border border-rose-200 text-[10px] font-bold inline-flex items-center gap-1"
                        >
                          <Ban className="w-3 h-3" />
                          <span>Void</span>
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
};
