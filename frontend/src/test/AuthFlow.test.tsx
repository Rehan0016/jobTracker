import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { LoginPage } from '../pages/LoginPage';
import { RegisterPage } from '../pages/RegisterPage';
import { AuthProvider, useAuth } from '../context/AuthContext';
import * as authApi from '../api/auth';

// Mock auth API module
vi.mock('../api/auth', () => ({
  loginApi: vi.fn(),
  registerApi: vi.fn(),
}));

describe('Frontend Authentication Flows', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
  });

  it('renders login page with email, password fields and sign in button', () => {
    render(
      <MemoryRouter>
        <AuthProvider>
          <LoginPage />
        </AuthProvider>
      </MemoryRouter>
    );

    expect(screen.getByRole('heading', { name: /welcome back/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/email address/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /sign in/i })).toBeInTheDocument();
  });

  it('displays client error if submitted with empty fields', async () => {
    render(
      <MemoryRouter>
        <AuthProvider>
          <LoginPage />
        </AuthProvider>
      </MemoryRouter>
    );

    const submitBtn = screen.getByRole('button', { name: /sign in/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText(/please provide both email and password/i)).toBeInTheDocument();
    });
  });

  it('successful login saves token to localStorage and logs user in', async () => {
    const mockAuthResponse = {
      token: 'jwt.token.mock.123',
      tokenType: 'Bearer',
      id: 1,
      name: 'Alice Developer',
      email: 'alice@example.com',
      role: 'USER' as const,
    };

    vi.mocked(authApi.loginApi).mockResolvedValueOnce(mockAuthResponse);

    render(
      <MemoryRouter>
        <AuthProvider>
          <LoginPage />
        </AuthProvider>
      </MemoryRouter>
    );

    fireEvent.change(screen.getByLabelText(/email address/i), {
      target: { value: 'alice@example.com' },
    });
    fireEvent.change(screen.getByLabelText(/password/i), {
      target: { value: 'password123' },
    });

    fireEvent.click(screen.getByRole('button', { name: /sign in/i }));

    await waitFor(() => {
      expect(authApi.loginApi).toHaveBeenCalledWith({
        email: 'alice@example.com',
        password: 'password123',
      });
      expect(localStorage.getItem('token')).toBe('jwt.token.mock.123');
      expect(localStorage.getItem('user')).toContain('alice@example.com');
    });
  });

  it('renders registration page and validates password minimum length', async () => {
    render(
      <MemoryRouter>
        <AuthProvider>
          <RegisterPage />
        </AuthProvider>
      </MemoryRouter>
    );

    expect(screen.getByRole('heading', { name: /create your account/i })).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText(/full name/i), { target: { value: 'Bob Smith' } });
    fireEvent.change(screen.getByLabelText(/email address/i), { target: { value: 'bob@example.com' } });
    fireEvent.change(screen.getByLabelText(/password/i), { target: { value: '123' } });

    fireEvent.click(screen.getByRole('button', { name: /create account/i }));

    await waitFor(() => {
      expect(screen.getByText(/password must be at least 6 characters/i)).toBeInTheDocument();
    });
  });

  it('logout clears localStorage and resets auth state', async () => {
    localStorage.setItem('token', 'existing.token');
    localStorage.setItem('user', JSON.stringify({ id: 1, name: 'Alice', email: 'alice@example.com', role: 'USER' }));

    const TestLogoutComponent = () => {
      const { user, isAuthenticated, logout } = useAuth();
      return (
        <div>
          <span data-testid="auth-status">{isAuthenticated ? 'logged-in' : 'logged-out'}</span>
          <span data-testid="user-email">{user?.email}</span>
          <button onClick={logout}>Sign Out</button>
        </div>
      );
    };

    render(
      <MemoryRouter>
        <AuthProvider>
          <TestLogoutComponent />
        </AuthProvider>
      </MemoryRouter>
    );

    expect(screen.getByTestId('auth-status')).toHaveTextContent('logged-in');
    expect(screen.getByTestId('user-email')).toHaveTextContent('alice@example.com');

    fireEvent.click(screen.getByText('Sign Out'));

    expect(screen.getByTestId('auth-status')).toHaveTextContent('logged-out');
    expect(localStorage.getItem('token')).toBeNull();
    expect(localStorage.getItem('user')).toBeNull();
  });
});
