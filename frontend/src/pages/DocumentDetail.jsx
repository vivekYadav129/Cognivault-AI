import React, { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { documentService } from '../services/documentService';
import { StatusBadge } from '../components/common/Badge';
import LoadingSpinner from '../components/common/LoadingSpinner';
import ErrorState from '../components/common/ErrorState';
import Modal from '../components/common/Modal';
import {
  ArrowLeft,
  Layers,
  MessageSquare,
  HelpCircle,
  Trash2,
  Copy,
  Check,
  Search,
  Calendar,
  HardDrive,
  FileSpreadsheet,
  AlertTriangle
} from 'lucide-react';
import toast from 'react-hot-toast';

export const DocumentDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();

  const [document, setDocument] = useState(null);
  const [chunks, setChunks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Chunk search filter
  const [chunkSearch, setChunkSearch] = useState('');
  const [copiedChunkId, setCopiedChunkId] = useState(null);

  // Delete modal
  const [deleteModalOpen, setDeleteModalOpen] = useState(false);
  const [deleting, setDeleting] = useState(false);

  const fetchDocumentAndChunks = async () => {
    setLoading(true);
    setError(null);
    try {
      const [docData, chunksData] = await Promise.all([
        documentService.getDocumentById(id),
        documentService.getDocumentChunks(id),
      ]);
      setDocument(docData);
      setChunks(chunksData || []);
    } catch (err) {
      console.error('Error fetching document details', err);
      const msg = err.response?.data?.message || 'Could not load document details or chunks.';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (id) {
      fetchDocumentAndChunks();
    }
  }, [id]);

  const handleCopyChunk = (chunk) => {
    navigator.clipboard.writeText(chunk.content);
    setCopiedChunkId(chunk.id);
    toast.success(`Copied Chunk #${chunk.chunkIndex} content to clipboard`);
    setTimeout(() => setCopiedChunkId(null), 2000);
  };

  const handleDelete = async () => {
    setDeleting(true);
    try {
      await documentService.deleteDocument(id);
      toast.success('Document deleted successfully');
      navigate('/documents');
    } catch (err) {
      console.error('Delete error', err);
      toast.error('Failed to delete document.');
      setDeleting(false);
    }
  };

  const filteredChunks = chunks.filter((chunk) =>
    (chunk.content || '').toLowerCase().includes(chunkSearch.toLowerCase())
  );

  if (loading) {
    return (
      <div className="py-20 flex justify-center">
        <LoadingSpinner text="Retrieving document metadata and vector chunks..." size="lg" />
      </div>
    );
  }

  if (error || !document) {
    return (
      <div className="space-y-4">
        <Link
          to="/documents"
          className="inline-flex items-center gap-2 text-xs font-semibold text-slate-400 hover:text-white transition-colors"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Back to Documents</span>
        </Link>
        <ErrorState
          title="Document Not Found"
          message={error || 'The requested document could not be found or you do not have permission to view it.'}
          onRetry={fetchDocumentAndChunks}
        />
      </div>
    );
  }

  return (
    <div className="space-y-8 animate-fade-in">
      {/* Top Breadcrumb and Actions */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="space-y-1">
          <Link
            to="/documents"
            className="inline-flex items-center gap-1.5 text-xs font-medium text-slate-400 hover:text-white transition-colors mb-1"
          >
            <ArrowLeft className="w-3.5 h-3.5" />
            <span>Documents</span>
            <span>/</span>
            <span className="text-slate-300">#{document.id}</span>
          </Link>
          <div className="flex items-center gap-3">
            <h2 className="text-xl sm:text-2xl font-bold text-white tracking-tight">
              {document.originalFileName || document.fileName}
            </h2>
            <StatusBadge status={document.status} />
          </div>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={() => navigate(`/chat?docId=${document.id}`)}
            className="inline-flex items-center gap-2 px-3.5 py-2 rounded-xl text-xs font-semibold bg-brand-600 hover:bg-brand-500 text-white shadow-md shadow-brand-500/20 transition-all"
          >
            <MessageSquare className="w-3.5 h-3.5" />
            <span>Chat With Document</span>
          </button>

          <button
            onClick={() => navigate(`/quiz?docId=${document.id}`)}
            className="inline-flex items-center gap-2 px-3.5 py-2 rounded-xl text-xs font-semibold bg-indigo-600 hover:bg-indigo-500 text-white shadow-md shadow-indigo-500/20 transition-all"
          >
            <HelpCircle className="w-3.5 h-3.5" />
            <span>Generate Quiz</span>
          </button>

          <button
            onClick={() => setDeleteModalOpen(true)}
            className="p-2 rounded-xl text-slate-400 hover:text-rose-400 hover:bg-rose-500/10 border border-slate-700/80 transition-colors"
            title="Delete Document"
          >
            <Trash2 className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* Metadata Overview Cards */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        <div className="glass-card p-4 rounded-2xl border border-slate-800">
          <div className="flex items-center gap-2 text-slate-400 text-xs font-semibold mb-1">
            <HardDrive className="w-3.5 h-3.5 text-brand-400" />
            <span>File Size</span>
          </div>
          <p className="text-lg font-bold text-white">
            {((document.fileSize || 0) / 1024).toFixed(1)} KB
          </p>
          <p className="text-[11px] text-slate-500 font-mono mt-0.5">{document.fileType || 'binary'}</p>
        </div>

        <div className="glass-card p-4 rounded-2xl border border-slate-800">
          <div className="flex items-center gap-2 text-slate-400 text-xs font-semibold mb-1">
            <Layers className="w-3.5 h-3.5 text-indigo-400" />
            <span>Vector Chunks</span>
          </div>
          <p className="text-lg font-bold text-white">{chunks.length}</p>
          <p className="text-[11px] text-slate-500 mt-0.5">Segmented & embedded</p>
        </div>

        <div className="glass-card p-4 rounded-2xl border border-slate-800">
          <div className="flex items-center gap-2 text-slate-400 text-xs font-semibold mb-1">
            <FileSpreadsheet className="w-3.5 h-3.5 text-purple-400" />
            <span>Page Count</span>
          </div>
          <p className="text-lg font-bold text-white">{document.pageCount || 1}</p>
          <p className="text-[11px] text-slate-500 mt-0.5">Pages extracted</p>
        </div>

        <div className="glass-card p-4 rounded-2xl border border-slate-800">
          <div className="flex items-center gap-2 text-slate-400 text-xs font-semibold mb-1">
            <Calendar className="w-3.5 h-3.5 text-emerald-400" />
            <span>Uploaded At</span>
          </div>
          <p className="text-sm font-bold text-white truncate">
            {document.createdAt ? new Date(document.createdAt).toLocaleDateString() : 'Recent'}
          </p>
          <p className="text-[11px] text-slate-500 mt-0.5 font-mono">
            {document.createdAt ? new Date(document.createdAt).toLocaleTimeString() : ''}
          </p>
        </div>
      </div>

      {/* Vector Chunks Section */}
      <div className="space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div>
            <h3 className="text-lg font-bold text-white tracking-tight">Indexed Knowledge Chunks</h3>
            <p className="text-xs text-slate-400">
              Inspect the exact context chunks used by the RAG model to ground responses and citations
            </p>
          </div>

          <div className="relative w-full sm:w-72">
            <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400">
              <Search className="w-3.5 h-3.5" />
            </div>
            <input
              type="text"
              value={chunkSearch}
              onChange={(e) => setChunkSearch(e.target.value)}
              placeholder="Search in chunks..."
              className="w-full pl-8 pr-3 py-1.5 bg-slate-900 border border-slate-700 rounded-xl text-xs text-white placeholder-slate-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
            />
          </div>
        </div>

        {chunks.length === 0 ? (
          <div className="glass-card p-8 rounded-2xl border border-slate-800 text-center">
            <Layers className="w-8 h-8 text-slate-500 mx-auto mb-2" />
            <p className="text-sm font-semibold text-white">No chunks available yet</p>
            <p className="text-xs text-slate-400 mt-1">
              Document might still be processing embeddings or had no text content.
            </p>
          </div>
        ) : filteredChunks.length === 0 ? (
          <p className="text-xs text-slate-500 italic p-4 text-center">
            No chunks matched "{chunkSearch}".
          </p>
        ) : (
          <div className="space-y-3">
            {filteredChunks.map((chunk) => (
              <div
                key={chunk.id}
                className="glass-card rounded-2xl border border-slate-800/80 p-4 space-y-2 hover:border-slate-700 transition-colors"
              >
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <span className="px-2 py-0.5 rounded-md bg-brand-500/10 text-brand-400 font-mono text-xs font-semibold border border-brand-500/20">
                      Chunk #{chunk.chunkIndex}
                    </span>
                    <span className="text-[11px] text-slate-400">
                      {chunk.characterCount || chunk.content?.length || 0} characters
                    </span>
                  </div>

                  <button
                    onClick={() => handleCopyChunk(chunk)}
                    className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs font-medium text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
                  >
                    {copiedChunkId === chunk.id ? (
                      <>
                        <Check className="w-3.5 h-3.5 text-emerald-400" />
                        <span className="text-emerald-400">Copied</span>
                      </>
                    ) : (
                      <>
                        <Copy className="w-3.5 h-3.5" />
                        <span>Copy</span>
                      </>
                    )}
                  </button>
                </div>

                <div className="p-3 rounded-xl bg-slate-950/80 border border-slate-800/60 font-mono text-xs text-slate-300 leading-relaxed whitespace-pre-wrap selection:bg-brand-500/30">
                  {chunk.content}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

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
              <p className="font-semibold text-rose-200">Irreversible Action</p>
              <p className="mt-0.5 text-rose-300/80">
                Are you sure you want to delete this document and all {chunks.length} pgvector chunks?
              </p>
            </div>
          </div>

          <div className="flex justify-end gap-3 pt-3">
            <button
              type="button"
              onClick={() => setDeleteModalOpen(false)}
              className="px-4 py-2 rounded-xl text-xs font-semibold bg-slate-800 hover:bg-slate-700 text-slate-300"
            >
              Cancel
            </button>
            <button
              type="button"
              onClick={handleDelete}
              disabled={deleting}
              className="px-4 py-2 rounded-xl text-xs font-semibold bg-rose-600 hover:bg-rose-500 text-white flex items-center gap-2"
            >
              {deleting ? 'Deleting...' : 'Delete Permanently'}
            </button>
          </div>
        </div>
      </Modal>
    </div>
  );
};

export default DocumentDetail;
