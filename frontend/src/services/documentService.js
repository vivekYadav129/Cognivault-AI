import api from './api';

export const documentService = {
  async getDocuments() {
    const response = await api.get('/documents');
    return response.data; // DocumentListResponse: { documents: [...], count: N }
  },

  async getDocumentById(id) {
    const response = await api.get(`/documents/${id}`);
    return response.data; // DocumentResponse
  },

  async getDocumentChunks(id) {
    const response = await api.get(`/documents/${id}/chunks`);
    return response.data; // List<DocumentChunkResponse>
  },

  async uploadDocument(file, onUploadProgress) {
    const formData = new FormData();
    formData.append('file', file);

    const response = await api.post('/documents/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
      onUploadProgress: (progressEvent) => {
        if (onUploadProgress && progressEvent.total) {
          const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total);
          onUploadProgress(percent);
        }
      },
    });
    return response.data; // DocumentResponse
  },

  async deleteDocument(id) {
    await api.delete(`/documents/${id}`);
  },
};
