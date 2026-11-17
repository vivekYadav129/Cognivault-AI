import React, { useState, useEffect, useRef } from 'react';
import { useSearchParams } from 'react-router-dom';
import { ragService } from '../services/ragService';
import { documentService } from '../services/documentService';
import { ScoreBadge } from '../components/common/Badge';
import Modal from '../components/common/Modal';
import {
  Send,
  Bot,
  User,
  FileText,
  Sliders,
  Trash2,
  Copy,
  Check,
  BookOpen
} from 'lucide-react';
import toast from 'react-hot-toast';
import ReactMarkdown from 'react-markdown';

export const Chat = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const initialDocId = searchParams.get('docId') || '';
  const initialQuery = searchParams.get('q') || '';

  const [documents, setDocuments] = useState([]);
  const [selectedDocId, setSelectedDocId] = useState(initialDocId);
  const [topK, setTopK] = useState(5);

  const [inputQuery, setInputQuery] = useState(initialQuery);
  const [messages, setMessages] = useState([
    {
      id: 'welcome',
      sender: 'assistant',
      text: "Hello! I am **Cognivault AI**, your enterprise knowledge assistant. Ask me questions, and I will generate answers strictly grounded in your uploaded documents with precise source citations.",
      citations: [],
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    },
  ]);
  const [loading, setLoading] = useState(false);
  const [copiedId, setCopiedId] = useState(null);

  // Citation details modal
  const [activeCitation, setActiveCitation] = useState(null);
  const [showConfig, setShowConfig] = useState(false);

  const messagesEndRef = useRef(null);
  const inputRef = useRef(null);

  useEffect(() => {
    const fetchDocs = async () => {
      try {
        const data = await documentService.getDocuments();
        setDocuments(data.documents || []);
      } catch (err) {
        console.error('Failed to load documents list for chat filter', err);
      }
    };
    fetchDocs();
  }, []);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, loading]);

  // Execute initial query if provided from dashboard
  useEffect(() => {
    if (initialQuery && documents.length >= 0) {
      handleSendMessage(initialQuery);
      // Clear URL params
      setSearchParams({});
    }
  }, [initialQuery]);

  const handleSendMessage = async (queryToSend) => {
    const query = typeof queryToSend === 'string' ? queryToSend : inputQuery;
    if (!query || !query.trim() || loading) return;

    const trimmed = query.trim();
    setInputQuery('');

    // Add user message to thread
    const userMessage = {
      id: Date.now().toString(),
      sender: 'user',
      text: trimmed,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    };
    setMessages((prev) => [...prev, userMessage]);
    setLoading(true);

    try {
      const response = await ragService.queryRag({
        query: trimmed,
        topK: topK,
        documentId: selectedDocId ? Number(selectedDocId) : null,
      });

      const assistantMessage = {
        id: (Date.now() + 1).toString(),
        sender: 'assistant',
        text: response.answer,
        citations: response.citations || [],
        sourcesUsed: response.sourcesUsed || 0,
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      };

      setMessages((prev) => [...prev, assistantMessage]);
    } catch (err) {
      console.error('RAG query error', err);
      const errMsg =
        err.response?.data?.message ||
        'Error retrieving grounded response. Please verify that documents are uploaded and the backend is running.';

      setMessages((prev) => [
        ...prev,
        {
          id: (Date.now() + 1).toString(),
          sender: 'assistant',
          isError: true,
          text: `⚠️ **Request Failed**: ${errMsg}`,
          citations: [],
          timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        },
      ]);
      toast.error(errMsg);
    } finally {
      setLoading(false);
      setTimeout(() => inputRef.current?.focus(), 100);
    }
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSendMessage();
    }
  };

  const handleCopy = (msgId, text) => {
    navigator.clipboard.writeText(text);
    setCopiedId(msgId);
    toast.success('Answer copied to clipboard');
    setTimeout(() => setCopiedId(null), 2000);
  };

  const clearChat = () => {
    setMessages([
      {
        id: 'welcome-reset',
        sender: 'assistant',
        text: 'Chat history cleared. How can I assist you with your knowledge base today?',
        citations: [],
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      },
    ]);
  };

  const sampleQuestions = [
    'What are the core technical features described in the documents?',
    'Summarize the primary conclusions and architecture.',
    'What security and isolation guarantees are provided?',
  ];

  return (
    <div className="flex flex-col h-[calc(100vh-7rem)] max-w-5xl mx-auto animate-fade-in">
      {/* Chat Controls Header */}
      <div className="glass-card rounded-2xl border border-slate-800 p-3.5 mb-3 flex flex-wrap items-center justify-between gap-3 flex-shrink-0">
        <div className="flex items-center gap-2">
          <div className="w-8 h-8 rounded-xl bg-brand-500/10 border border-brand-500/20 flex items-center justify-center text-brand-400">
            <Bot className="w-4 h-4" />
          </div>
          <div>
            <h2 className="text-sm font-bold text-white flex items-center gap-1.5">
              <span>AI Grounded Assistant</span>
              <span className="text-[10px] font-semibold px-1.5 py-0.5 rounded bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                pgvector RAG
              </span>
            </h2>
            <p className="text-[11px] text-slate-400">
              {selectedDocId
                ? `Scoped to: ${documents.find((d) => String(d.id) === String(selectedDocId))?.originalFileName || 'Specific Document'}`
                : 'Searching across all uploaded documents'}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          {/* Document Scope Selector */}
          <div className="flex items-center gap-1.5 bg-slate-900 border border-slate-700/80 rounded-xl px-2.5 py-1.5 text-xs">
            <FileText className="w-3.5 h-3.5 text-brand-400" />
            <select
              value={selectedDocId}
              onChange={(e) => setSelectedDocId(e.target.value)}
              className="bg-transparent text-slate-200 focus:outline-none cursor-pointer max-w-[150px] sm:max-w-[200px] truncate"
            >
              <option value="" className="bg-slate-900 text-slate-200">
                All Documents ({documents.length})
              </option>
              {documents.map((doc) => (
                <option key={doc.id} value={doc.id} className="bg-slate-900 text-slate-200">
                  {doc.originalFileName || doc.fileName}
                </option>
              ))}
            </select>
          </div>

          {/* Config toggle */}
          <button
            onClick={() => setShowConfig(!showConfig)}
            className={`p-2 rounded-xl border text-xs font-semibold flex items-center gap-1.5 transition-colors ${
              showConfig
                ? 'bg-brand-600/20 text-brand-300 border-brand-500/40'
                : 'bg-slate-900 text-slate-400 hover:text-white border-slate-700/80'
            }`}
            title="Retrieval Settings (Top-K)"
          >
            <Sliders className="w-3.5 h-3.5" />
            <span className="hidden sm:inline">Top-K: {topK}</span>
          </button>

          {/* Clear button */}
          <button
            onClick={clearChat}
            className="p-2 rounded-xl bg-slate-900 text-slate-400 hover:text-rose-400 hover:bg-rose-500/10 border border-slate-700/80 transition-colors"
            title="Clear Chat Conversation"
          >
            <Trash2 className="w-3.5 h-3.5" />
          </button>
        </div>
      </div>

      {/* Expanded Config Slider Drawer */}
      {showConfig && (
        <div className="glass-card rounded-2xl border border-slate-800 p-4 mb-3 animate-slide-up flex flex-col sm:flex-row items-center justify-between gap-4 text-xs">
          <div className="space-y-0.5">
            <span className="font-semibold text-white">Top-K Vector Context Chunks</span>
            <p className="text-slate-400">
              Controls how many matching chunks from pgvector are fed into the LLM context prompt.
            </p>
          </div>
          <div className="flex items-center gap-3 w-full sm:w-60">
            <input
              type="range"
              min="1"
              max="15"
              value={topK}
              onChange={(e) => setTopK(Number(e.target.value))}
              className="w-full accent-brand-500 cursor-pointer"
            />
            <span className="font-mono font-bold text-brand-400 bg-slate-900 px-2.5 py-1 rounded-lg border border-slate-700">
              {topK}
            </span>
          </div>
        </div>
      )}

      {/* Message History Feed */}
      <div className="flex-1 overflow-y-auto space-y-4 px-1 py-3 pr-2">
        {messages.map((msg) => {
          const isUser = msg.sender === 'user';
          return (
            <div
              key={msg.id}
              className={`flex gap-3 sm:gap-4 ${isUser ? 'justify-end' : 'justify-start'}`}
            >
              {/* Assistant Avatar */}
              {!isUser && (
                <div className="w-8 h-8 rounded-xl bg-gradient-to-tr from-brand-600 to-indigo-600 flex items-center justify-center text-white flex-shrink-0 shadow-md shadow-brand-500/20 mt-1">
                  <Bot className="w-4 h-4" />
                </div>
              )}

              {/* Message Bubble */}
              <div
                className={`max-w-[85%] sm:max-w-[78%] rounded-2xl p-4 sm:p-5 ${
                  isUser
                    ? 'bg-brand-600 text-white rounded-br-none shadow-lg shadow-brand-600/20'
                    : msg.isError
                    ? 'bg-rose-500/10 border border-rose-500/30 text-rose-200 rounded-bl-none'
                    : 'glass-card border border-slate-800 text-slate-100 rounded-bl-none shadow-xl'
                }`}
              >
                {/* Header row */}
                <div className="flex items-center justify-between gap-3 mb-2 text-xs opacity-75">
                  <span className="font-semibold">{isUser ? 'You' : 'Cognivault AI'}</span>
                  <span>{msg.timestamp}</span>
                </div>

                {/* Body markdown */}
                <div className="prose prose-invert prose-sm max-w-none text-xs sm:text-sm leading-relaxed break-words">
                  <ReactMarkdown>{msg.text}</ReactMarkdown>
                </div>

                {/* Citations Footer for Assistant */}
                {!isUser && msg.citations && msg.citations.length > 0 && (
                  <div className="mt-4 pt-3 border-t border-slate-800/80 space-y-2">
                    <div className="flex items-center gap-1.5 text-xs font-semibold text-slate-400">
                      <BookOpen className="w-3.5 h-3.5 text-brand-400" />
                      <span>Verified Sources & Citations ({msg.citations.length})</span>
                    </div>

                    <div className="flex flex-wrap gap-2">
                      {msg.citations.map((cite, cIdx) => (
                        <button
                          key={cIdx}
                          onClick={() => setActiveCitation(cite)}
                          className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-slate-900 hover:bg-slate-800 border border-slate-700/80 text-xs font-medium text-slate-300 hover:text-brand-300 transition-colors group"
                        >
                          <span className="font-mono text-brand-400 font-bold">
                            [S{cite.chunkIndex !== undefined ? cite.chunkIndex + 1 : cIdx + 1}]
                          </span>
                          <span className="truncate max-w-[140px] sm:max-w-[200px]">
                            {cite.documentName || 'Document'}
                          </span>
                          <ScoreBadge score={cite.similarityScore} />
                        </button>
                      ))}
                    </div>
                  </div>
                )}

                {/* Message action footer */}
                {!isUser && !msg.isError && (
                  <div className="mt-3 flex items-center justify-end gap-2 pt-2 border-t border-slate-800/60">
                    <button
                      onClick={() => handleCopy(msg.id, msg.text)}
                      className="p-1 rounded text-slate-400 hover:text-white transition-colors"
                      title="Copy Answer"
                    >
                      {copiedId === msg.id ? (
                        <Check className="w-3.5 h-3.5 text-emerald-400" />
                      ) : (
                        <Copy className="w-3.5 h-3.5" />
                      )}
                    </button>
                  </div>
                )}
              </div>

              {/* User Avatar */}
              {isUser && (
                <div className="w-8 h-8 rounded-xl bg-slate-800 border border-slate-700 flex items-center justify-center text-slate-300 flex-shrink-0 mt-1">
                  <User className="w-4 h-4" />
                </div>
              )}
            </div>
          );
        })}

        {/* Loading / Typing indicator */}
        {loading && (
          <div className="flex gap-3 justify-start animate-fade-in">
            <div className="w-8 h-8 rounded-xl bg-gradient-to-tr from-brand-600 to-indigo-600 flex items-center justify-center text-white flex-shrink-0 shadow-md">
              <Bot className="w-4 h-4" />
            </div>
            <div className="glass-card border border-slate-800 rounded-2xl rounded-bl-none p-4 flex items-center gap-3">
              <div className="flex space-x-1.5">
                <div className="w-2 h-2 rounded-full bg-brand-400 animate-bounce" style={{ animationDelay: '0ms' }} />
                <div className="w-2 h-2 rounded-full bg-indigo-400 animate-bounce" style={{ animationDelay: '150ms' }} />
                <div className="w-2 h-2 rounded-full bg-purple-400 animate-bounce" style={{ animationDelay: '300ms' }} />
              </div>
              <span className="text-xs text-slate-400 font-medium">
                Searching vector index and grounding answer...
              </span>
            </div>
          </div>
        )}

        <div ref={messagesEndRef} />
      </div>

      {/* Suggested prompts (if message count is 1) */}
      {messages.length === 1 && (
        <div className="py-2 flex flex-wrap gap-2">
          {sampleQuestions.map((q, i) => (
            <button
              key={i}
              onClick={() => handleSendMessage(q)}
              className="text-xs px-3 py-1.5 rounded-xl bg-slate-900/80 hover:bg-slate-800 border border-slate-700/80 text-slate-300 hover:text-white transition-colors"
            >
              💡 {q}
            </button>
          ))}
        </div>
      )}

      {/* Input Composer Bar */}
      <div className="pt-2 flex-shrink-0">
        <form
          onSubmit={(e) => {
            e.preventDefault();
            handleSendMessage();
          }}
          className="relative flex items-center gap-2 bg-slate-900/90 border border-slate-700/80 rounded-2xl p-2 shadow-2xl focus-within:border-brand-500/80 transition-colors"
        >
          <textarea
            ref={inputRef}
            rows="1"
            value={inputQuery}
            onChange={(e) => setInputQuery(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Ask a question about your knowledge documents (Press Enter to send)..."
            className="flex-1 bg-transparent border-0 px-3 py-2 text-sm text-white placeholder-slate-500 focus:outline-none resize-none max-h-32"
          />

          <button
            type="submit"
            disabled={loading || !inputQuery.trim()}
            className="p-3 rounded-xl bg-brand-600 hover:bg-brand-500 text-white shadow-lg shadow-brand-500/20 disabled:opacity-40 disabled:cursor-not-allowed transition-all"
            title="Send Message"
          >
            <Send className="w-4 h-4" />
          </button>
        </form>
        <p className="text-[10px] text-center text-slate-500 mt-1.5">
          Answers are strictly grounded in retrieved vector chunks. All citations are validated.
        </p>
      </div>

      {/* Citation Inspector Modal */}
      <Modal
        isOpen={Boolean(activeCitation)}
        onClose={() => setActiveCitation(null)}
        title="Source Citation Details"
      >
        {activeCitation && (
          <div className="space-y-4 text-xs">
            <div className="flex items-center justify-between p-3 rounded-xl bg-slate-950/70 border border-slate-800">
              <div>
                <span className="text-[10px] uppercase font-semibold text-slate-400 block">
                  Source Document
                </span>
                <span className="text-sm font-bold text-white">
                  {activeCitation.documentName || 'Unknown Document'}
                </span>
              </div>
              <ScoreBadge score={activeCitation.similarityScore} />
            </div>

            <div className="grid grid-cols-3 gap-2 text-center">
              <div className="p-2.5 rounded-xl bg-slate-900 border border-slate-800">
                <span className="text-slate-400 text-[10px] uppercase block">Chunk ID</span>
                <span className="font-mono font-bold text-brand-400">#{activeCitation.chunkId || '-'}</span>
              </div>
              <div className="p-2.5 rounded-xl bg-slate-900 border border-slate-800">
                <span className="text-slate-400 text-[10px] uppercase block">Chunk Index</span>
                <span className="font-mono font-bold text-white">#{activeCitation.chunkIndex ?? 0}</span>
              </div>
              <div className="p-2.5 rounded-xl bg-slate-900 border border-slate-800">
                <span className="text-slate-400 text-[10px] uppercase block">Page</span>
                <span className="font-mono font-bold text-white">{activeCitation.pageNumber || 1}</span>
              </div>
            </div>

            <div className="pt-2">
              <span className="text-slate-400 text-[10px] uppercase font-semibold block mb-1">
                Cosine Similarity Score
              </span>
              <p className="font-mono text-slate-300">
                {activeCitation.similarityScore ? activeCitation.similarityScore.toFixed(4) : 'N/A'}
              </p>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
};

export default Chat;
