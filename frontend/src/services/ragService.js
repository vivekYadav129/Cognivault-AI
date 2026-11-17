import api from './api';

export const ragService = {
  async queryRag({ query, topK = 5, documentId = null }) {
    const payload = {
      query,
      topK: Math.max(1, Math.min(20, topK || 5)),
    };
    if (documentId !== null && documentId !== undefined && documentId !== '') {
      payload.documentId = Number(documentId);
    }
    const response = await api.post('/rag/query', payload);
    return response.data; // RagQueryResponse: { answer, citations, sourcesUsed }
  },

  async search({ query, topK = 5, documentId = null }) {
    const payload = {
      query,
      topK: Math.max(1, Math.min(20, topK || 5)),
    };
    if (documentId !== null && documentId !== undefined && documentId !== '') {
      payload.documentId = Number(documentId);
    }
    const response = await api.post('/search', payload);
    return response.data; // SearchResponse: { query, results: [...] }
  },
};
