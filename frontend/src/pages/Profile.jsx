import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { documentService } from '../services/documentService';
import {
  User,
  Mail,
  Shield,
  Calendar,
  Key,
  Database,
  RefreshCw,
  LogOut,
  Copy,
  Check,
  CheckCircle2,
  HardDrive,
  Layers,
  Files
} from 'lucide-react';
import toast from 'react-hot-toast';

export const Profile = () => {
  const { user, token, logout, refreshUser } = useAuth();
  const navigate = useNavigate();

  const [refreshing, setRefreshing] = useState(false);
  const [tokenCopied, setTokenCopied] = useState(false);
  const [stats, setStats] = useState({ docCount: 0, chunkCount: 0, totalSize: 0 });
  const [loadingStats, setLoadingStats] = useState(true);

  useEffect(() => {
    const fetchUserStats = async () => {
      try {
        const data = await documentService.getDocuments();
        const docs = data.documents || [];
        const totalChunks = docs.reduce((sum, d) => sum + (d.chunkCount || 0), 0);
        const totalBytes = docs.reduce((sum, d) => sum + (d.fileSize || 0), 0);
        setStats({
          docCount: docs.length,
          chunkCount: totalChunks,
          totalSize: totalBytes,
        });
      } catch (err) {
        console.error('Failed to load user document stats', err);
      } finally {
        setLoadingStats(false);
      }
    };
    fetchUserStats();
  }, []);

  const handleRefresh = async () => {
    setRefreshing(true);
    await refreshUser();
    setRefreshing(false);
    toast.success('User profile refreshed');
  };

  const handleCopyToken = () => {
    if (token) {
      navigator.clipboard.writeText(token);
      setTokenCopied(true);
      toast.success('JWT Token copied to clipboard');
      setTimeout(() => setTokenCopied(false), 2000);
    }
  };

  const handleLogout = () => {
    logout();
    toast.success('Signed out successfully');
    navigate('/login');
  };

  return (
    <div className="max-w-4xl mx-auto space-y-8 animate-fade-in">
      {/* Profile Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-white tracking-tight">User Account & Security</h2>
          <p className="text-xs sm:text-sm text-slate-400">
            Manage your personal profile, credentials, and knowledge footprint
          </p>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={handleRefresh}
            disabled={refreshing}
            className="inline-flex items-center gap-1.5 px-3 py-2 rounded-xl text-xs font-semibold bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 transition-colors"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${refreshing ? 'animate-spin' : ''}`} />
            <span>Refresh</span>
          </button>

          <button
            onClick={handleLogout}
            className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl text-xs font-semibold bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 border border-rose-500/30 transition-colors"
          >
            <LogOut className="w-3.5 h-3.5" />
            <span>Sign Out</span>
          </button>
        </div>
      </div>

      {/* Main Profile Info Card */}
      <div className="glass-card rounded-3xl border border-slate-800 p-6 sm:p-8 space-y-6">
        <div className="flex flex-col sm:flex-row sm:items-center gap-5 pb-6 border-b border-slate-800">
          <div className="w-20 h-20 rounded-2xl bg-gradient-to-tr from-brand-600 via-indigo-600 to-purple-600 flex items-center justify-center text-white text-3xl font-extrabold shadow-xl shadow-brand-500/25 border border-brand-400/30 flex-shrink-0">
            {user?.name ? user.name.charAt(0).toUpperCase() : 'U'}
          </div>

          <div className="space-y-1 min-w-0 flex-1">
            <div className="flex flex-wrap items-center gap-2">
              <h3 className="text-xl font-bold text-white">{user?.name || 'User Name'}</h3>
              <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-brand-500/10 text-brand-400 border border-brand-500/20">
                <Shield className="w-3 h-3" />
                {user?.role || 'USER'}
              </span>
            </div>
            <p className="text-sm text-slate-400 flex items-center gap-1.5">
              <Mail className="w-3.5 h-3.5 text-slate-500" />
              <span>{user?.email || 'user@example.com'}</span>
            </p>
            <p className="text-xs text-slate-500 font-mono">
              Database Identifier: User #{user?.id || '-'}
            </p>
          </div>
        </div>

        {/* Detailed Info Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
          <div className="p-4 rounded-2xl bg-slate-900/60 border border-slate-800 space-y-1">
            <span className="text-slate-400 font-semibold uppercase text-[10px] block">
              Member Since
            </span>
            <div className="flex items-center gap-2 text-white font-medium">
              <Calendar className="w-4 h-4 text-brand-400" />
              <span>
                {user?.createdAt ? new Date(user.createdAt).toLocaleDateString() : 'Active session'}
              </span>
            </div>
          </div>

          <div className="p-4 rounded-2xl bg-slate-900/60 border border-slate-800 space-y-1">
            <span className="text-slate-400 font-semibold uppercase text-[10px] block">
              Authentication Provider
            </span>
            <div className="flex items-center gap-2 text-white font-medium">
              <CheckCircle2 className="w-4 h-4 text-emerald-400" />
              <span>Spring Boot JWT Stateless Filter</span>
            </div>
          </div>
        </div>
      </div>

      {/* Knowledge Footprint Metrics */}
      <div className="space-y-4">
        <h3 className="text-base font-bold text-white tracking-tight">
          Personal Knowledge Footprint
        </h3>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div className="glass-card p-5 rounded-2xl border border-slate-800">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">
                Documents Owned
              </span>
              <Files className="w-4 h-4 text-brand-400" />
            </div>
            <p className="text-2xl font-extrabold text-white mt-3">{stats.docCount}</p>
            <p className="text-xs text-slate-500 mt-0.5">Isolated to your user ID</p>
          </div>

          <div className="glass-card p-5 rounded-2xl border border-slate-800">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">
                Total Vector Chunks
              </span>
              <Layers className="w-4 h-4 text-indigo-400" />
            </div>
            <p className="text-2xl font-extrabold text-white mt-3">{stats.chunkCount}</p>
            <p className="text-xs text-slate-500 mt-0.5">Indexed in pgvector</p>
          </div>

          <div className="glass-card p-5 rounded-2xl border border-slate-800">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">
                Storage Footprint
              </span>
              <HardDrive className="w-4 h-4 text-emerald-400" />
            </div>
            <p className="text-2xl font-extrabold text-white mt-3">
              {(stats.totalSize / 1024).toFixed(1)} KB
            </p>
            <p className="text-xs text-slate-500 mt-0.5">Raw uploaded files</p>
          </div>
        </div>
      </div>

      {/* Security & JWT Token Inspection */}
      <div className="glass-card rounded-3xl border border-slate-800 p-6 sm:p-8 space-y-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <Key className="w-4 h-4 text-brand-400" />
            <h4 className="text-sm font-bold text-white">Active Session Token (JWT)</h4>
          </div>

          <button
            onClick={handleCopyToken}
            className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs font-semibold bg-slate-800 hover:bg-slate-700 text-slate-300 transition-colors"
          >
            {tokenCopied ? (
              <>
                <Check className="w-3.5 h-3.5 text-emerald-400" />
                <span className="text-emerald-400">Copied</span>
              </>
            ) : (
              <>
                <Copy className="w-3.5 h-3.5" />
                <span>Copy Token</span>
              </>
            )}
          </button>
        </div>

        <p className="text-xs text-slate-400">
          This cryptographic token authenticates all requests to the Spring Boot REST endpoints via the{' '}
          <code className="text-brand-300 bg-slate-900 px-1 py-0.5 rounded font-mono">Authorization: Bearer</code> header.
        </p>

        <div className="p-3.5 rounded-xl bg-slate-950/80 border border-slate-800 font-mono text-[11px] text-slate-400 break-all select-all">
          {token ? token : 'No active token'}
        </div>
      </div>
    </div>
  );
};

export default Profile;
