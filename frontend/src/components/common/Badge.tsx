import React from 'react';
import { ApplicationStatus } from '../../types/application';

interface BadgeProps {
  status: ApplicationStatus;
  size?: 'sm' | 'md';
}

const statusConfig: Record<
  ApplicationStatus,
  { label: string; bg: string; text: string; border: string; dot: string }
> = {
  APPLIED: {
    label: 'Applied',
    bg: 'bg-blue-50',
    text: 'text-blue-700',
    border: 'border-blue-200',
    dot: 'bg-blue-500',
  },
  ASSESSMENT: {
    label: 'Assessment',
    bg: 'bg-cyan-50',
    text: 'text-cyan-700',
    border: 'border-cyan-200',
    dot: 'bg-cyan-500',
  },
  INTERVIEW: {
    label: 'Interview',
    bg: 'bg-purple-50',
    text: 'text-purple-700',
    border: 'border-purple-200',
    dot: 'bg-purple-500',
  },
  OFFER: {
    label: 'Offer',
    bg: 'bg-emerald-50',
    text: 'text-emerald-700',
    border: 'border-emerald-200',
    dot: 'bg-emerald-500',
  },
  SELECTED: {
    label: 'Selected',
    bg: 'bg-amber-50',
    text: 'text-amber-700',
    border: 'border-amber-200',
    dot: 'bg-amber-500',
  },
  REJECTED: {
    label: 'Rejected',
    bg: 'bg-rose-50',
    text: 'text-rose-700',
    border: 'border-rose-200',
    dot: 'bg-rose-500',
  },
  WITHDRAWN: {
    label: 'Withdrawn',
    bg: 'bg-slate-100',
    text: 'text-slate-600',
    border: 'border-slate-300',
    dot: 'bg-slate-400',
  },
};

export const Badge: React.FC<BadgeProps> = ({ status, size = 'md' }) => {
  const config = statusConfig[status] || {
    label: status,
    bg: 'bg-slate-50',
    text: 'text-slate-700',
    border: 'border-slate-200',
    dot: 'bg-slate-400',
  };

  const sizeClasses = size === 'sm' ? 'px-2 py-0.5 text-xs' : 'px-2.5 py-1 text-xs';

  return (
    <span
      className={`inline-flex items-center gap-1.5 font-medium rounded-full border ${config.bg} ${config.text} ${config.border} ${sizeClasses}`}
    >
      <span className={`w-1.5 h-1.5 rounded-full ${config.dot}`} aria-hidden="true" />
      {config.label}
    </span>
  );
};
