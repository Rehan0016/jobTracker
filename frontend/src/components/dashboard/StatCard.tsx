import React from 'react';

interface StatCardProps {
  title: string;
  value: number | string;
  icon: React.ReactNode;
  color: 'blue' | 'purple' | 'emerald' | 'amber' | 'rose' | 'indigo' | 'cyan' | 'teal';
  subtitle?: string;
}

const colorStyles = {
  blue: {
    bg: 'bg-blue-50',
    text: 'text-blue-600',
    border: 'border-blue-100',
    ring: 'focus:ring-blue-500',
  },
  purple: {
    bg: 'bg-purple-50',
    text: 'text-purple-600',
    border: 'border-purple-100',
    ring: 'focus:ring-purple-500',
  },
  emerald: {
    bg: 'bg-emerald-50',
    text: 'text-emerald-600',
    border: 'border-emerald-100',
    ring: 'focus:ring-emerald-500',
  },
  amber: {
    bg: 'bg-amber-50',
    text: 'text-amber-600',
    border: 'border-amber-100',
    ring: 'focus:ring-amber-500',
  },
  rose: {
    bg: 'bg-rose-50',
    text: 'text-rose-600',
    border: 'border-rose-100',
    ring: 'focus:ring-rose-500',
  },
  indigo: {
    bg: 'bg-indigo-50',
    text: 'text-indigo-600',
    border: 'border-indigo-100',
    ring: 'focus:ring-indigo-500',
  },
  cyan: {
    bg: 'bg-cyan-50',
    text: 'text-cyan-600',
    border: 'border-cyan-100',
    ring: 'focus:ring-cyan-500',
  },
  teal: {
    bg: 'bg-teal-50',
    text: 'text-teal-600',
    border: 'border-teal-100',
    ring: 'focus:ring-teal-500',
  },
};

export const StatCard: React.FC<StatCardProps> = ({
  title,
  value,
  icon,
  color,
  subtitle,
}) => {
  const styles = colorStyles[color];

  return (
    <div className="bg-white rounded-2xl p-6 border border-slate-200/80 shadow-xs hover:shadow-md transition-shadow duration-200">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">{title}</p>
          <p className="text-3xl font-extrabold text-slate-900 mt-2 tracking-tight">{value}</p>
          {subtitle && <p className="text-xs text-slate-400 mt-1">{subtitle}</p>}
        </div>
        <div className={`w-12 h-12 rounded-xl flex items-center justify-center border ${styles.bg} ${styles.text} ${styles.border}`}>
          {icon}
        </div>
      </div>
    </div>
  );
};
