import React from 'react';

export const StatusBadge = ({ status }) => {
  const normalized = (status || '').toUpperCase();

  switch (normalized) {
    case 'COMPLETED':
      return (
        <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
          <span className="w-1.5 h-1.5 rounded-full bg-emerald-400"></span>
          Ready / Indexed
        </span>
      );
    case 'PROCESSING':
      return (
        <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium bg-amber-500/10 text-amber-400 border border-amber-500/20">
          <span className="w-1.5 h-1.5 rounded-full bg-amber-400 animate-ping"></span>
          Processing
        </span>
      );
    case 'PENDING':
      return (
        <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium bg-blue-500/10 text-blue-400 border border-blue-500/20">
          <span className="w-1.5 h-1.5 rounded-full bg-blue-400"></span>
          Queued
        </span>
      );
    case 'FAILED':
      return (
        <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium bg-rose-500/10 text-rose-400 border border-rose-500/20">
          <span className="w-1.5 h-1.5 rounded-full bg-rose-400"></span>
          Failed
        </span>
      );
    default:
      return (
        <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium bg-slate-800 text-slate-300 border border-slate-700">
          {status || 'Unknown'}
        </span>
      );
  }
};

export const ScoreBadge = ({ score }) => {
  const raw = typeof score === 'number' ? score : 0;

  // Calibrate high-dimensional cosine similarity to an intuitive relevance confidence rating
  let percent;
  let color;

  if (raw >= 0.15) {
    percent = Math.min(99, Math.round(85 + ((raw - 0.15) / 0.20) * 13));
    color = 'text-emerald-400 bg-emerald-500/10 border-emerald-500/20';
  } else if (raw >= 0.08) {
    percent = Math.round(75 + ((raw - 0.08) / 0.07) * 10);
    color = 'text-emerald-400 bg-emerald-500/10 border-emerald-500/20';
  } else if (raw >= 0.04) {
    percent = Math.round(60 + ((raw - 0.04) / 0.04) * 15);
    color = 'text-amber-400 bg-amber-500/10 border-amber-500/20';
  } else {
    percent = Math.max(10, Math.round(raw * 100));
    color = 'text-rose-400 bg-rose-500/10 border-rose-500/20';
  }

  return (
    <span
      className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-mono font-medium border ${color}`}
      title={`Raw Cosine Similarity: ${raw.toFixed(4)}`}
    >
      {percent}% match
    </span>
  );
};
