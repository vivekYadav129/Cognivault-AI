import React, { useState, useEffect, useRef } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { documentService } from '../services/documentService';
import { StatusBadge } from '../components/common/Badge';
import LoadingSpinner from '../components/common/LoadingSpinner';
import EmptyState from '../components/common/EmptyState';
import ErrorState from '../components/common/ErrorState';
import Modal from '../components/common/Modal';
import {
  UploadCloud,
  FileText,
  Search,
  Filter,
  Trash2,
  ExternalLink,
  MessageSquare,
  HelpCircle,
  RefreshCw,
  AlertTriangle,
  Layers,
  FileCheck2,
  Clock
} from 'lucide-react';
import toast from 'react-hot-toast';

export const Documents = () => {
  const navigate = useNavigate();
  const fileInputRef = useRef(null);

  const [documents, setDocuments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Search and filter state
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');

  // Uploading state
  const [isDragging, setIsDragging] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);

  // Delete modal state
  const [deleteModalOpen, setDeleteModalOpen] = useState(false);
  const [documentToDelete, setDocumentToDelete] = useState(null);
  const [deleting, setDeleting] = useState(false);

  const fetchDocuments = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await documentService.getDocuments();
      setDocuments(data.documents || []);
    } catch (err) {
      console.error('Error fetching documents', err);
      setError('Failed to fetch documents. Please verify that the backend is running.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDocuments();
  }, []);

  const handleFileUpload = async (file) => {
    if (!file) return;

    // Check file extension
    const allowedExtensions = ['.pdf', '.txt', '.docx'];
    const name = file.name.toLowerCase();
    const isValid = allowedExtensions.some((ext) => name.endsWith(ext));

    if (!isValid) {
      toast.error('Unsupported file format. Please upload PDF, TXT, or DOCX files.');
      return;
    }

    if (file.size > 10 * 1024 * 1024) {
      toast.error('File size exceeds the 10MB limit.');
      return;
    }

    setUploading(true);
    setUploadProgress(0);
    const toastId = toast.loading(`Uploading & indexing ${file.name}...`);

    try {
      const response = await documentService.uploadDocument(file, (progress) => {
        setUploadProgress(progress);
      });

      toast.success(
        `Successfully indexed "${response.originalFileName || file.name}" into ${response.chunkCount || 0} vector chunks!`,
        { id: toastId }
      );
      // Refresh list
      fetchDocuments();
    } catch (err) {
      console.error('Upload failed', err);
      const msg = err.response?.data?.message || 'Failed to upload and process document.';
      toast.error(msg, { id: toastId });
    } finally {
      setUploading(false);
      setUploadProgress(0);
      if (fileInputRef.current) fileInputRef.current.value = '';
    }
  };

  const handleDragOver = (e) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = () => {
    setIsDragging(false);
  };

  const handleDrop = (e) => {
    e.preventDefault();
    setIsDragging(false);
    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      handleFileUpload(e.dataTransfer.files[0]);
    }
  };

  const confirmDelete = (doc) => {
    setDocumentToDelete(doc);
    setDeleteModalOpen(true);
  };

  const handleDelete = async () => {
    if (!documentToDelete) return;
    setDeleting(true);
    try {
      await documentService.deleteDocument(documentToDelete.id);
      toast.success(`Deleted "${documentToDelete.originalFileName || documentToDelete.fileName}"`);
      setDocuments((prev) => prev.filter((d) => d.id !== documentToDelete.id));
      setDeleteModalOpen(false);
    } catch (err) {
      console.error('Delete error', err);
      toast.error('Failed to delete document.');
    } finally {
      setDeleting(false);
      setDocumentToDelete(null);
    }
  };

  // Filtered documents
  const filteredDocuments = documents.filter((doc) => {
    const matchesSearch =
      (doc.originalFileName || doc.fileName || '').toLowerCase().includes(searchQuery.toLowerCase());
    const matchesStatus = statusFilter === 'ALL' || doc.status === statusFilter;
    return matchesSearch && matchesStatus;
  });

  return (
    <div className="space-y-8 animate-fade-in">
      {/* Header and Controls */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-white tracking-tight">Knowledge Documents</h2>
          <p className="text-xs sm:text-sm text-slate-400">
            Upload PDFs, manage text chunks, and ground your AI knowledge assistant
          </p>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={fetchDocuments}
            disabled={loading}
            className="inline-flex items-center gap-1.5 px-3 py-2 rounded-xl text-xs font-semibold bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 transition-colors"
            title="Refresh document list"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
            <span>Refresh</span>
          </button>

          <button
            onClick={() => fileInputRef.current?.click()}
            disabled={uploading}
            className="inline-flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-semibold bg-brand-600 hover:bg-brand-500 text-white shadow-lg shadow-brand-500/25 transition-all"
          >
            <UploadCloud className="w-4 h-4" />
            <span>Upload New Document</span>
          </button>
        </div>
      </div>

      {/* Hidden file input */}
      <input
        type="file"
        ref={fileInputRef}
        onChange={(e) => {
          if (e.target.files && e.target.files[0]) {
            handleFileUpload(e.target.files[0]);
          }
        }}
        accept=".pdf,.txt,.docx"
        className="hidden"
      />

      {/* Drag & Drop Upload Hero Area */}
      <div
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
        onClick={() => !uploading && fileInputRef.current?.click()}
        className={`relative border-2 border-dashed rounded-3xl p-8 text-center cursor-pointer transition-all ${
          isDragging
            ? 'border-brand-400 bg-brand-500/10 scale-[1.01]'
            : 'border-slate-800 hover:border-slate-700 bg-slate-900/40 hover:bg-slate-900/60'
        }`}
      >
        <div className="flex flex-col items-center justify-center max-w-md mx-auto">
          <div className="w-14 h-14 rounded-2xl bg-brand-500/10 border border-brand-500/20 flex items-center justify-center text-brand-400 mb-3 shadow-inner">
            <UploadCloud className="w-7 h-7" />
          </div>

          <p className="text-sm font-semibold text-white mb-1">
            {uploading ? 'Uploading and indexing document...' : 'Click to upload or drag & drop documents'}
          </p>
          <p className="text-xs text-slate-400 mb-3">
            Supports PDF, DOCX, and TXT files up to 10MB
          </p>

          {uploading ? (
            <div className="w-full max-w-xs space-y-2">
              <div className="w-full bg-slate-800 rounded-full h-2 overflow-hidden">
                <div
                  className="bg-brand-500 h-full transition-all duration-300"
                  style={{ width: `${uploadProgress}%` }}
                />
              </div>
              <p className="text-[11px] font-mono text-brand-400">{uploadProgress}% complete</p>
            </div>
          ) : (
            <div className="flex items-center gap-2 text-[11px] text-slate-500">
              <span className="px-2 py-0.5 rounded bg-slate-800">Chunked: 1000 chars</span>
              <span>•</span>
              <span className="px-2 py-0.5 rounded bg-slate-800">Overlap: 150 chars</span>
              <span>•</span>
              <span className="px-2 py-0.5 rounded bg-slate-800">Vectorized: 1536d</span>
            </div>
          )}
        </div>
      </div>

      {/* Search and Filters Bar */}
      <div className="glass-card p-4 rounded-2xl border border-slate-800 flex flex-col sm:flex-row items-center justify-between gap-4">
        <div className="relative w-full sm:w-80">
          <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400">
            <Search className="w-4 h-4" />
          </div>
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search by file name..."
            className="w-full pl-9 pr-3 py-2 bg-slate-900 border border-slate-700/80 rounded-xl text-xs text-white placeholder-slate-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
          />
        </div>

        <div className="flex items-center gap-2 w-full sm:w-auto">
          <Filter className="w-3.5 h-3.5 text-slate-400" />
          <span className="text-xs text-slate-400">Status:</span>
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="bg-slate-900 border border-slate-700 text-xs text-slate-200 rounded-xl px-2.5 py-1.5 focus:outline-none focus:ring-1 focus:ring-brand-500"
          >
            <option value="ALL">All Statuses ({documents.length})</option>
            <option value="COMPLETED">Ready / Indexed</option>
            <option value="PROCESSING">Processing</option>
            <option value="FAILED">Failed</option>
          </select>
        </div>
      </div>

      {/* Documents Grid / List */}
      {loading ? (
        <div className="py-20 flex justify-center">
          <LoadingSpinner text="Loading documents..." size="lg" />
        </div>
      ) : error ? (
        <ErrorState message={error} onRetry={fetchDocuments} />
      ) : filteredDocuments.length === 0 ? (
        <EmptyState
          title={searchQuery ? 'No documents match your search' : 'No documents found'}
          description={
            searchQuery
              ? `No document names matched "${searchQuery}". Try a different keyword.`
              : 'Upload your first document above to enable AI question answering and knowledge testing.'
          }
          actionLabel={searchQuery ? 'Clear Search' : 'Upload Document'}
          onAction={searchQuery ? () => setSearchQuery('') : () => fileInputRef.current?.click()}
        />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {filteredDocuments.map((doc) => (
            <div
              key={doc.id}
              className="glass-card glass-card-hover rounded-2xl border border-slate-800/80 p-5 flex flex-col justify-between group"
            >
              <div>
                {/* Header: File icon + Status */}
                <div className="flex items-start justify-between gap-3 mb-3">
                  <div className="w-10 h-10 rounded-xl bg-brand-500/10 border border-brand-500/20 flex items-center justify-center text-brand-400 flex-shrink-0 group-hover:scale-105 transition-transform">
                    <FileText className="w-5 h-5" />
                  </div>
                  <StatusBadge status={doc.status} />
                </div>

                {/* File title */}
                <Link
                  to={`/documents/${doc.id}`}
                  className="font-bold text-sm text-white hover:text-brand-300 transition-colors line-clamp-1 block mb-1"
                  title={doc.originalFileName || doc.fileName}
                >
                  {doc.originalFileName || doc.fileName}
                </Link>
                <p className="text-[11px] text-slate-500 font-mono">
                  ID: #{doc.id} • {doc.fileType || 'DOCUMENT'}
                </p>

                {/* Metadata tags */}
                <div className="mt-4 grid grid-cols-3 gap-2 text-center py-2.5 px-3 rounded-xl bg-slate-950/60 border border-slate-800/80 text-xs">
                  <div>
                    <span className="block text-slate-400 text-[10px] uppercase font-semibold">Chunks</span>
                    <span className="font-bold text-white">{doc.chunkCount || 0}</span>
                  </div>
                  <div className="border-x border-slate-800">
                    <span className="block text-slate-400 text-[10px] uppercase font-semibold">Pages</span>
                    <span className="font-bold text-white">{doc.pageCount || 1}</span>
                  </div>
                  <div>
                    <span className="block text-slate-400 text-[10px] uppercase font-semibold">Size</span>
                    <span className="font-bold text-white">
                      {((doc.fileSize || 0) / 1024).toFixed(0)} KB
                    </span>
                  </div>
                </div>
              </div>

              {/* Actions Footer */}
              <div className="mt-5 pt-4 border-t border-slate-800/80 flex items-center justify-between gap-2">
                <div className="flex items-center gap-1">
                  <button
                    onClick={() => navigate(`/chat?docId=${doc.id}`)}
                    className="p-1.5 rounded-lg text-slate-400 hover:text-brand-300 hover:bg-slate-800 transition-colors"
                    title="Chat with this document"
                  >
                    <MessageSquare className="w-4 h-4" />
                  </button>
                  <button
                    onClick={() => navigate(`/quiz?docId=${doc.id}`)}
                    className="p-1.5 rounded-lg text-slate-400 hover:text-indigo-300 hover:bg-slate-800 transition-colors"
                    title="Generate Quiz on this document"
                  >
                    <HelpCircle className="w-4 h-4" />
                  </button>
                  <button
                    onClick={() => confirmDelete(doc)}
                    className="p-1.5 rounded-lg text-slate-400 hover:text-rose-400 hover:bg-rose-500/10 transition-colors"
                    title="Delete document"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>

                <Link
                  to={`/documents/${doc.id}`}
                  className="inline-flex items-center gap-1 text-xs font-semibold text-brand-400 hover:text-brand-300 transition-colors"
                >
                  <span>Inspect Chunks</span>
                  <ExternalLink className="w-3.5 h-3.5" />
                </Link>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Delete Confirmation Modal */}
      <Modal
        isOpen={deleteModalOpen}
        onClose={() => setDeleteModalOpen(false)}
        title="Delete Document"
      >
        <div className="space-y-4">
          <div className="flex items-start gap-3 p-3 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-300 text-xs">
            <AlertTriangle className="w-5 h-5 text-rose-400 flex-shrink-0 mt-0.5" />
            <div>
              <p className="font-semibold text-rose-200">This action cannot be undone.</p>
              <p className="mt-0.5 text-rose-300/80">
                Deleting this document will remove its raw file and all corresponding vector embeddings and chunks from pgvector.
              </p>
            </div>
          </div>

          <p className="text-sm text-slate-300">
            Are you sure you want to permanently delete{' '}
            <span className="font-semibold text-white">
              "{documentToDelete?.originalFileName || documentToDelete?.fileName}"
            </span>
            ?
          </p>

          <div className="flex justify-end gap-3 pt-3">
            <button
              type="button"
              onClick={() => setDeleteModalOpen(false)}
              className="px-4 py-2 rounded-xl text-xs font-semibold bg-slate-800 hover:bg-slate-700 text-slate-300 transition-colors"
            >
              Cancel
            </button>
            <button
              type="button"
              onClick={handleDelete}
              disabled={deleting}
              className="px-4 py-2 rounded-xl text-xs font-semibold bg-rose-600 hover:bg-rose-500 text-white transition-colors flex items-center gap-2"
            >
              {deleting ? (
                <>
                  <span className="w-3.5 h-3.5 rounded-full border-2 border-white/20 border-t-white animate-spin"></span>
                  <span>Deleting...</span>
                </>
              ) : (
                <>
                  <Trash2 className="w-3.5 h-3.5" />
                  <span>Confirm Delete</span>
                </>
              )}
            </button>
          </div>
        </div>
      </Modal>
    </div>
  );
};

export default Documents;
