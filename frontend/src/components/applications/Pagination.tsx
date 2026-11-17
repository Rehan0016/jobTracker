import React from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';

interface PaginationProps {
  currentPage: number;
  totalPages: number;
  totalElements: number;
  pageSize: number;
  onPageChange: (page: number) => void;
  onPageSizeChange: (size: number) => void;
}

export const Pagination: React.FC<PaginationProps> = ({
  currentPage,
  totalPages,
  totalElements,
  pageSize,
  onPageChange,
  onPageSizeChange,
}) => {
  if (totalElements === 0) return null;

  const startItem = currentPage * pageSize + 1;
  const endItem = Math.min((currentPage + 1) * pageSize, totalElements);

  return (
    <div className="flex flex-col sm:flex-row items-center justify-between gap-4 px-4 py-3 bg-white rounded-2xl border border-slate-200/80 shadow-xs">
      <div className="flex items-center gap-3 text-xs text-slate-500">
        <span>
          Showing <strong className="font-semibold text-slate-900">{startItem}</strong> to{' '}
          <strong className="font-semibold text-slate-900">{endItem}</strong> of{' '}
          <strong className="font-semibold text-slate-900">{totalElements}</strong> applications
        </span>
        <div className="flex items-center gap-1.5 pl-3 border-l border-slate-200">
          <span>Rows:</span>
          <select
            value={pageSize}
            onChange={(e) => onPageSizeChange(Number(e.target.value))}
            className="border border-slate-200 rounded-lg px-2 py-1 text-xs text-slate-700 bg-slate-50 font-medium focus:outline-none focus:ring-1 focus:ring-brand-500 cursor-pointer"
          >
            <option value={10}>10</option>
            <option value={20}>20</option>
            <option value={50}>50</option>
          </select>
        </div>
      </div>

      <div className="flex items-center gap-1">
        <button
          onClick={() => onPageChange(currentPage - 1)}
          disabled={currentPage === 0}
          className="p-1.5 rounded-lg border border-slate-200 text-slate-600 hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer transition-colors"
          aria-label="Previous page"
        >
          <ChevronLeft className="w-4 h-4" />
        </button>

        <span className="px-3 py-1 text-xs font-semibold text-slate-700 bg-slate-50 rounded-lg border border-slate-200">
          Page {currentPage + 1} of {Math.max(totalPages, 1)}
        </span>

        <button
          onClick={() => onPageChange(currentPage + 1)}
          disabled={currentPage >= totalPages - 1}
          className="p-1.5 rounded-lg border border-slate-200 text-slate-600 hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer transition-colors"
          aria-label="Next page"
        >
          <ChevronRight className="w-4 h-4" />
        </button>
      </div>
    </div>
  );
};
