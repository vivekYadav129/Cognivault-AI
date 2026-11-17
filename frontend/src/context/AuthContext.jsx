import React, { createContext, useContext, useState, useEffect } from 'react';
import { authService } from '../services/authService';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [token, setToken] = useState(() => localStorage.getItem('cognivault_token'));
  const [user, setUser] = useState(() => {
    const saved = localStorage.getItem('cognivault_user');
    try {
      return saved ? JSON.parse(saved) : null;
    } catch {
      return null;
    }
  });
  const [loading, setLoading] = useState(true);

  // Validate or restore user profile on mount
  useEffect(() => {
    const initAuth = async () => {
      const storedToken = localStorage.getItem('cognivault_token');
      if (storedToken) {
        try {
          const currentUser = await authService.getCurrentUser();
          setUser(currentUser);
          localStorage.setItem('cognivault_user', JSON.stringify(currentUser));
        } catch (err) {
          console.warn('Session expired or invalid token', err);
          authService.logout();
          setToken(null);
          setUser(null);
        }
      }
      setLoading(false);
    };

    initAuth();
  }, []);

  const login = async (email, password) => {
    const data = await authService.login(email, password);
    const receivedToken = data.token;
    const receivedUser = data.user;

    localStorage.setItem('cognivault_token', receivedToken);
    localStorage.setItem('cognivault_user', JSON.stringify(receivedUser));

    setToken(receivedToken);
    setUser(receivedUser);
    return receivedUser;
  };

  const register = async (name, email, password) => {
    const data = await authService.register(name, email, password);
    const receivedToken = data.token;
    const receivedUser = data.user;

    localStorage.setItem('cognivault_token', receivedToken);
    localStorage.setItem('cognivault_user', JSON.stringify(receivedUser));

    setToken(receivedToken);
    setUser(receivedUser);
    return receivedUser;
  };

  const logout = () => {
    authService.logout();
    setToken(null);
    setUser(null);
  };

  const refreshUser = async () => {
    try {
      const currentUser = await authService.getCurrentUser();
      setUser(currentUser);
      localStorage.setItem('cognivault_user', JSON.stringify(currentUser));
      return currentUser;
    } catch (err) {
      console.error('Failed to refresh user', err);
      return null;
    }
  };

  const value = {
    user,
    token,
    loading,
    isAuthenticated: Boolean(token && user),
    login,
    register,
    logout,
    refreshUser,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
