import React from 'react';
import {
  ExternalLink,
  Pencil,
  Trash2,
  MapPin,
  Calendar,
  ArrowUpDown,
  ArrowUp,
  ArrowDown,
} from 'lucide-react';
import { ApplicationStatus, JobApplication } from '../../types/application';
import { Badge } from '../common/Badge';

interface ApplicationTableProps {
  applications: JobApplication[];
  onEdit: (application: JobApplication) => void;
  onDelete: (application: JobApplication) => void;
  onStatusChange?: (application: JobApplication, newStatus: ApplicationStatus) => void;
  sortBy: string;
  sortDirection: 'asc' | 'desc';
  onSort: (field: string) => void;
}

export const ApplicationTable: React.FC<ApplicationTableProps> = ({
  applications,
  onEdit,
  onDelete,
  onStatusChange,
  sortBy,
  sortDirection,
  onSort,
}) => {
  const renderSortIcon = (field: string) => {
    if (sortBy !== field) {
      return <ArrowUpDown className="w-3.5 h-3.5 text-slate-400 group-hover:text-slate-600" />;
    }
    return sortDirection === 'asc' ? (
      <ArrowUp className="w-3.5 h-3.5 text-brand-600" />
    ) : (
      <ArrowDown className="w-3.5 h-3.5 text-brand-600" />
    );
  };

  return (
    <div className="overflow-hidden bg-white rounded-2xl border border-slate-200/80 shadow-xs">
      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-slate-200/80 text-left">
          <thead className="bg-slate-50/75 text-xs font-semibold uppercase tracking-wider text-slate-500">
            <tr>
              <th scope="col" className="px-6 py-3.5">
                <button
                  onClick={() => onSort('companyName')}
                  className="flex items-center gap-1.5 group cursor-pointer hover:text-slate-900 transition-colors"
                >
                  <span>Company</span>
                  {renderSortIcon('companyName')}
                </button>
              </th>
              <th scope="col" className="px-6 py-3.5">
                <button
                  onClick={() => onSort('jobTitle')}
                  className="flex items-center gap-1.5 group cursor-pointer hover:text-slate-900 transition-colors"
                >
                  <span>Job Title</span>
                  {renderSortIcon('jobTitle')}
                </button>
              </th>
              <th scope="col" className="px-6 py-3.5">
                Status
              </th>
              <th scope="col" className="px-6 py-3.5">
                Location
              </th>
              <th scope="col" className="px-6 py-3.5">
                <button
                  onClick={() => onSort('applicationDate')}
                  className="flex items-center gap-1.5 group cursor-pointer hover:text-slate-900 transition-colors"
                >
                  <span>Applied Date</span>
                  {renderSortIcon('applicationDate')}
                </button>
              </th>
              <th scope="col" className="px-6 py-3.5 text-right">
                Actions
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100 text-sm">
            {applications.map((app) => (
              <tr
                key={app.id}
                className="hover:bg-slate-50/60 transition-colors duration-100 group"
              >
                {/* Company Name */}
                <td className="px-6 py-4 whitespace-nowrap">
                  <div className="flex items-center gap-2">
                    <span className="font-semibold text-slate-900 group-hover:text-brand-600 transition-colors">
                      {app.companyName}
                    </span>
                    {app.jobUrl && (
                      <a
                        href={app.jobUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="text-slate-400 hover:text-brand-600 transition-colors p-0.5 rounded hover:bg-slate-100"
                        title="View job posting"
                      >
                        <ExternalLink className="w-3.5 h-3.5" />
                      </a>
                    )}
                  </div>
                  {app.jobDescription && (
                    <p className="text-xs text-slate-500 max-w-xs truncate mt-0.5" title={app.jobDescription}>
                      <span className="font-medium text-slate-600">Desc: </span>{app.jobDescription}
                    </p>
                  )}
                  {app.notes && (
                    <p className="text-xs text-slate-400 max-w-xs truncate mt-0.5" title={app.notes}>
                      <span className="font-medium text-slate-500">Note: </span>{app.notes}
                    </p>
                  )}
                </td>

                {/* Job Title */}
                <td className="px-6 py-4 whitespace-nowrap">
                  <span className="text-slate-800 font-medium">{app.jobTitle}</span>
                </td>

                {/* Status Badge & Quick Changer */}
                <td className="px-6 py-4 whitespace-nowrap">
                  <div className="flex items-center gap-1.5">
                    <Badge status={app.status} />
                    {onStatusChange && (
                      <select
                        aria-label={`Update status for ${app.companyName}`}
                        value={app.status}
                        onChange={(e) => onStatusChange(app, e.target.value as ApplicationStatus)}
                        className="text-[11px] font-medium text-slate-500 bg-transparent hover:bg-slate-100 rounded px-1.5 py-0.5 border border-transparent hover:border-slate-200 cursor-pointer outline-none transition-colors"
                        title="Quick change status"
                      >
                        <option value="APPLIED">Applied</option>
                        <option value="ASSESSMENT">Assessment</option>
                        <option value="INTERVIEW">Interview</option>
                        <option value="OFFER">Offer</option>
                        <option value="SELECTED">Selected</option>
                        <option value="REJECTED">Rejected</option>
                        <option value="WITHDRAWN">Withdrawn</option>
                      </select>
                    )}
                  </div>
                </td>

                {/* Location */}
                <td className="px-6 py-4 whitespace-nowrap text-slate-500">
                  {app.location ? (
                    <div className="flex items-center gap-1 text-xs">
                      <MapPin className="w-3.5 h-3.5 text-slate-400" />
                      <span>{app.location}</span>
                    </div>
                  ) : (
                    <span className="text-xs text-slate-400">—</span>
                  )}
                </td>

                {/* Application Date & Deadline */}
                <td className="px-6 py-4 whitespace-nowrap text-slate-500 text-xs">
                  <div className="flex items-center gap-1.5">
                    <Calendar className="w-3.5 h-3.5 text-slate-400" />
                    <span>{app.applicationDate || '—'}</span>
                  </div>
                  {app.deadline && (
                    <div className="flex items-center gap-1 text-[11px] text-amber-600 font-medium mt-1">
                      <span>Due: {app.deadline}</span>
                    </div>
                  )}
                </td>

                {/* Actions */}
                <td className="px-6 py-4 whitespace-nowrap text-right text-xs font-medium">
                  <div className="flex items-center justify-end gap-1">
                    <button
                      onClick={() => onEdit(app)}
                      className="p-1.5 rounded-lg text-slate-500 hover:text-brand-600 hover:bg-brand-50 transition-colors cursor-pointer"
                      title="Edit application"
                      aria-label={`Edit ${app.companyName}`}
                    >
                      <Pencil className="w-4 h-4" />
                    </button>
                    <button
                      onClick={() => onDelete(app)}
                      className="p-1.5 rounded-lg text-slate-500 hover:text-rose-600 hover:bg-rose-50 transition-colors cursor-pointer"
                      title="Delete application"
                      aria-label={`Delete ${app.companyName}`}
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};
