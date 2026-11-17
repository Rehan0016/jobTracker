import React, { SelectHTMLAttributes, forwardRef } from 'react';

interface Option {
  value: string;
  label: string;
}

interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  label?: string;
  options: Option[];
  error?: string;
  helperText?: string;
}

export const Select = forwardRef<HTMLSelectElement, SelectProps>(
  ({ label, options, error, helperText, id, className = '', required, ...props }, ref) => {
    const generatedId = React.useId();
    const selectId = id || props.name || generatedId;

    return (
      <div className="w-full">
        {label && (
          <label htmlFor={selectId} className="block text-sm font-medium text-slate-700 mb-1.5">
            {label}
            {required && <span className="text-rose-500 ml-1">*</span>}
          </label>
        )}
        <select
          ref={ref}
          id={selectId}
          required={required}
          className={`block w-full rounded-lg border text-sm transition-colors duration-150 focus:outline-none focus:ring-2 focus:ring-offset-0 pl-3.5 pr-8 py-2.5 bg-white disabled:bg-slate-50 disabled:text-slate-500 ${
            error
              ? 'border-rose-300 text-rose-900 focus:border-rose-500 focus:ring-rose-200'
              : 'border-slate-300 text-slate-900 hover:border-slate-400 focus:border-brand-500 focus:ring-brand-100'
          } ${className}`}
          {...props}
        >
          {options.map((opt) => (
            <option key={opt.value} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </select>
        {error && <p className="mt-1 text-xs text-rose-600 font-medium">{error}</p>}
        {helperText && !error && <p className="mt-1 text-xs text-slate-500">{helperText}</p>}
      </div>
    );
  }
);

Select.displayName = 'Select';
