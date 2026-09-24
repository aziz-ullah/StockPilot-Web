import React, { useState, useEffect } from 'react';
import { productService, categoryService } from '../../services/api';
import { Product, Variant, Category, StockMovement } from '../../types';
import { Search, Plus, Package, Edit, History, Tag, Check, X, AlertTriangle } from 'lucide-react';

export const ProductsView: React.FC = () => {
  const [activeSubTab, setActiveSubTab] = useState<'catalog' | 'movements'>('catalog');
  const [products, setProducts] = useState<Product[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [movements, setMovements] = useState<StockMovement[]>([]);
  const [search, setSearch] = useState<string>('');
  const [selectedCategory, setSelectedCategory] = useState<number | undefined>(undefined);
  const [loading, setLoading] = useState<boolean>(true);

  // Modals
  const [showAddModal, setShowAddModal] = useState<boolean>(false);
  const [editingVariant, setEditingVariant] = useState<Variant | null>(null);

  // Add Product Form
  const [newProdName, setNewProdName] = useState<string>('');
  const [newProdCat, setNewProdCat] = useState<number>(1);
  const [newColor, setNewColor] = useState<string>('');
  const [newSize, setNewSize] = useState<string>('M');
  const [newCostPrice, setNewCostPrice] = useState<number>(36.00);
  const [newSellingPrice, setNewSellingPrice] = useState<number>(70.00);
  const [newStockQty, setNewStockQty] = useState<number>(10);
  const [newMinAlert, setNewMinAlert] = useState<number>(5);

  // Edit Variant Form
  const [editSellingPrice, setEditSellingPrice] = useState<number>(0);
  const [editCostPrice, setEditCostPrice] = useState<number>(0);
  const [editStockQty, setEditStockQty] = useState<number>(0);

  const loadData = async () => {
    setLoading(true);
    try {
      const [prods, cats, movs] = await Promise.all([
        productService.listProducts(selectedCategory, search),
        categoryService.list(),
        productService.listMovements()
      ]);
      setProducts(prods);
      setCategories(cats);
      setMovements(movs);
      if (cats.length > 0 && !newProdCat) setNewProdCat(cats[0].id);
    } catch (err) {
      console.error("Error loading products:", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [search, selectedCategory]);

  const handleCreateProduct = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await productService.createProduct({
        category_id: newProdCat,
        name: newProdName,
        variants: [
          {
            color: newColor,
            size: newSize,
            cost_price: newCostPrice,
            selling_price: newSellingPrice,
            stock_quantity: newStockQty,
            min_stock_alert: newMinAlert
          }
        ]
      });
      setShowAddModal(false);
      setNewProdName('');
      setNewColor('');
      loadData();
    } catch (err: any) {
      alert(err.response?.data?.detail || "Failed to create product");
    }
  };

  const handleUpdateVariant = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingVariant) return;
    try {
      await productService.updateVariant(editingVariant.id, {
        cost_price: editCostPrice,
        selling_price: editSellingPrice,
        stock_quantity: editStockQty
      });
      setEditingVariant(null);
      loadData();
    } catch (err: any) {
      alert(err.response?.data?.detail || "Failed to update variant");
    }
  };

  return (
    <div className="space-y-6">
      {/* Header & Sub-Tabs */}
      <div className="flex flex-wrap items-center justify-between gap-4 border-b border-slate-200 pb-4">
        <div className="flex items-center gap-4">
          <div className="flex bg-slate-100 p-1 rounded-xl border border-slate-200">
            <button
              onClick={() => setActiveSubTab('catalog')}
              className={`flex items-center gap-2 px-4 py-2 rounded-lg text-xs font-bold transition-all ${
                activeSubTab === 'catalog'
                  ? 'bg-white text-slate-900 shadow-sm border border-slate-200'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              <Package className="w-4 h-4 text-emerald-600" />
              <span>Products Catalog</span>
            </button>

            <button
              onClick={() => setActiveSubTab('movements')}
              className={`flex items-center gap-2 px-4 py-2 rounded-lg text-xs font-bold transition-all ${
                activeSubTab === 'movements'
                  ? 'bg-white text-slate-900 shadow-sm border border-slate-200'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              <History className="w-4 h-4 text-blue-600" />
              <span>Stock Movement Audit Trail</span>
            </button>
          </div>
        </div>

        {activeSubTab === 'catalog' && (
          <button
            onClick={() => setShowAddModal(true)}
            className="px-4 py-2.5 bg-emerald-600 hover:bg-emerald-500 text-white rounded-lg text-xs font-bold transition-all shadow-md shadow-emerald-900/10 flex items-center gap-2"
          >
            <Plus className="w-4 h-4" />
            <span>Add New Product</span>
          </button>
        )}
      </div>

      {activeSubTab === 'catalog' ? (
        <>
          {/* Search & Category Filters */}
          <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm flex flex-wrap items-center justify-between gap-4">
            <div className="relative flex-1 min-w-[240px]">
              <Search className="w-4 h-4 text-slate-400 absolute left-3 top-3" />
              <input
                type="text"
                placeholder="Search by Product Name, SKU, Color..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="w-full pl-9 pr-4 py-2 border border-slate-300 rounded-lg text-xs focus:ring-2 focus:ring-emerald-500 outline-none"
              />
            </div>

            <div className="flex items-center gap-2">
              <Tag className="w-4 h-4 text-slate-400" />
              <select
                value={selectedCategory || ''}
                onChange={(e) => setSelectedCategory(e.target.value ? Number(e.target.value) : undefined)}
                className="border border-slate-300 rounded-lg px-3 py-2 text-xs focus:ring-2 focus:ring-emerald-500 outline-none bg-white font-medium"
              >
                <option value="">All Categories</option>
                {categories.map((c) => (
                  <option key={c.id} value={c.id}>{c.name}</option>
                ))}
              </select>
            </div>
          </div>

          {/* Product Grid / Table */}
          <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs text-slate-600">
                <thead className="bg-slate-50 text-slate-700 uppercase font-semibold border-b border-slate-200">
                  <tr>
                    <th className="py-3.5 px-4">Product & Category</th>
                    <th className="py-3.5 px-4">SKU</th>
                    <th className="py-3.5 px-4">Color</th>
                    <th className="py-3.5 px-4">Size</th>
                    <th className="py-3.5 px-4 text-right">Cost Price ($)</th>
                    <th className="py-3.5 px-4 text-right">Selling Price ($)</th>
                    <th className="py-3.5 px-4 text-center">Stock Quantity</th>
                    <th className="py-3.5 px-4 text-center">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-200">
                  {products.length === 0 ? (
                    <tr>
                      <td colSpan={8} className="py-8 text-center text-slate-400">
                        No products found matching filters.
                      </td>
                    </tr>
                  ) : (
                    products.flatMap((prod) =>
                      prod.variants.map((v) => (
                        <tr key={v.id} className="hover:bg-slate-50 transition-colors">
                          <td className="py-3 px-4">
                            <span className="font-bold text-slate-900 block text-sm">{prod.name}</span>
                            <span className="text-[10px] bg-slate-100 text-slate-600 px-2 py-0.5 rounded font-semibold border border-slate-200">
                              {prod.category_name}
                            </span>
                          </td>
                          <td className="py-3 px-4 font-mono font-semibold text-slate-700">{v.sku}</td>
                          <td className="py-3 px-4 font-medium text-slate-800">{v.color}</td>
                          <td className="py-3 px-4 font-bold text-slate-700">{v.size}</td>
                          <td className="py-3 px-4 text-right font-medium text-slate-600">${Number(v.cost_price).toFixed(2)}</td>
                          <td className="py-3 px-4 text-right font-bold text-emerald-700">${Number(v.selling_price).toFixed(2)}</td>
                          <td className="py-3 px-4 text-center">
                            <span className={`inline-block px-2.5 py-1 rounded-full font-extrabold text-xs ${
                              v.stock_quantity <= v.min_stock_alert
                                ? 'bg-rose-100 text-rose-700 border border-rose-200'
                                : 'bg-emerald-100 text-emerald-700 border border-emerald-200'
                            }`}>
                              {v.stock_quantity}
                            </span>
                          </td>
                          <td className="py-3 px-4 text-center">
                            <button
                              onClick={() => {
                                setEditingVariant(v);
                                setEditCostPrice(v.cost_price);
                                setEditSellingPrice(v.selling_price);
                                setEditStockQty(v.stock_quantity);
                              }}
                              className="p-1.5 text-blue-600 hover:bg-blue-50 rounded-md transition-colors border border-blue-200"
                              title="Edit Variant"
                            >
                              <Edit className="w-3.5 h-3.5" />
                            </button>
                          </td>
                        </tr>
                      ))
                    )
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </>
      ) : (
        /* Stock Movement Audit Log */
        <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
          <div className="p-4 border-b border-slate-200 flex items-center justify-between">
            <h3 className="font-bold text-slate-800 text-base">Inventory Audit Trail Log</h3>
            <span className="text-xs text-slate-500 font-medium">Showing latest stock movements</span>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs text-slate-600">
              <thead className="bg-slate-50 text-slate-700 uppercase font-semibold border-b border-slate-200">
                <tr>
                  <th className="py-3 px-4">Date & Time</th>
                  <th className="py-3 px-4">Product / SKU</th>
                  <th className="py-3 px-4">Movement Type</th>
                  <th className="py-3 px-4 text-center">Quantity Change</th>
                  <th className="py-3 px-4 text-center">Stock Audit</th>
                  <th className="py-3 px-4">Reference #</th>
                  <th className="py-3 px-4">Logged By</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200">
                {movements.map((m) => (
                  <tr key={m.id} className="hover:bg-slate-50">
                    <td className="py-3 px-4 text-slate-500">{new Date(m.created_at).toLocaleString()}</td>
                    <td className="py-3 px-4">
                      <span className="font-bold text-slate-900 block">{m.product_name} ({m.color}, {m.size})</span>
                      <span className="font-mono text-[10px] text-slate-400">{m.sku}</span>
                    </td>
                    <td className="py-3 px-4 font-semibold">
                      <span className={`px-2 py-0.5 rounded text-[10px] uppercase font-bold border ${
                        m.movement_type.includes('SALE') ? 'bg-amber-50 text-amber-700 border-amber-200' :
                        m.movement_type.includes('PURCHASE') ? 'bg-blue-50 text-blue-700 border-blue-200' :
                        'bg-purple-50 text-purple-700 border-purple-200'
                      }`}>
                        {m.movement_type}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-center font-bold text-sm">
                      <span className={m.quantity > 0 ? 'text-emerald-600' : 'text-rose-600'}>
                        {m.quantity > 0 ? `+${m.quantity}` : m.quantity}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-center font-mono">
                      {m.previous_stock} &rarr; <span className="font-bold text-slate-900">{m.new_stock}</span>
                    </td>
                    <td className="py-3 px-4 font-mono text-slate-700">{m.reference_id || 'N/A'}</td>
                    <td className="py-3 px-4 font-medium text-slate-700">{m.created_by_name || 'System'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Add Product Modal */}
      {showAddModal && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl border border-slate-200 max-w-lg w-full p-6 shadow-2xl space-y-4">
            <div className="flex items-center justify-between border-b border-slate-200 pb-3">
              <h3 className="text-lg font-bold text-slate-900">Add New Product & Initial Variant</h3>
              <button onClick={() => setShowAddModal(false)} className="text-slate-400 hover:text-slate-600">
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleCreateProduct} className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">Product Name</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Essential Heavyweight Hoodie"
                  value={newProdName}
                  onChange={(e) => setNewProdName(e.target.value)}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg text-xs focus:ring-2 focus:ring-emerald-500 outline-none"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">Category</label>
                  <select
                    value={newProdCat}
                    onChange={(e) => setNewProdCat(Number(e.target.value))}
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg text-xs focus:ring-2 focus:ring-emerald-500 outline-none bg-white font-medium"
                  >
                    {categories.map((c) => (
                      <option key={c.id} value={c.id}>{c.name}</option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">Color</label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. Black / Light Oatmeal"
                    value={newColor}
                    onChange={(e) => setNewColor(e.target.value)}
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg text-xs focus:ring-2 focus:ring-emerald-500 outline-none"
                  />
                </div>
              </div>

              <div className="grid grid-cols-3 gap-3">
                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">Size</label>
                  <select
                    value={newSize}
                    onChange={(e) => setNewSize(e.target.value)}
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg text-xs focus:ring-2 focus:ring-emerald-500 outline-none bg-white"
                  >
                    {['XS', 'S', 'M', 'L', 'XL', 'XXL'].map((s) => (
                      <option key={s} value={s}>{s}</option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">Cost Price ($)</label>
                  <input
                    type="number"
                    step="0.01"
                    required
                    value={newCostPrice}
                    onChange={(e) => setNewCostPrice(Number(e.target.value))}
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg text-xs focus:ring-2 focus:ring-emerald-500 outline-none"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">Selling Price ($)</label>
                  <input
                    type="number"
                    step="0.01"
                    required
                    value={newSellingPrice}
                    onChange={(e) => setNewSellingPrice(Number(e.target.value))}
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg text-xs focus:ring-2 focus:ring-emerald-500 outline-none font-bold text-emerald-700"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">Initial Stock Qty</label>
                  <input
                    type="number"
                    required
                    value={newStockQty}
                    onChange={(e) => setNewStockQty(Number(e.target.value))}
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg text-xs focus:ring-2 focus:ring-emerald-500 outline-none font-bold"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">Min Stock Alert</label>
                  <input
                    type="number"
                    required
                    value={newMinAlert}
                    onChange={(e) => setNewMinAlert(Number(e.target.value))}
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg text-xs focus:ring-2 focus:ring-emerald-500 outline-none"
                  />
                </div>
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
                  className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-lg text-xs font-bold shadow-md shadow-emerald-900/10"
                >
                  Save Product
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Edit Variant Modal */}
      {editingVariant && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl border border-slate-200 max-w-md w-full p-6 shadow-2xl space-y-4">
            <div className="flex items-center justify-between border-b border-slate-200 pb-3">
              <div>
                <h3 className="text-base font-bold text-slate-900">Edit Variant: {editingVariant.sku}</h3>
                <p className="text-xs text-slate-500">{editingVariant.product_name} ({editingVariant.color} - {editingVariant.size})</p>
              </div>
              <button onClick={() => setEditingVariant(null)} className="text-slate-400 hover:text-slate-600">
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleUpdateVariant} className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">Cost Price ($)</label>
                <input
                  type="number"
                  step="0.01"
                  required
                  value={editCostPrice}
                  onChange={(e) => setEditCostPrice(Number(e.target.value))}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg text-xs focus:ring-2 focus:ring-emerald-500 outline-none"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">Selling Price ($)</label>
                <input
                  type="number"
                  step="0.01"
                  required
                  value={editSellingPrice}
                  onChange={(e) => setEditSellingPrice(Number(e.target.value))}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg text-xs focus:ring-2 focus:ring-emerald-500 outline-none font-bold text-emerald-700"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">Current Stock Quantity</label>
                <input
                  type="number"
                  required
                  value={editStockQty}
                  onChange={(e) => setEditStockQty(Number(e.target.value))}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg text-xs focus:ring-2 focus:ring-emerald-500 outline-none font-bold text-slate-900"
                />
              </div>

              <div className="pt-3 flex justify-end gap-3">
                <button
                  type="button"
                  onClick={() => setEditingVariant(null)}
                  className="px-4 py-2 border border-slate-300 rounded-lg text-xs font-semibold text-slate-600 hover:bg-slate-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-lg text-xs font-bold shadow-md"
                >
                  Update Variant
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
