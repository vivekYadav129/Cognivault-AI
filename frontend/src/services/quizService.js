import api from './api';

export const quizService = {
  /**
   * Generates a new intelligent quiz (Skill-Based, Document-Based, or Mixed).
   */
  generateQuiz: async (payload) => {
    const response = await api.post('/quiz/generate', payload);
    return response.data;
  },

  /**
   * Submits user answers for server-side evaluation and scoring.
   */
  submitQuiz: async (quizId, answers) => {
    const response = await api.post(`/quiz/${quizId}/submit`, { answers });
    return response.data;
  },

  /**
   * Retrieves all quizzes and attempt history for the authenticated user.
   */
  getUserQuizzes: async () => {
    const response = await api.get('/quiz');
    return response.data;
  },

  /**
   * Retrieves a specific quiz by ID.
   */
  getQuizById: async (quizId) => {
    const response = await api.get(`/quiz/${quizId}`);
    return response.data;
  },

  /**
   * Extracts normalized technical skills and topics from an uploaded document.
   */
  extractSkills: async (documentId) => {
    const response = await api.get(`/quiz/skills/${documentId}`);
    return response.data;
  },

  /**
   * Runs automated evaluation on a quiz (skill relevance, technical accuracy, duplicate rate).
   */
  evaluateQuiz: async (quizId) => {
    const response = await api.get(`/quiz/${quizId}/evaluate`);
    return response.data;
  },
};

