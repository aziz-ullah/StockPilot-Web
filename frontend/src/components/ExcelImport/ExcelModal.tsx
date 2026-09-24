import React, { useState } from 'react';
import { excelService } from '../../services/api';
import { ImportSummary } from '../../types';
import { Upload, Download, X, CheckCircle, FileSpreadsheet, AlertCircle } from 'lucide-react';

interface ExcelModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

export const ExcelModal: React.FC<ExcelModalProps> = ({ isOpen, onClose, onSuccess }) => {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState<boolean>(false);
  const [summary, setSummary] = useState<ImportSummary | null>(null);

  if (!isOpen) return null;

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      setSelectedFile(e.target.files[0]);
    }
  };

  const handleUpload = async () => {
    if (!selectedFile) return;
    setUploading(true);
    try {
      const res = await excelService.importExcel(selectedFile);
      setSummary(res.summary);
      onSuccess();
    } catch (err: any) {
      alert(err.response?.data?.detail || "Failed to import Excel file");
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-2xl border border-slate-200 max-w-lg w-full p-6 shadow-2xl space-y-5">
        <div className="flex items-center justify-between border-b border-slate-200 pb-3">
          <h3 className="text-base font-bold text-slate-900 flex items-center gap-2">
            <FileSpreadsheet className="w-5 h-5 text-emerald-600" />
            <span>Excel Importer & Template System</span>
          </h3>
          <button onClick={onClose} className="text-slate-400 hover:text-slate-600">
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Download Standard Template Button */}
        <div className="bg-slate-50 p-4 rounded-xl border border-slate-200 flex items-center justify-between">
          <div>
            <h4 className="text-xs font-bold text-slate-900">Download Standard Template</h4>
            <p className="text-[11px] text-slate-500">Get the standard .xlsx template for future bulk inventory uploads</p>
          </div>
          <button
            onClick={excelService.downloadTemplate}
            className="px-3 py-1.5 bg-white border border-slate-300 hover:border-emerald-500 text-slate-700 hover:text-emerald-700 rounded-lg text-xs font-bold transition-all shadow-sm flex items-center gap-1.5 shrink-0"
          >
            <Download className="w-3.5 h-3.5" />
            <span>Template (.xlsx)</span>
          </button>
        </div>

        {/* File Drop Area */}
        {!summary ? (
          <div className="space-y-4">
            <div className="border-2 border-dashed border-slate-300 hover:border-emerald-500 rounded-2xl p-6 text-center space-y-3 transition-colors bg-slate-50/50">
              <Upload className="w-10 h-10 text-emerald-600 mx-auto" />
              <div>
                <p className="text-xs font-bold text-slate-800">Select or Drag Excel Workbook (.xlsx)</p>
                <p className="text-[11px] text-slate-400 mt-0.5">Supports multi-sheet inventory & historical sales workbooks</p>
              </div>
              <input
                type="file"
                accept=".xlsx, .xls"
                onChange={handleFileChange}
                className="block w-full text-xs text-slate-500 file:mr-4 file:py-2 file:px-4 file:rounded-lg file:border-0 file:text-xs file:font-semibold file:bg-emerald-50 file:text-emerald-700 hover:file:bg-emerald-100 cursor-pointer"
              />
            </div>

            {selectedFile && (
              <div className="flex items-center justify-between bg-emerald-50 p-3 rounded-lg border border-emerald-200 text-xs">
                <span className="font-bold text-emerald-900">{selectedFile.name} ({Math.round(selectedFile.size / 1024)} KB)</span>
                <button
                  onClick={handleUpload}
                  disabled={uploading}
                  className="px-4 py-1.5 bg-emerald-600 hover:bg-emerald-500 text-white font-bold rounded-md transition-all shadow-sm"
                >
                  {uploading ? "Processing..." : "Start Import"}
                </button>
              </div>
            )}
          </div>
        ) : (
          /* Import Result Summary */
          <div className="space-y-4 bg-emerald-50/60 p-4 rounded-xl border border-emerald-200">
            <div className="flex items-center gap-2 text-emerald-800 font-bold text-sm">
              <CheckCircle className="w-5 h-5 text-emerald-600" />
              <span>Excel Import Completed Successfully!</span>
            </div>

            <div className="grid grid-cols-2 gap-2 text-xs">
              <div className="bg-white p-2.5 rounded-lg border border-emerald-100">
                <span className="text-slate-500 block text-[10px]">Categories Created</span>
                <span className="font-bold text-slate-900 text-sm">{summary.categories_created}</span>
              </div>
              <div className="bg-white p-2.5 rounded-lg border border-emerald-100">
                <span className="text-slate-500 block text-[10px]">Products Created</span>
                <span className="font-bold text-slate-900 text-sm">{summary.products_created}</span>
              </div>
              <div className="bg-white p-2.5 rounded-lg border border-emerald-100">
                <span className="text-slate-500 block text-[10px]">Variants Created</span>
                <span className="font-bold text-slate-900 text-sm">{summary.variants_created}</span>
              </div>
              <div className="bg-white p-2.5 rounded-lg border border-emerald-100">
                <span className="text-slate-500 block text-[10px]">Unsold Stock Added</span>
                <span className="font-bold text-emerald-600 text-sm">+{summary.stock_units_added}</span>
              </div>
              <div className="bg-white p-2.5 rounded-lg border border-emerald-100 col-span-2">
                <span className="text-slate-500 block text-[10px]">Historical Sales Transactions Imported</span>
                <span className="font-black text-slate-900 text-base">{summary.historical_sales_imported} Orders</span>
              </div>
            </div>

            <button
              onClick={() => { setSummary(null); setSelectedFile(null); onClose(); }}
              className="w-full py-2 bg-emerald-600 text-white rounded-lg text-xs font-bold"
            >
              Done & Refresh Views
            </button>
          </div>
        )}
      </div>
    </div>
  );
};
