import React from 'react';
import { Menu, Bell } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';

interface NavbarProps {
  onMenuClick: () => void;
  title: string;
}

export const Navbar: React.FC<NavbarProps> = ({ onMenuClick, title }) => {
  const { user } = useAuth();

  return (
    <header className="sticky top-0 z-30 flex items-center justify-between h-16 px-4 sm:px-6 bg-white/80 backdrop-blur-md border-b border-slate-200/80">
      <div className="flex items-center gap-3">
        <button
          onClick={onMenuClick}
          className="p-2 rounded-lg text-slate-500 hover:text-slate-700 hover:bg-slate-100 lg:hidden cursor-pointer"
          aria-label="Open sidebar"
        >
          <Menu className="w-5 h-5" />
        </button>
        <h1 className="text-xl font-bold text-slate-900 tracking-tight">{title}</h1>
      </div>

      <div className="flex items-center gap-3">
        <button
          className="p-2 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition-colors relative cursor-pointer"
          aria-label="Notifications"
        >
          <Bell className="w-5 h-5" />
          <span className="absolute top-1.5 right-1.5 w-2 h-2 rounded-full bg-brand-500" />
        </button>

        <div className="hidden sm:flex items-center gap-2 pl-3 border-l border-slate-200">
          <div className="w-8 h-8 rounded-full bg-slate-100 text-brand-700 flex items-center justify-center font-bold text-xs border border-slate-200">
            {user?.name ? user.name.charAt(0).toUpperCase() : 'U'}
          </div>
          <span className="text-sm font-medium text-slate-700">{user?.name}</span>
        </div>
      </div>
    </header>
  );
};
