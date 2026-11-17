import React from 'react';
import { Search, Plus, Filter } from 'lucide-react';
import { ApplicationStatus } from '../../types/application';
import { Button } from '../common/Button';

interface ApplicationFilterBarProps {
  search: string;
  onSearchChange: (value: string) => void;
  statusFilter: ApplicationStatus | 'ALL';
  onStatusChange: (status: ApplicationStatus | 'ALL') => void;
  companyFilter?: string;
  onCompanyChange?: (value: string) => void;
  titleFilter?: string;
  onTitleChange?: (value: string) => void;
  onResetFilters?: () => void;
  onNewClick: () => void;
}

export const ApplicationFilterBar: React.FC<ApplicationFilterBarProps> = ({
  search,
  onSearchChange,
  statusFilter,
  onStatusChange,
  companyFilter = '',
  onCompanyChange,
  titleFilter = '',
  onTitleChange,
  onResetFilters,
  onNewClick,
}) => {
  return (
    <div className="bg-white p-4 rounded-2xl border border-slate-200/80 shadow-xs space-y-3">
      <div className="flex flex-col lg:flex-row items-stretch lg:items-center justify-between gap-3">
        {/* Search input */}
        <div className="relative flex-1">
          <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
            <Search className="w-4 h-4" />
          </div>
          <input
            type="text"
            value={search}
            onChange={(e) => onSearchChange(e.target.value)}
            placeholder="Search anything (company, role, notes)..."
            className="w-full pl-10 pr-4 py-2 text-sm rounded-xl border border-slate-200 bg-slate-50/50 hover:bg-white focus:bg-white focus:border-brand-500 focus:ring-2 focus:ring-brand-100 transition-all outline-none"
          />
        </div>

        {/* Company filter */}
        {onCompanyChange && (
          <div className="relative min-w-[160px]">
            <input
              type="text"
              value={companyFilter}
              onChange={(e) => onCompanyChange(e.target.value)}
              placeholder="Filter company..."
              className="w-full px-3 py-2 text-sm rounded-xl border border-slate-200 bg-slate-50/50 hover:bg-white focus:bg-white focus:border-brand-500 focus:ring-2 focus:ring-brand-100 transition-all outline-none"
            />
          </div>
        )}

        {/* Role / Job Title filter */}
        {onTitleChange && (
          <div className="relative min-w-[160px]">
            <input
              type="text"
              value={titleFilter}
              onChange={(e) => onTitleChange(e.target.value)}
              placeholder="Filter job role..."
              className="w-full px-3 py-2 text-sm rounded-xl border border-slate-200 bg-slate-50/50 hover:bg-white focus:bg-white focus:border-brand-500 focus:ring-2 focus:ring-brand-100 transition-all outline-none"
            />
          </div>
        )}

        {/* Status filter dropdown */}
        <div className="relative min-w-[160px]">
          <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400">
            <Filter className="w-3.5 h-3.5" />
          </div>
          <select
            value={statusFilter}
            onChange={(e) => onStatusChange(e.target.value as ApplicationStatus | 'ALL')}
            className="w-full pl-9 pr-8 py-2 text-sm rounded-xl border border-slate-200 bg-slate-50/50 hover:bg-white focus:bg-white focus:border-brand-500 focus:ring-2 focus:ring-brand-100 transition-all outline-none text-slate-700 font-medium cursor-pointer"
          >
            <option value="ALL">All Statuses</option>
            <option value="APPLIED">Applied</option>
            <option value="ASSESSMENT">Assessment</option>
            <option value="INTERVIEW">Interview</option>
            <option value="OFFER">Offer</option>
            <option value="SELECTED">Selected</option>
            <option value="REJECTED">Rejected</option>
            <option value="WITHDRAWN">Withdrawn</option>
          </select>
        </div>

        {onResetFilters && (search || companyFilter || titleFilter || statusFilter !== 'ALL') && (
          <button
            onClick={onResetFilters}
            className="text-xs text-brand-600 hover:text-brand-700 font-semibold px-2 py-1 cursor-pointer whitespace-nowrap"
          >
            Reset
          </button>
        )}

        {/* Action button */}
        <div>
          <Button
            onClick={onNewClick}
            variant="primary"
            size="md"
            leftIcon={<Plus className="w-4 h-4" />}
            className="w-full sm:w-auto whitespace-nowrap"
          >
            Add Application
          </Button>
        </div>
      </div>
    </div>
  );
};
