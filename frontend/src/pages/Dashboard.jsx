import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { documentService } from '../services/documentService';
import { ragService } from '../services/ragService';
import { StatusBadge, ScoreBadge } from '../components/common/Badge';
import LoadingSpinner from '../components/common/LoadingSpinner';
import EmptyState from '../components/common/EmptyState';
import ErrorState from '../components/common/ErrorState';
import {
  Files,
  Layers,
  Sparkles,
  Search,
  ArrowRight,
  Upload,
  Brain,
  MessageSquare,
  HelpCircle,
  Clock,
  ShieldCheck
} from 'lucide-react';
import toast from 'react-hot-toast';

export const Dashboard = () => {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [documents, setDocuments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Quick Semantic Search State
  const [quickQuery, setQuickQuery] = useState('');
  const [searching, setSearching] = useState(false);
  const [searchResults, setSearchResults] = useState(null);

  // Direct RAG question shortcut
  const [chatPrompt, setChatPrompt] = useState('');

  const fetchDashboardData = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await documentService.getDocuments();
      setDocuments(data.documents || []);
    } catch (err) {
      console.error('Failed to load documents', err);
      setError('Could not load knowledge assistant statistics. Ensure the Spring Boot backend is active.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDashboardData();
  }, []);

  const totalChunks = documents.reduce((sum, doc) => sum + (doc.chunkCount || 0), 0);
  const totalPages = documents.reduce((sum, doc) => sum + (doc.pageCount || 0), 0);
  const readyDocs = documents.filter((doc) => doc.status === 'COMPLETED').length;

  const handleQuickSearch = async (e) => {
    e.preventDefault();
    if (!quickQuery.trim()) return;

    setSearching(true);
    try {
      const res = await ragService.search({ query: quickQuery.trim(), topK: 3 });
      setSearchResults(res.results || []);
      if (!res.results || res.results.length === 0) {
        toast('No matching vector chunks found with current threshold.', { icon: '🔍' });
      }
    } catch (err) {
      console.error('Search error', err);
      toast.error('Vector search failed. Check backend connection.');
    } finally {
      setSearching(false);
    }
  };

  const handleLaunchChat = (e) => {
    e.preventDefault();
    if (chatPrompt.trim()) {
      navigate(`/chat?q=${encodeURIComponent(chatPrompt.trim())}`);
    } else {
      navigate('/chat');
    }
  };

  if (loading) {
    return (
      <div className="py-20 flex justify-center">
        <LoadingSpinner text="Loading dashboard knowledge..." size="lg" />
      </div>
    );
  }

  if (error) {
    return <ErrorState message={error} onRetry={fetchDashboardData} />;
  }

  return (
    <div className="space-y-8 animate-fade-in">
      {/* Welcome & Quick RAG Bar Hero */}
      <div className="relative overflow-hidden rounded-3xl glass-card border border-brand-500/20 p-6 sm:p-8 bg-gradient-to-br from-brand-950/40 via-slate-900/60 to-slate-950/80">
        <div className="relative z-10 flex flex-col md:flex-row items-start md:items-center justify-between gap-6">
          <div>
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-brand-500/10 border border-brand-500/30 text-brand-300 text-xs font-semibold mb-3">
              <Sparkles className="w-3.5 h-3.5" />
              <span>Cognivault RAG Engine Active</span>
            </div>
            <h2 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">
              Welcome back, {user?.name || 'Knowledge Engineer'}
            </h2>
            <p className="text-slate-400 text-sm mt-1 max-w-xl">
              Your personal AI Knowledge Assistant is ready. Search your vector database, chat with grounding citations, or generate quizzes.
            </p>
          </div>

          <div className="flex items-center gap-3">
            <Link
              to="/documents"
              className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl text-sm font-semibold bg-brand-600 hover:bg-brand-500 text-white shadow-lg shadow-brand-500/25 transition-all"
            >
              <Upload className="w-4 h-4" />
              <span>Upload Document</span>
            </Link>
            <Link
              to="/chat"
              className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl text-sm font-semibold bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 transition-all"
            >
              <MessageSquare className="w-4 h-4 text-brand-400" />
              <span>Open Chat</span>
            </Link>
          </div>
        </div>

        {/* Ask Question Bar */}
        <div className="mt-6 pt-6 border-t border-slate-800/80">
          <form onSubmit={handleLaunchChat} className="flex gap-2">
            <div className="relative flex-1">
              <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
                <Brain className="h-4 w-4 text-brand-400" />
              </div>
              <input
                type="text"
                value={chatPrompt}
                onChange={(e) => setChatPrompt(e.target.value)}
                placeholder="Ask anything grounded across your documents (e.g. 'Summarize the architecture' or 'What are the main findings?')"
                className="w-full pl-10 pr-4 py-3 bg-slate-900/90 border border-slate-700/80 rounded-2xl text-sm text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-brand-500"
              />
            </div>
            <button
              type="submit"
              className="px-5 py-3 rounded-2xl bg-brand-600 hover:bg-brand-500 text-white text-sm font-semibold flex items-center gap-2 shadow-md shadow-brand-500/20 transition-all whitespace-nowrap"
            >
              <span>Ask RAG</span>
              <ArrowRight className="w-4 h-4" />
            </button>
          </form>
        </div>
      </div>

      {/* Metrics Row */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        <div className="glass-card glass-card-hover p-5 rounded-2xl border border-slate-800">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">
              Total Documents
            </span>
            <div className="w-10 h-10 rounded-xl bg-blue-500/10 border border-blue-500/20 flex items-center justify-center text-blue-400">
              <Files className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-4 flex items-baseline gap-2">
            <span className="text-3xl font-extrabold text-white">{documents.length}</span>
            <span className="text-xs text-emerald-400 font-medium">
              {readyDocs} ready
            </span>
          </div>
          <p className="text-xs text-slate-500 mt-1">PDF, DOCX, TXT files</p>
        </div>

        <div className="glass-card glass-card-hover p-5 rounded-2xl border border-slate-800">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">
              Indexed Chunks
            </span>
            <div className="w-10 h-10 rounded-xl bg-indigo-500/10 border border-indigo-500/20 flex items-center justify-center text-indigo-400">
              <Layers className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-4 flex items-baseline gap-2">
            <span className="text-3xl font-extrabold text-white">{totalChunks}</span>
            <span className="text-xs text-slate-400 font-medium">in pgvector</span>
          </div>
          <p className="text-xs text-slate-500 mt-1">1536-dim vector embeddings</p>
        </div>

        <div className="glass-card glass-card-hover p-5 rounded-2xl border border-slate-800">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">
              Total Pages
            </span>
            <div className="w-10 h-10 rounded-xl bg-purple-500/10 border border-purple-500/20 flex items-center justify-center text-purple-400">
              <Clock className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-4 flex items-baseline gap-2">
            <span className="text-3xl font-extrabold text-white">{totalPages}</span>
            <span className="text-xs text-slate-400 font-medium">extracted</span>
          </div>
          <p className="text-xs text-slate-500 mt-1">Parsed and segmented</p>
        </div>

        <div className="glass-card glass-card-hover p-5 rounded-2xl border border-slate-800">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">
              Grounding Shield
            </span>
            <div className="w-10 h-10 rounded-xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400">
              <ShieldCheck className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-4 flex items-baseline gap-2">
            <span className="text-3xl font-extrabold text-emerald-400">100%</span>
            <span className="text-xs text-emerald-300 font-medium">Strict RAG</span>
          </div>
          <p className="text-xs text-slate-500 mt-1">Hallucination & prompt guard</p>
        </div>
      </div>

      {/* Main 2-column section: Recent Documents & Vector Search Test */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Left Column: Recent Documents (2 cols) */}
        <div className="lg:col-span-2 space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="text-lg font-bold text-white tracking-tight">Recent Documents</h3>
              <p className="text-xs text-slate-400">Manage and inspect uploaded knowledge sources</p>
            </div>
            <Link
              to="/documents"
              className="text-xs font-semibold text-brand-400 hover:text-brand-300 flex items-center gap-1 transition-colors"
            >
              <span>View all ({documents.length})</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </Link>
          </div>

          {documents.length === 0 ? (
            <EmptyState
              title="No documents uploaded yet"
              description="Upload your first PDF or document to generate embeddings and ask questions with citations."
              actionLabel="Upload First Document"
              onAction={() => navigate('/documents')}
            />
          ) : (
            <div className="glass-card rounded-2xl border border-slate-800/80 overflow-hidden">
              <div className="divide-y divide-slate-800/60">
                {documents.slice(0, 5).map((doc) => (
                  <div
                    key={doc.id}
                    className="p-4 sm:p-5 flex items-center justify-between gap-4 hover:bg-slate-850/50 transition-colors group"
                  >
                    <div className="flex items-center gap-3.5 min-w-0">
                      <div className="w-10 h-10 rounded-xl bg-slate-800 border border-slate-700/80 flex items-center justify-center text-brand-400 flex-shrink-0 group-hover:border-brand-500/50 transition-colors">
                        <Files className="w-5 h-5" />
                      </div>
                      <div className="min-w-0">
                        <Link
                          to={`/documents/${doc.id}`}
                          className="text-sm font-semibold text-white hover:text-brand-300 transition-colors truncate block"
                        >
                          {doc.originalFileName || doc.fileName}
                        </Link>
                        <div className="flex items-center gap-3 text-xs text-slate-400 mt-0.5">
                          <span>{doc.chunkCount || 0} chunks</span>
                          <span>•</span>
                          <span>{doc.pageCount || 1} pages</span>
                          <span>•</span>
                          <span>{((doc.fileSize || 0) / 1024).toFixed(1)} KB</span>
                        </div>
                      </div>
                    </div>

                    <div className="flex items-center gap-3">
                      <StatusBadge status={doc.status} />
                      <Link
                        to={`/documents/${doc.id}`}
                        className="p-2 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
                        title="View Document Details & Chunks"
                      >
                        <ArrowRight className="w-4 h-4" />
                      </Link>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* Right Column: Semantic Vector Search Preview */}
        <div className="space-y-4">
          <div>
            <h3 className="text-lg font-bold text-white tracking-tight">Semantic Vector Search</h3>
            <p className="text-xs text-slate-400">Directly test vector cosine similarity</p>
          </div>

          <div className="glass-card p-5 rounded-2xl border border-slate-800 space-y-4">
            <form onSubmit={handleQuickSearch} className="space-y-2">
              <div className="relative">
                <input
                  type="text"
                  value={quickQuery}
                  onChange={(e) => setQuickQuery(e.target.value)}
                  placeholder="Enter semantic query..."
                  className="w-full pl-3 pr-9 py-2 bg-slate-900 border border-slate-700 rounded-xl text-xs text-white placeholder-slate-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
                />
                <button
                  type="submit"
                  disabled={searching || !quickQuery.trim()}
                  className="absolute right-1.5 top-1.5 p-1 rounded-lg text-slate-400 hover:text-white disabled:opacity-40"
                >
                  <Search className="w-4 h-4" />
                </button>
              </div>
            </form>

            {searching && (
              <div className="py-6 flex justify-center">
                <LoadingSpinner size="sm" text="Embedding query & searching vector index..." />
              </div>
            )}

            {searchResults && (
              <div className="space-y-3">
                <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">
                  Search Results ({searchResults.length})
                </p>
                {searchResults.length === 0 ? (
                  <p className="text-xs text-slate-500 italic">No matching chunks above threshold.</p>
                ) : (
                  searchResults.map((result, idx) => (
                    <div
                      key={idx}
                      className="p-3 rounded-xl bg-slate-950/70 border border-slate-800/80 space-y-1.5 text-xs"
                    >
                      <div className="flex items-center justify-between">
                        <span className="font-mono text-slate-400 text-[11px]">Chunk #{result.chunkId}</span>
                        <ScoreBadge score={result.similarityScore} />
                      </div>
                      <p className="text-slate-300 line-clamp-2 leading-relaxed">
                        {result.content}
                      </p>
                    </div>
                  ))
                )}
              </div>
            )}

            <div className="pt-2 border-t border-slate-800/60">
              <Link
                to="/quiz"
                className="w-full flex items-center justify-between p-3 rounded-xl bg-brand-600/10 hover:bg-brand-600/20 border border-brand-500/20 text-brand-300 text-xs font-semibold transition-colors"
              >
                <div className="flex items-center gap-2">
                  <HelpCircle className="w-4 h-4 text-brand-400" />
                  <span>Launch AI Document Quiz</span>
                </div>
                <ArrowRight className="w-3.5 h-3.5" />
              </Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
