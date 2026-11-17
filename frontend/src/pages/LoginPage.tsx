import React, { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { Mail, Lock, Sparkles, ArrowRight } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { Input } from '../components/common/Input';
import { Button } from '../components/common/Button';
import { getErrorMessage } from '../api/client';

export const LoginPage: React.FC = () => {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const from = (location.state as { from?: { pathname: string } })?.from?.pathname || '/dashboard';

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!email.trim() || !password.trim()) {
      setError('Please provide both email and password.');
      return;
    }

    setIsLoading(true);
    try {
      await login({ email: email.trim(), password });
      navigate(from, { replace: true });
    } catch (err: unknown) {
      setError(getErrorMessage(err));
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-50 p-4 sm:p-6 lg:p-8">
      <div className="w-full max-w-md">
        {/* Header Branding */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-12 h-12 rounded-2xl bg-gradient-to-tr from-brand-600 to-indigo-500 text-white shadow-lg shadow-brand-500/25 mb-4">
            <Sparkles className="w-6 h-6" />
          </div>
          <h2 className="text-2xl font-bold text-slate-900 tracking-tight">Welcome back</h2>
          <p className="text-sm text-slate-500 mt-1.5">Sign in to manage and track your job applications</p>
        </div>

        {/* Card */}
        <div className="bg-white rounded-2xl shadow-xl shadow-slate-200/50 border border-slate-200/80 p-8">
          <form onSubmit={handleSubmit} noValidate className="space-y-4">
            {error && (
              <div className="p-3.5 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-xs font-medium">
                {error}
              </div>
            )}

            <Input
              label="Email Address"
              type="email"
              required
              placeholder="you@example.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              leftIcon={<Mail className="w-4 h-4" />}
            />

            <Input
              label="Password"
              type="password"
              required
              placeholder="••••••••"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              leftIcon={<Lock className="w-4 h-4" />}
            />

            <div className="pt-2">
              <Button
                type="submit"
                variant="primary"
                size="lg"
                isLoading={isLoading}
                className="w-full"
                rightIcon={<ArrowRight className="w-4 h-4" />}
              >
                Sign In
              </Button>
            </div>
          </form>

          {/* Quick Demo Fill */}
          <div className="mt-6 pt-6 border-t border-slate-100 text-center">
            <p className="text-xs text-slate-400 mb-2 font-medium">Testing locally?</p>
            <button
              type="button"
              onClick={() => {
                setEmail('alice@example.com');
                setPassword('password123');
              }}
              className="text-xs text-brand-600 hover:text-brand-700 font-semibold cursor-pointer underline"
            >
              Fill demo credentials (alice@example.com)
            </button>
          </div>
        </div>

        {/* Registration footer link */}
        <p className="text-center text-sm text-slate-500 mt-6">
          Don't have an account?{' '}
          <Link to="/register" className="font-semibold text-brand-600 hover:text-brand-700 transition-colors">
            Create an account
          </Link>
        </p>
      </div>
    </div>
  );
};
