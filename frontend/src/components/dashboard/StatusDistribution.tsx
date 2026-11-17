import React from 'react';
import { DashboardStats } from '../../types/application';

interface StatusDistributionProps {
  stats: DashboardStats;
}

export const StatusDistribution: React.FC<StatusDistributionProps> = ({ stats }) => {
  const total = stats.totalApplications || 1; // avoid divide by zero

  const items = [
    { label: 'Applied', count: stats.applied, color: 'bg-blue-500', textColor: 'text-blue-700' },
    { label: 'Assessment', count: stats.assessment ?? 0, color: 'bg-cyan-500', textColor: 'text-cyan-700' },
    { label: 'Interviews', count: stats.interviews, color: 'bg-purple-500', textColor: 'text-purple-700' },
    { label: 'Offers', count: stats.offers, color: 'bg-emerald-500', textColor: 'text-emerald-700' },
    { label: 'Selected', count: stats.selected, color: 'bg-amber-500', textColor: 'text-amber-700' },
    { label: 'Rejected', count: stats.rejected, color: 'bg-rose-500', textColor: 'text-rose-700' },
    { label: 'Withdrawn', count: stats.withdrawn ?? 0, color: 'bg-slate-400', textColor: 'text-slate-600' },
  ];

  return (
    <div className="bg-white rounded-2xl p-6 border border-slate-200/80 shadow-xs">
      <h3 className="text-base font-bold text-slate-900 mb-1">Application Pipeline</h3>
      <p className="text-xs text-slate-500 mb-6">Distribution across current hiring stages</p>

      {/* Progress segmented bar */}
      <div className="w-full h-3 bg-slate-100 rounded-full overflow-hidden flex mb-6">
        {stats.totalApplications === 0 ? (
          <div className="w-full h-full bg-slate-200" />
        ) : (
          items.map((item) => {
            const percentage = (item.count / total) * 100;
            if (percentage === 0) return null;
            return (
              <div
                key={item.label}
                style={{ width: `${percentage}%` }}
                className={`h-full ${item.color} transition-all duration-300`}
                title={`${item.label}: ${item.count} (${percentage.toFixed(0)}%)`}
              />
            );
          })
        )}
      </div>

      {/* Breakdown list */}
      <div className="grid grid-cols-2 sm:grid-cols-4 lg:grid-cols-7 gap-3">
        {items.map((item) => {
          const pct = stats.totalApplications > 0 ? ((item.count / total) * 100).toFixed(0) : '0';
          return (
            <div key={item.label} className="p-3 rounded-xl bg-slate-50 border border-slate-100">
              <div className="flex items-center gap-2 mb-1">
                <span className={`w-2 h-2 rounded-full ${item.color}`} />
                <span className="text-xs font-medium text-slate-600">{item.label}</span>
              </div>
              <div className="flex items-baseline justify-between mt-1">
                <span className="text-lg font-bold text-slate-900">{item.count}</span>
                <span className="text-xs text-slate-400 font-medium">{pct}%</span>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};
