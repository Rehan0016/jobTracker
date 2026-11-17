import React from 'react';
import { Inbox } from 'lucide-react';
import { Button } from './Button';

interface EmptyStateProps {
  title: string;
  description: string;
  icon?: React.ReactNode;
  actionText?: string;
  onAction?: () => void;
}

export const EmptyState: React.FC<EmptyStateProps> = ({
  title,
  description,
  icon,
  actionText,
  onAction,
}) => {
  return (
    <div className="flex flex-col items-center justify-center py-16 px-4 text-center rounded-2xl bg-white border border-dashed border-slate-200">
      <div className="w-16 h-16 rounded-2xl bg-slate-50 flex items-center justify-center text-slate-400 mb-4 border border-slate-100 shadow-sm">
        {icon || <Inbox className="w-8 h-8 stroke-[1.5]" />}
      </div>
      <h3 className="text-base font-semibold text-slate-900">{title}</h3>
      <p className="mt-1.5 text-sm text-slate-500 max-w-sm">{description}</p>
      {actionText && onAction && (
        <div className="mt-6">
          <Button onClick={onAction} variant="primary" size="md">
            {actionText}
          </Button>
        </div>
      )}
    </div>
  );
};
