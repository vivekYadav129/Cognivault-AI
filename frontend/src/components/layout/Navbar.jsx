import React from 'react';
import { Menu, Upload, MessageSquarePlus, User, CheckCircle2 } from 'lucide-react';
import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

export const Navbar = ({ onOpenSidebar }) => {
  const { user } = useAuth();
  const location = useLocation();

  const getPageTitle = (pathname) => {
    if (pathname === '/dashboard') return 'Dashboard Overview';
    if (pathname === '/documents') return 'Knowledge Documents';
    if (pathname.startsWith('/documents/')) return 'Document Details & Chunks';
    if (pathname === '/chat') return 'AI Knowledge Chat (RAG)';
    if (pathname === '/quiz') return 'Interactive Quiz Arena';
    if (pathname === '/profile') return 'User Profile & Security';
    return 'Cognivault AI';
  };

  return (
    <header className="sticky top-0 z-30 h-16 bg-slate-900/80 backdrop-blur-md border-b border-slate-800/80 px-4 sm:px-6 flex items-center justify-between">
      <div className="flex items-center gap-3">
        {/* Mobile toggle */}
        <button
          onClick={onOpenSidebar}
          className="p-2 rounded-xl text-slate-400 hover:text-white hover:bg-slate-800/80 lg:hidden"
        >
          <Menu className="w-5 h-5" />
        </button>

        <div>
          <h1 className="text-base sm:text-lg font-bold text-white tracking-tight">
            {getPageTitle(location.pathname)}
          </h1>
          <p className="text-[11px] text-slate-400 hidden sm:block">
            Grounding AI decisions with enterprise vector retrieval
          </p>
        </div>
      </div>

      <div className="flex items-center gap-2 sm:gap-4">
        {/* Live Backend status badge */}
        <div className="hidden md:flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-xs font-medium">
          <CheckCircle2 className="w-3.5 h-3.5" />
          <span>Spring Boot 3 API Online</span>
        </div>

        {/* Quick actions */}
        <Link
          to="/documents"
          className="hidden sm:inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-semibold bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 transition-colors"
        >
          <Upload className="w-3.5 h-3.5 text-brand-400" />
          <span>Upload PDF</span>
        </Link>

        <Link
          to="/chat"
          className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-semibold bg-brand-600 hover:bg-brand-500 text-white transition-all shadow-md shadow-brand-500/20"
        >
          <MessageSquarePlus className="w-3.5 h-3.5" />
          <span>New Chat</span>
        </Link>

        {/* Profile shortcut */}
        <Link
          to="/profile"
          className="w-8 h-8 rounded-xl bg-slate-800 border border-slate-700 flex items-center justify-center text-slate-300 hover:text-white hover:border-brand-500/50 transition-colors"
          title="Account Profile"
        >
          <User className="w-4 h-4" />
        </Link>
      </div>
    </header>
  );
};

export default Navbar;
