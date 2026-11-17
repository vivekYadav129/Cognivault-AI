import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { documentService } from '../services/documentService';
import { quizService } from '../services/quizService';
import LoadingSpinner from '../components/common/LoadingSpinner';
import EmptyState from '../components/common/EmptyState';
import {
  Brain,
  Sparkles,
  CheckCircle2,
  XCircle,
  RotateCcw,
  Award,
  ArrowRight,
  ArrowLeft,
  BookOpen,
  Code2,
  Bug,
  HelpCircle,
  Layers,
  History,
  Check,
  Cpu,
  ShieldCheck,
  Flame,
  Gauge
} from 'lucide-react';
import toast from 'react-hot-toast';

const CATEGORY_OPTIONS = [
  { id: 'CONCEPTUAL', label: 'Conceptual', icon: BookOpen },
  { id: 'CODING', label: 'Coding', icon: Code2 },
  { id: 'DEBUGGING', label: 'Debugging', icon: Bug },
  { id: 'SCENARIO', label: 'Scenario-Based', icon: HelpCircle },
  { id: 'OUTPUT_PREDICTION', label: 'Output Prediction', icon: Cpu },
  { id: 'REAL_WORLD', label: 'Real-World System', icon: Layers },
];

export const Quiz = () => {
  const [searchParams] = useSearchParams();
  const initialDocId = searchParams.get('docId') || '';

  // Documents state
  const [documents, setDocuments] = useState([]);
  const [loadingDocs, setLoadingDocs] = useState(true);

  // Configuration state
  const [selectedDocId, setSelectedDocId] = useState(initialDocId);
  const [quizMode, setQuizMode] = useState('SKILL_BASED'); // 'SKILL_BASED' | 'DOCUMENT_BASED' | 'MIXED'
  const [difficulty, setDifficulty] = useState('INTERMEDIATE'); // 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED'
  const [questionCount, setQuestionCount] = useState(5);
  const [selectedCategories, setSelectedCategories] = useState([
    'CONCEPTUAL',
    'SCENARIO',
    'CODING',
    'DEBUGGING',
  ]);
  const [topicFocus, setTopicFocus] = useState('');

  // Extracted skills preview state
  const [extractedSkillsData, setExtractedSkillsData] = useState(null);
  const [loadingSkills, setLoadingSkills] = useState(false);

  // Quiz lifecycle state: 'setup' | 'generating' | 'active' | 'submitting' | 'results' | 'history'
  const [stage, setStage] = useState('setup');
  const [activeQuiz, setActiveQuiz] = useState(null);
  const [currentQuestionIndex, setCurrentQuestionIndex] = useState(0);
  const [userAnswers, setUserAnswers] = useState({}); // { [questionId]: optionIndex }
  const [quizResult, setQuizResult] = useState(null);

  // Quality evaluation report state
  const [evaluationReport, setEvaluationReport] = useState(null);
  const [loadingEval, setLoadingEval] = useState(false);

  // User Quiz History state
  const [quizHistory, setQuizHistory] = useState([]);
  const [loadingHistory, setLoadingHistory] = useState(false);

  // Load documents on mount
  useEffect(() => {
    const fetchDocs = async () => {
      try {
        const data = await documentService.getDocuments();
        setDocuments(data.documents || []);
      } catch (err) {
        console.error('Failed to load documents', err);
      } finally {
        setLoadingDocs(false);
      }
    };
    fetchDocs();
  }, []);

  // When a document is selected, auto-extract skills to preview
  useEffect(() => {
    if (!selectedDocId) {
      setExtractedSkillsData(null);
      return;
    }
    const fetchSkills = async () => {
      setLoadingSkills(true);
      try {
        const res = await quizService.extractSkills(selectedDocId);
        setExtractedSkillsData(res);
      } catch (err) {
        console.warn('Skill preview extraction failed', err);
      } finally {
        setLoadingSkills(false);
      }
    };
    fetchSkills();
  }, [selectedDocId]);

  const toggleCategory = (catId) => {
    if (selectedCategories.includes(catId)) {
      if (selectedCategories.length === 1) {
        toast.error('Select at least one question category');
        return;
      }
      setSelectedCategories(selectedCategories.filter((c) => c !== catId));
    } else {
      setSelectedCategories([...selectedCategories, catId]);
    }
  };

  const handleGenerateQuiz = async () => {
    setStage('generating');
    setUserAnswers({});
    setCurrentQuestionIndex(0);
    setQuizResult(null);
    setEvaluationReport(null);

    const payload = {
      documentId: selectedDocId ? Number(selectedDocId) : null,
      mode: quizMode,
      difficulty: difficulty,
      questionCount: Number(questionCount),
      categories: selectedCategories,
      topicFocus: topicFocus.trim() || null,
    };

    try {
      const quiz = await quizService.generateQuiz(payload);
      setActiveQuiz(quiz);
      setStage('active');
      toast.success(`Generated ${quiz.questions?.length || 0}-question ${quiz.mode.replace('_', ' ')} assessment!`);
    } catch (err) {
      console.error('Quiz generation failed', err);
      const errMsg = err.response?.data?.message || 'Failed to generate quiz. Please check parameters.';
      toast.error(errMsg);
      setStage('setup');
    }
  };

  const handleSelectOption = (questionId, optionIdx) => {
    setUserAnswers((prev) => ({
      ...prev,
      [questionId]: optionIdx,
    }));
  };

  const handleSubmitQuiz = async () => {
    if (!activeQuiz) return;
    const answeredCount = Object.keys(userAnswers).length;
    const totalCount = activeQuiz.questions.length;

    if (answeredCount < totalCount) {
      const confirmSubmit = window.confirm(
        `You have answered ${answeredCount} of ${totalCount} questions. Submit anyway?`
      );
      if (!confirmSubmit) return;
    }

    setStage('submitting');
    try {
      const result = await quizService.submitQuiz(activeQuiz.id, userAnswers);
      setQuizResult(result);
      setStage('results');
      toast.success(`Quiz submitted! Final Score: ${result.score} / ${result.totalQuestions}`);
    } catch (err) {
      console.error('Quiz submission failed', err);
      toast.error('Failed to submit quiz answers. Please try again.');
      setStage('active');
    }
  };

  const handleFetchHistory = async () => {
    setLoadingHistory(true);
    setStage('history');
    try {
      const historyList = await quizService.getUserQuizzes();
      setQuizHistory(historyList || []);
    } catch (err) {
      console.error('Failed to load history', err);
      toast.error('Failed to load quiz history.');
    } finally {
      setLoadingHistory(false);
    }
  };

  const handleEvaluateQuiz = async () => {
    if (!activeQuiz) return;
    setLoadingEval(true);
    try {
      const report = await quizService.evaluateQuiz(activeQuiz.id);
      setEvaluationReport(report);
      toast.success('AI Evaluation Report calculated successfully!');
    } catch (err) {
      console.error('Evaluation failed', err);
      toast.error('Failed to calculate evaluation metrics.');
    } finally {
      setLoadingEval(false);
    }
  };

  if (loadingDocs) {
    return (
      <div className="py-20 flex justify-center">
        <LoadingSpinner text="Loading Quiz Arena..." size="lg" />
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto space-y-8 animate-fade-in pb-12">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-indigo-500/10 border border-indigo-500/30 text-indigo-300 text-xs font-semibold mb-2">
            <Sparkles className="w-3.5 h-3.5" />
            <span>Intelligent AI Technical Assessment</span>
          </div>
          <h2 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">Quiz Arena</h2>
          <p className="text-xs sm:text-sm text-slate-400 mt-1">
            Generate skill-based technical assessments, coding challenges, and grounded knowledge tests
          </p>
        </div>

        <div className="flex items-center gap-2">
          {stage !== 'history' && (
            <button
              onClick={handleFetchHistory}
              className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl text-xs font-semibold bg-slate-900 hover:bg-slate-800 text-slate-300 border border-slate-800 transition-colors"
            >
              <History className="w-3.5 h-3.5 text-slate-400" />
              <span>Quiz History</span>
            </button>
          )}

          {stage !== 'setup' && (
            <button
              onClick={() => {
                setStage('setup');
                setActiveQuiz(null);
                setQuizResult(null);
              }}
              className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl text-xs font-semibold bg-brand-600 hover:bg-brand-500 text-white transition-colors shadow-md shadow-brand-500/20"
            >
              <RotateCcw className="w-3.5 h-3.5" />
              <span>New Assessment</span>
            </button>
          )}
        </div>
      </div>

      {/* STAGE: SETUP FORM */}
      {stage === 'setup' && (
        <div className="glass-card rounded-3xl border border-slate-800 p-6 sm:p-8 space-y-8">
          <div className="flex items-center gap-3 pb-4 border-b border-slate-800">
            <div className="w-10 h-10 rounded-xl bg-brand-500/10 border border-brand-500/20 flex items-center justify-center text-brand-400">
              <Brain className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-white">Configure Your Assessment</h3>
              <p className="text-xs text-slate-400">
                Choose evaluation mode, technical skills, difficulty, and question categories
              </p>
            </div>
          </div>

          {/* 1. QUIZ MODE SELECTOR */}
          <div className="space-y-3">
            <label className="block text-xs font-bold uppercase tracking-wider text-slate-300">
              1. Assessment Mode
            </label>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
              {/* Skill-Based */}
              <div
                onClick={() => setQuizMode('SKILL_BASED')}
                className={`p-4 rounded-2xl border cursor-pointer transition-all ${
                  quizMode === 'SKILL_BASED'
                    ? 'bg-brand-600/15 border-brand-500 text-white shadow-lg shadow-brand-500/10'
                    : 'bg-slate-900/60 border-slate-800 text-slate-300 hover:border-slate-700'
                }`}
              >
                <div className="flex items-center justify-between mb-1.5">
                  <div className="flex items-center gap-2">
                    <Code2 className="w-4 h-4 text-brand-400" />
                    <span className="font-bold text-sm">Skill-Based</span>
                  </div>
                  <span className="text-[10px] px-2 py-0.5 rounded-full bg-emerald-500/20 text-emerald-300 font-semibold">
                    Recommended
                  </span>
                </div>
                <p className="text-xs text-slate-400 leading-relaxed">
                  Extracts candidate skills from resumes and tests deep technical knowledge, trade-offs, and architecture.
                </p>
              </div>

              {/* Document-Based */}
              <div
                onClick={() => setQuizMode('DOCUMENT_BASED')}
                className={`p-4 rounded-2xl border cursor-pointer transition-all ${
                  quizMode === 'DOCUMENT_BASED'
                    ? 'bg-brand-600/15 border-brand-500 text-white shadow-lg shadow-brand-500/10'
                    : 'bg-slate-900/60 border-slate-800 text-slate-300 hover:border-slate-700'
                }`}
              >
                <div className="flex items-center gap-2 mb-1.5">
                  <BookOpen className="w-4 h-4 text-indigo-400" />
                  <span className="font-bold text-sm">Document-Based</span>
                </div>
                <p className="text-xs text-slate-400 leading-relaxed">
                  Strictly grounded in uploaded documents. Every question includes verified chunk source citations.
                </p>
              </div>

              {/* Mixed */}
              <div
                onClick={() => setQuizMode('MIXED')}
                className={`p-4 rounded-2xl border cursor-pointer transition-all ${
                  quizMode === 'MIXED'
                    ? 'bg-brand-600/15 border-brand-500 text-white shadow-lg shadow-brand-500/10'
                    : 'bg-slate-900/60 border-slate-800 text-slate-300 hover:border-slate-700'
                }`}
              >
                <div className="flex items-center gap-2 mb-1.5">
                  <Layers className="w-4 h-4 text-purple-400" />
                  <span className="font-bold text-sm">Mixed Assessment</span>
                </div>
                <p className="text-xs text-slate-400 leading-relaxed">
                  Balanced mix combining document context and deep technical skill questions with explicit grounding tags.
                </p>
              </div>
            </div>
          </div>

          {/* 2. DOCUMENT SELECTION & EXTRACTED SKILLS PREVIEW */}
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <label className="block text-xs font-bold uppercase tracking-wider text-slate-300">
                2. Source Document (Optional for direct topic tests)
              </label>
              {loadingSkills && (
                <span className="text-[11px] text-brand-400 flex items-center gap-1">
                  <LoadingSpinner size="sm" /> Extracting skills...
                </span>
              )}
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 max-h-56 overflow-y-auto pr-1">
              <div
                onClick={() => setSelectedDocId('')}
                className={`p-3.5 rounded-2xl border cursor-pointer transition-all ${
                  selectedDocId === ''
                    ? 'bg-brand-600/15 border-brand-500 text-white'
                    : 'bg-slate-900/60 border-slate-800 text-slate-300 hover:border-slate-700'
                }`}
              >
                <div className="flex items-center justify-between">
                  <span className="font-semibold text-sm">No Document (Direct Topic Focus)</span>
                  <span className="text-[10px] px-2 py-0.5 rounded bg-slate-800 text-slate-400">
                    Custom Topic
                  </span>
                </div>
                <p className="text-xs text-slate-400 mt-1">Assess arbitrary technologies without uploading a file</p>
              </div>

              {documents.map((doc) => (
                <div
                  key={doc.id}
                  onClick={() => setSelectedDocId(String(doc.id))}
                  className={`p-3.5 rounded-2xl border cursor-pointer transition-all ${
                    String(selectedDocId) === String(doc.id)
                      ? 'bg-brand-600/15 border-brand-500 text-white'
                      : 'bg-slate-900/60 border-slate-800 text-slate-300 hover:border-slate-700'
                  }`}
                >
                  <div className="flex items-center justify-between">
                    <span className="font-semibold text-sm truncate max-w-[200px]" title={doc.originalFileName}>
                      {doc.originalFileName || doc.fileName}
                    </span>
                    <span className="text-[10px] px-2 py-0.5 rounded bg-slate-800 text-slate-400 font-mono">
                      #{doc.id}
                    </span>
                  </div>
                  <p className="text-xs text-slate-400 mt-1">
                    {doc.chunkCount || 0} chunks • {doc.fileType || 'PDF'}
                  </p>
                </div>
              ))}
            </div>

            {/* Extracted Skills Preview Pills */}
            {extractedSkillsData && extractedSkillsData.allSkills?.length > 0 && (
              <div className="p-4 rounded-2xl bg-slate-900/80 border border-slate-800 space-y-2.5">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-slate-300 flex items-center gap-1.5">
                    <ShieldCheck className="w-4 h-4 text-emerald-400" />
                    Detected Candidate Skills ({extractedSkillsData.allSkills.length})
                  </span>
                  <span className="text-[10px] text-slate-500">Click a skill to focus assessment</span>
                </div>
                <div className="flex flex-wrap gap-1.5">
                  {extractedSkillsData.allSkills.map((skill) => (
                    <button
                      key={skill}
                      type="button"
                      onClick={() => setTopicFocus(skill)}
                      className={`px-2.5 py-1 rounded-lg text-xs font-medium border transition-colors ${
                        topicFocus.toLowerCase() === skill.toLowerCase()
                          ? 'bg-brand-500 text-white border-brand-400 shadow-sm'
                          : 'bg-slate-800/80 text-slate-300 border-slate-700 hover:border-brand-500'
                      }`}
                    >
                      {skill}
                    </button>
                  ))}
                </div>
              </div>
            )}
          </div>

          {/* 3. DIFFICULTY & QUESTION COUNT */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
            {/* Difficulty */}
            <div className="space-y-2">
              <label className="block text-xs font-bold uppercase tracking-wider text-slate-300">
                3. Difficulty Level
              </label>
              <div className="grid grid-cols-3 gap-2">
                {[
                  { id: 'BEGINNER', label: 'Beginner', icon: ShieldCheck, color: 'text-emerald-400' },
                  { id: 'INTERMEDIATE', label: 'Mid-Level', icon: Gauge, color: 'text-brand-400' },
                  { id: 'ADVANCED', label: 'Senior', icon: Flame, color: 'text-amber-400' },
                ].map((item) => (
                  <button
                    key={item.id}
                    type="button"
                    onClick={() => setDifficulty(item.id)}
                    className={`py-2.5 rounded-xl text-xs font-bold border transition-all flex flex-col items-center justify-center gap-1 ${
                      difficulty === item.id
                        ? 'bg-brand-600/20 border-brand-500 text-white shadow-md'
                        : 'bg-slate-900 border-slate-800 text-slate-400 hover:border-slate-700'
                    }`}
                  >
                    <item.icon className={`w-3.5 h-3.5 ${item.color}`} />
                    <span>{item.label}</span>
                  </button>
                ))}
              </div>
            </div>

            {/* Question Count */}
            <div className="space-y-2">
              <label className="block text-xs font-bold uppercase tracking-wider text-slate-300">
                4. Number of Questions
              </label>
              <div className="grid grid-cols-4 gap-2">
                {[5, 10, 15, 20].map((num) => (
                  <button
                    key={num}
                    type="button"
                    onClick={() => setQuestionCount(num)}
                    className={`py-2.5 rounded-xl text-xs font-bold border transition-all ${
                      questionCount === num
                        ? 'bg-brand-600 border-brand-500 text-white shadow-md'
                        : 'bg-slate-900 border-slate-800 text-slate-400 hover:border-slate-700'
                    }`}
                  >
                    {num} Qs
                  </button>
                ))}
              </div>
            </div>
          </div>

          {/* 4. QUESTION CATEGORIES (MULTI-SELECT) */}
          <div className="space-y-2.5">
            <label className="block text-xs font-bold uppercase tracking-wider text-slate-300">
              5. Question Categories (Select all that apply)
            </label>
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-2.5">
              {CATEGORY_OPTIONS.map((cat) => {
                const isSelected = selectedCategories.includes(cat.id);
                return (
                  <button
                    key={cat.id}
                    type="button"
                    onClick={() => toggleCategory(cat.id)}
                    className={`p-3 rounded-xl border text-left text-xs font-medium transition-all flex items-center justify-between gap-2 ${
                      isSelected
                        ? 'bg-indigo-600/20 border-indigo-500 text-white'
                        : 'bg-slate-900/60 border-slate-800 text-slate-400 hover:border-slate-700'
                    }`}
                  >
                    <div className="flex items-center gap-2">
                      <cat.icon className={`w-3.5 h-3.5 ${isSelected ? 'text-indigo-400' : 'text-slate-500'}`} />
                      <span>{cat.label}</span>
                    </div>
                    {isSelected && <Check className="w-3.5 h-3.5 text-indigo-400" />}
                  </button>
                );
              })}
            </div>
          </div>

          {/* 5. TOPIC FOCUS */}
          <div className="space-y-2">
            <label className="block text-xs font-bold uppercase tracking-wider text-slate-300">
              6. Technology / Topic Focus (Optional)
            </label>
            <input
              type="text"
              value={topicFocus}
              onChange={(e) => setTopicFocus(e.target.value)}
              placeholder="e.g. Java Collections, Spring Security, React Hooks, MongoDB Aggregation, pgvector"
              className="w-full px-4 py-2.5 bg-slate-900 border border-slate-700/80 rounded-xl text-sm text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-brand-500"
            />
          </div>

          {/* GENERATE BUTTON */}
          <div className="pt-2">
            <button
              type="button"
              onClick={handleGenerateQuiz}
              className="w-full flex items-center justify-center gap-2 py-3.5 rounded-2xl bg-brand-600 hover:bg-brand-500 text-white font-bold text-sm shadow-xl shadow-brand-500/25 transition-all"
            >
              <Sparkles className="w-4 h-4" />
              <span>Generate AI Assessment</span>
            </button>
          </div>
        </div>
      )}

      {/* STAGE: GENERATING LOADER */}
      {stage === 'generating' && (
        <div className="glass-card rounded-3xl border border-slate-800 p-12 text-center space-y-4">
          <div className="w-16 h-16 rounded-2xl bg-brand-600/20 border border-brand-500/30 flex items-center justify-center text-brand-400 mx-auto animate-pulse">
            <Brain className="w-8 h-8" />
          </div>
          <h3 className="text-xl font-bold text-white">Generating Assessment Questions...</h3>
          <p className="text-xs sm:text-sm text-slate-400 max-w-md mx-auto leading-relaxed">
            Extracting core technical competencies, verifying distractor plausibility, and ensuring zero question duplicates.
          </p>
        </div>
      )}

      {/* STAGE: SUBMITTING LOADER */}
      {stage === 'submitting' && (
        <div className="glass-card rounded-3xl border border-slate-800 p-12 text-center space-y-4">
          <div className="w-16 h-16 rounded-2xl bg-emerald-600/20 border border-emerald-500/30 flex items-center justify-center text-emerald-400 mx-auto animate-pulse">
            <ShieldCheck className="w-8 h-8" />
          </div>
          <h3 className="text-xl font-bold text-white">Evaluating Assessment Server-Side...</h3>
          <p className="text-xs sm:text-sm text-slate-400 max-w-md mx-auto leading-relaxed">
            Calculating topic-wise competence, verifying answers, and generating comprehensive explanations.
          </p>
        </div>
      )}

      {/* STAGE: ACTIVE QUIZ */}
      {stage === 'active' && activeQuiz && (
        <div className="space-y-6">
          {/* Progress Header & Question Palette */}
          <div className="glass-card rounded-2xl border border-slate-800 p-4 space-y-3">
            <div className="flex items-center justify-between text-xs font-semibold text-slate-400">
              <span className="flex items-center gap-2">
                <span className="text-white font-bold">{activeQuiz.title}</span>
                <span className="text-[10px] px-2 py-0.5 rounded bg-brand-500/20 text-brand-300 font-mono">
                  {activeQuiz.mode.replace('_', ' ')}
                </span>
              </span>
              <span>
                Question {currentQuestionIndex + 1} of {activeQuiz.questions.length}
              </span>
            </div>

            {/* Progress Bar */}
            <div className="w-full bg-slate-800 rounded-full h-2 overflow-hidden">
              <div
                className="bg-brand-500 h-full transition-all duration-300"
                style={{
                  width: `${((currentQuestionIndex + 1) / activeQuiz.questions.length) * 100}%`,
                }}
              />
            </div>

            {/* Question Quick Palette */}
            <div className="flex flex-wrap gap-1.5 pt-1">
              {activeQuiz.questions.map((q, idx) => {
                const isAnswered = userAnswers[q.id] !== undefined;
                const isCurrent = idx === currentQuestionIndex;
                return (
                  <button
                    key={q.id}
                    type="button"
                    onClick={() => setCurrentQuestionIndex(idx)}
                    className={`w-7 h-7 rounded-lg text-xs font-bold transition-all ${
                      isCurrent
                        ? 'bg-brand-500 text-white ring-2 ring-brand-400'
                        : isAnswered
                        ? 'bg-slate-700 text-emerald-300'
                        : 'bg-slate-900 text-slate-400 border border-slate-800'
                    }`}
                  >
                    {idx + 1}
                  </button>
                );
              })}
            </div>
          </div>

          {/* Current Question Card */}
          {(() => {
            const currentQ = activeQuiz.questions[currentQuestionIndex];
            const selectedOpt = userAnswers[currentQ.id];

            return (
              <div className="glass-card rounded-3xl border border-slate-800 p-6 sm:p-8 space-y-6">
                {/* Badges bar */}
                <div className="flex flex-wrap items-center justify-between gap-2 pb-2 border-b border-slate-800/80">
                  <div className="flex items-center gap-2">
                    <span className="text-xs font-mono uppercase text-brand-400 font-bold tracking-wider">
                      Question #{currentQuestionIndex + 1}
                    </span>
                    {currentQ.skillTag && (
                      <span className="text-[11px] px-2.5 py-0.5 rounded-full bg-slate-800 text-slate-300 border border-slate-700">
                        {currentQ.skillTag}
                      </span>
                    )}
                    <span className="text-[11px] px-2 py-0.5 rounded-full bg-slate-800 text-slate-400">
                      {currentQ.category}
                    </span>
                  </div>

                  {currentQ.groundingType === 'DOCUMENT_GROUNDED' ? (
                    <span className="text-[10px] px-2.5 py-0.5 rounded-full bg-indigo-500/20 text-indigo-300 border border-indigo-500/30 font-semibold">
                      Document Grounded {currentQ.sourceCitation}
                    </span>
                  ) : (
                    <span className="text-[10px] px-2.5 py-0.5 rounded-full bg-slate-800 text-slate-400 font-semibold">
                      Technical Knowledge
                    </span>
                  )}
                </div>

                {/* Question Text */}
                <h3 className="text-base sm:text-lg font-bold text-white leading-snug">
                  {currentQ.questionText}
                </h3>

                {/* Optional Code Snippet */}
                {currentQ.codeSnippet && (
                  <div className="rounded-2xl bg-slate-950 p-4 border border-slate-800 font-mono text-xs text-brand-300 overflow-x-auto whitespace-pre">
                    {currentQ.codeSnippet}
                  </div>
                )}

                {/* Options List */}
                <div className="space-y-3">
                  {currentQ.options.map((option, optIdx) => {
                    const isSelected = selectedOpt === optIdx;
                    return (
                      <button
                        key={optIdx}
                        type="button"
                        onClick={() => handleSelectOption(currentQ.id, optIdx)}
                        className={`w-full p-4 rounded-2xl border text-left text-sm font-medium transition-all flex items-center justify-between gap-3 ${
                          isSelected
                            ? 'bg-brand-600/20 border-brand-500 text-white shadow-md'
                            : 'bg-slate-900/60 border-slate-800 text-slate-300 hover:border-slate-700'
                        }`}
                      >
                        <div className="flex items-center gap-3">
                          <span
                            className={`w-7 h-7 rounded-lg flex items-center justify-center font-bold text-xs ${
                              isSelected
                                ? 'bg-brand-500 text-white'
                                : 'bg-slate-800 text-slate-300'
                            }`}
                          >
                            {String.fromCharCode(65 + optIdx)}
                          </span>
                          <span>{option}</span>
                        </div>
                        {isSelected && <Check className="w-5 h-5 text-brand-400 flex-shrink-0" />}
                      </button>
                    );
                  })}
                </div>

                {/* Navigation Controls */}
                <div className="flex items-center justify-between pt-4 border-t border-slate-800">
                  <button
                    type="button"
                    onClick={() => setCurrentQuestionIndex((prev) => Math.max(0, prev - 1))}
                    disabled={currentQuestionIndex === 0}
                    className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl text-xs font-semibold bg-slate-900 hover:bg-slate-800 text-slate-300 border border-slate-800 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
                  >
                    <ArrowLeft className="w-3.5 h-3.5" />
                    <span>Previous</span>
                  </button>

                  {currentQuestionIndex + 1 < activeQuiz.questions.length ? (
                    <button
                      type="button"
                      onClick={() => setCurrentQuestionIndex((prev) => prev + 1)}
                      className="inline-flex items-center gap-1.5 px-5 py-2.5 rounded-xl bg-brand-600 hover:bg-brand-500 text-white text-xs font-bold transition-all shadow-md shadow-brand-500/20"
                    >
                      <span>Next Question</span>
                      <ArrowRight className="w-3.5 h-3.5" />
                    </button>
                  ) : (
                    <button
                      type="button"
                      onClick={handleSubmitQuiz}
                      className="inline-flex items-center gap-1.5 px-6 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold transition-all shadow-lg shadow-emerald-500/25"
                    >
                      <CheckCircle2 className="w-4 h-4" />
                      <span>Submit Assessment</span>
                    </button>
                  )}
                </div>
              </div>
            );
          })()}
        </div>
      )}

      {/* STAGE: RESULTS & ANALYTICS */}
      {stage === 'results' && quizResult && (
        <div className="glass-card rounded-3xl border border-slate-800 p-6 sm:p-10 space-y-8 animate-fade-in">
          {/* Header Score Card */}
          <div className="text-center space-y-3">
            <div
              className={`w-20 h-20 rounded-3xl mx-auto flex items-center justify-center p-4 border ${
                quizResult.percentage >= 70
                  ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-400'
                  : 'bg-amber-500/10 border-amber-500/30 text-amber-400'
              }`}
            >
              <Award className="w-10 h-10" />
            </div>
            <h3 className="text-2xl sm:text-3xl font-extrabold text-white">Assessment Complete!</h3>
            <p className="text-slate-400 text-xs sm:text-sm max-w-md mx-auto">
              {quizResult.percentage >= 70
                ? 'Strong demonstration of technical competence and architectural principles.'
                : 'Review the technical explanations and deep concepts below to strengthen your domain knowledge.'}
            </p>

            <div className="pt-3 flex items-center justify-center gap-4">
              <div className="px-6 py-3.5 rounded-2xl bg-slate-900/90 border border-slate-800">
                <span className="text-[11px] uppercase font-bold text-slate-400 block">Score</span>
                <span className="text-2xl font-black text-white">
                  {quizResult.score} / {quizResult.totalQuestions}
                </span>
              </div>
              <div className="px-6 py-3.5 rounded-2xl bg-slate-900/90 border border-slate-800">
                <span className="text-[11px] uppercase font-bold text-slate-400 block">Accuracy</span>
                <span
                  className={`text-2xl font-black ${
                    quizResult.percentage >= 70 ? 'text-emerald-400' : 'text-amber-400'
                  }`}
                >
                  {quizResult.percentage}%
                </span>
              </div>
            </div>
          </div>

          {/* Topic-Wise Competence Breakdown */}
          {quizResult.topicBreakdown && Object.keys(quizResult.topicBreakdown).length > 0 && (
            <div className="space-y-3 pt-4 border-t border-slate-800">
              <h4 className="text-xs font-bold uppercase tracking-wider text-slate-300">
                Topic-Wise Technical Performance
              </h4>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                {Object.entries(quizResult.topicBreakdown).map(([topic, stat]) => (
                  <div key={topic} className="p-3.5 rounded-xl bg-slate-900/60 border border-slate-800 space-y-1.5">
                    <div className="flex items-center justify-between text-xs">
                      <span className="font-semibold text-white">{topic}</span>
                      <span className="font-mono text-slate-400">
                        {stat.correct}/{stat.total} ({stat.percentage}%)
                      </span>
                    </div>
                    <div className="w-full bg-slate-800 rounded-full h-1.5 overflow-hidden">
                      <div
                        className={`h-full ${
                          stat.percentage >= 70 ? 'bg-emerald-500' : 'bg-amber-500'
                        }`}
                        style={{ width: `${stat.percentage}%` }}
                      />
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Question Breakdown with Explanations */}
          <div className="space-y-4 pt-4 border-t border-slate-800">
            <h4 className="text-xs font-bold uppercase tracking-wider text-slate-300">
              Question Breakdown & Detailed Explanations
            </h4>

            <div className="space-y-4">
              {quizResult.questionResults.map((qr, idx) => (
                <div
                  key={qr.questionId}
                  className="p-5 rounded-2xl bg-slate-950/70 border border-slate-800/80 space-y-3 text-xs"
                >
                  <div className="flex items-start justify-between gap-3">
                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        <span className="text-[10px] font-mono text-slate-500">#{idx + 1}</span>
                        {qr.skillTag && (
                          <span className="text-[10px] px-2 py-0.5 rounded bg-slate-800 text-brand-300 font-semibold">
                            {qr.skillTag}
                          </span>
                        )}
                        <span className="text-[10px] px-2 py-0.5 rounded bg-slate-800 text-slate-400">
                          {qr.category}
                        </span>
                      </div>
                      <h5 className="font-bold text-white text-sm">{qr.questionText}</h5>
                    </div>

                    {qr.correct ? (
                      <span className="px-2.5 py-1 rounded-full bg-emerald-500/10 text-emerald-400 font-bold border border-emerald-500/20 flex-shrink-0 flex items-center gap-1">
                        <CheckCircle2 className="w-3.5 h-3.5" /> Correct
                      </span>
                    ) : (
                      <span className="px-2.5 py-1 rounded-full bg-rose-500/10 text-rose-400 font-bold border border-rose-500/20 flex-shrink-0 flex items-center gap-1">
                        <XCircle className="w-3.5 h-3.5" /> Incorrect
                      </span>
                    )}
                  </div>

                  {qr.codeSnippet && (
                    <div className="rounded-xl bg-slate-900 p-3 border border-slate-800 font-mono text-xs text-brand-300 whitespace-pre overflow-x-auto">
                      {qr.codeSnippet}
                    </div>
                  )}

                  {/* Options Evaluation */}
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 pt-1">
                    {qr.options.map((opt, optIdx) => {
                      const isUserPick = qr.selectedOptionIndex === optIdx;
                      const isCorrectPick = qr.correctAnswerIndex === optIdx;

                      let badgeStyle = 'bg-slate-900/50 border-slate-800/60 text-slate-400';
                      if (isCorrectPick) {
                        badgeStyle = 'bg-emerald-500/15 border-emerald-500/50 text-emerald-200 font-semibold';
                      } else if (isUserPick && !isCorrectPick) {
                        badgeStyle = 'bg-rose-500/15 border-rose-500/50 text-rose-200 font-semibold';
                      }

                      return (
                        <div
                          key={optIdx}
                          className={`p-2.5 rounded-xl border flex items-center justify-between text-xs ${badgeStyle}`}
                        >
                          <div className="flex items-center gap-2">
                            <span className="w-5 h-5 rounded flex items-center justify-center font-bold text-[10px] bg-slate-800">
                              {String.fromCharCode(65 + optIdx)}
                            </span>
                            <span>{opt}</span>
                          </div>
                          {isCorrectPick && <CheckCircle2 className="w-4 h-4 text-emerald-400" />}
                          {isUserPick && !isCorrectPick && <XCircle className="w-4 h-4 text-rose-400" />}
                        </div>
                      );
                    })}
                  </div>

                  {/* Explanation Block */}
                  <div className="p-3.5 rounded-xl bg-slate-900/90 border border-slate-800 space-y-1">
                    <span className="text-[10px] font-bold uppercase text-brand-400 flex items-center gap-1">
                      <BookOpen className="w-3 h-3" /> Technical Explanation
                      {qr.sourceCitation && (
                        <span className="text-slate-400 font-normal">({qr.sourceCitation})</span>
                      )}
                    </span>
                    <p className="text-slate-300 text-xs leading-relaxed">{qr.explanation}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* AI Accuracy & Quality Evaluation Section */}
          <div className="p-5 rounded-2xl bg-slate-900/60 border border-slate-800 space-y-3">
            <div className="flex items-center justify-between">
              <div>
                <h4 className="text-xs font-bold uppercase tracking-wider text-slate-300">
                  AI Assessment Quality & Grounding Audit
                </h4>
                <p className="text-[11px] text-slate-400">
                  Evaluate question uniqueness, technical accuracy, and grounding faithfulness
                </p>
              </div>

              <button
                type="button"
                onClick={handleEvaluateQuiz}
                disabled={loadingEval}
                className="px-3 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-semibold border border-slate-700 transition-colors"
              >
                {loadingEval ? 'Calculating...' : 'Run Quality Audit'}
              </button>
            </div>

            {evaluationReport && (
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5 pt-2">
                <div className="p-2.5 rounded-xl bg-slate-950 border border-slate-800 text-center">
                  <span className="text-[10px] text-slate-400 block">Skill Relevance</span>
                  <span className="text-sm font-bold text-emerald-400">
                    {evaluationReport.skillRelevanceRate}%
                  </span>
                </div>
                <div className="p-2.5 rounded-xl bg-slate-950 border border-slate-800 text-center">
                  <span className="text-[10px] text-slate-400 block">Technical Accuracy</span>
                  <span className="text-sm font-bold text-brand-400">
                    {evaluationReport.technicalAccuracyRate}%
                  </span>
                </div>
                <div className="p-2.5 rounded-xl bg-slate-950 border border-slate-800 text-center">
                  <span className="text-[10px] text-slate-400 block">Duplicate Rate</span>
                  <span className="text-sm font-bold text-emerald-400">
                    {evaluationReport.duplicateRate}%
                  </span>
                </div>
                <div className="p-2.5 rounded-xl bg-slate-950 border border-slate-800 text-center">
                  <span className="text-[10px] text-slate-400 block">Grounding Faithfulness</span>
                  <span className="text-sm font-bold text-purple-400">
                    {evaluationReport.groundingFaithfulnessRate}%
                  </span>
                </div>
              </div>
            )}
          </div>

          {/* Action Buttons */}
          <div className="flex flex-col sm:flex-row items-center justify-center gap-3 pt-4">
            <button
              onClick={handleGenerateQuiz}
              className="w-full sm:w-auto px-6 py-2.5 rounded-xl bg-brand-600 hover:bg-brand-500 text-white text-xs font-bold shadow-md shadow-brand-500/20 transition-all flex items-center justify-center gap-1.5"
            >
              <RotateCcw className="w-3.5 h-3.5" />
              <span>Retry with Fresh Questions</span>
            </button>

            <button
              onClick={() => {
                setStage('setup');
                setActiveQuiz(null);
                setQuizResult(null);
              }}
              className="w-full sm:w-auto px-6 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-bold border border-slate-700 transition-colors"
            >
              Configure New Assessment
            </button>
          </div>
        </div>
      )}

      {/* STAGE: QUIZ HISTORY */}
      {stage === 'history' && (
        <div className="glass-card rounded-3xl border border-slate-800 p-6 sm:p-8 space-y-6 animate-fade-in">
          <div className="flex items-center justify-between pb-4 border-b border-slate-800">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-brand-500/10 border border-brand-500/20 flex items-center justify-center text-brand-400">
                <History className="w-5 h-5" />
              </div>
              <div>
                <h3 className="text-base font-bold text-white">Your Assessment History</h3>
                <p className="text-xs text-slate-400">Past generated quizzes, attempt scores, and accuracy metrics</p>
              </div>
            </div>

            <button
              onClick={() => setStage('setup')}
              className="px-3 py-1.5 rounded-xl text-xs font-semibold bg-slate-800 hover:bg-slate-700 text-slate-300 border border-slate-700"
            >
              Back to Setup
            </button>
          </div>

          {loadingHistory ? (
            <div className="py-12 flex justify-center">
              <LoadingSpinner text="Loading your quiz history..." />
            </div>
          ) : quizHistory.length === 0 ? (
            <EmptyState
              title="No assessments taken yet"
              description="Complete your first quiz to track your technical scores and improvement over time."
              actionLabel="Start a Quiz"
              onAction={() => setStage('setup')}
            />
          ) : (
            <div className="space-y-3">
              {quizHistory.map((item) => (
                <div
                  key={item.id}
                  className="p-4 rounded-2xl bg-slate-900/60 border border-slate-800 hover:border-slate-700 transition-all flex flex-col sm:flex-row sm:items-center justify-between gap-3"
                >
                  <div className="space-y-1">
                    <div className="flex items-center gap-2">
                      <span className="font-bold text-sm text-white">{item.title}</span>
                      <span className="text-[10px] px-2 py-0.5 rounded bg-brand-500/20 text-brand-300 font-semibold font-mono">
                        {item.mode.replace('_', ' ')}
                      </span>
                      <span className="text-[10px] px-2 py-0.5 rounded bg-slate-800 text-slate-400">
                        {item.difficulty}
                      </span>
                    </div>
                    <p className="text-xs text-slate-400">
                      {item.questionCount} Questions • {new Date(item.createdAt).toLocaleDateString()}
                      {item.documentName ? ` • ${item.documentName}` : ''}
                    </p>
                  </div>

                  <div className="flex items-center gap-4">
                    {item.completed ? (
                      <div className="text-right">
                        <span className="text-[10px] text-slate-500 uppercase block font-bold">Latest Score</span>
                        <span className="text-sm font-bold text-emerald-400">
                          {item.latestScore} / {item.questionCount} ({item.latestPercentage}%)
                        </span>
                      </div>
                    ) : (
                      <span className="text-xs text-amber-400/90 font-medium">Not yet submitted</span>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default Quiz;
