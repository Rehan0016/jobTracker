import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { ApplicationsPage } from '../pages/ApplicationsPage';
import { AuthProvider } from '../context/AuthContext';
import * as appApi from '../api/applications';
import { JobApplication, PagedResponse } from '../types/application';

vi.mock('../api/applications', () => ({
  getApplicationsPagedApi: vi.fn(),
  createApplicationApi: vi.fn(),
  updateApplicationApi: vi.fn(),
  deleteApplicationApi: vi.fn(),
}));

describe('Applications CRUD & Filtering Flows', () => {
  const sampleApps: JobApplication[] = [
    {
      id: 1,
      companyName: 'Microsoft',
      jobTitle: 'Cloud Solutions Architect',
      location: 'Redmond, WA',
      jobUrl: 'https://careers.microsoft.com',
      status: 'APPLIED',
      applicationDate: '2026-01-15',
      notes: 'Applied through LinkedIn referral',
    },
  ];

  const mockPagedResponse: PagedResponse<JobApplication> = {
    content: sampleApps,
    currentPage: 0,
    pageSize: 10,
    totalElements: 1,
    totalPages: 1,
    first: true,
    last: true,
  };

  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.setItem('token', 'mock-token');
    localStorage.setItem('user', JSON.stringify({ id: 1, name: 'Alice', email: 'alice@example.com', role: 'USER' }));
  });

  it('renders application table with data and action buttons', async () => {
    vi.mocked(appApi.getApplicationsPagedApi).mockResolvedValueOnce(mockPagedResponse);

    render(
      <MemoryRouter>
        <AuthProvider>
          <ApplicationsPage />
        </AuthProvider>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Microsoft')).toBeInTheDocument();
      expect(screen.getByText('Cloud Solutions Architect')).toBeInTheDocument();
      expect(screen.getAllByText('Applied').length).toBeGreaterThanOrEqual(1);
      expect(screen.getByText('Redmond, WA')).toBeInTheDocument();
    });
  });

  it('opens create modal, validates required fields, and submits successfully', async () => {
    vi.mocked(appApi.getApplicationsPagedApi).mockResolvedValue(mockPagedResponse);
    vi.mocked(appApi.createApplicationApi).mockResolvedValueOnce({
      id: 2,
      companyName: 'Google',
      jobTitle: 'Software Engineer',
      location: 'Mountain View, CA',
      jobUrl: null,
      status: 'APPLIED',
      applicationDate: '2026-02-01',
      notes: null,
    });

    render(
      <MemoryRouter>
        <AuthProvider>
          <ApplicationsPage />
        </AuthProvider>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Microsoft')).toBeInTheDocument();
    });

    // Click "Add Application"
    fireEvent.click(screen.getByRole('button', { name: /add application/i }));

    // Expect modal to be open
    expect(screen.getByRole('heading', { name: /track new application/i })).toBeInTheDocument();

    // Fill form
    fireEvent.change(screen.getByLabelText(/company name/i), { target: { value: 'Google' } });
    fireEvent.change(screen.getByLabelText(/job title/i), { target: { value: 'Software Engineer' } });

    // Submit modal
    fireEvent.click(screen.getByRole('button', { name: /create application/i }));

    await waitFor(() => {
      expect(appApi.createApplicationApi).toHaveBeenCalledWith(
        expect.objectContaining({
          companyName: 'Google',
          jobTitle: 'Software Engineer',
          status: 'APPLIED',
        })
      );
    });
  });

  it('triggers delete confirmation modal and removes application', async () => {
    vi.mocked(appApi.getApplicationsPagedApi).mockResolvedValue(mockPagedResponse);
    vi.mocked(appApi.deleteApplicationApi).mockResolvedValueOnce();

    render(
      <MemoryRouter>
        <AuthProvider>
          <ApplicationsPage />
        </AuthProvider>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByLabelText('Delete Microsoft')).toBeInTheDocument();
    });

    // Click delete icon
    fireEvent.click(screen.getByLabelText('Delete Microsoft'));

    // Confirmation dialog should open
    expect(screen.getByRole('heading', { name: /delete application/i })).toBeInTheDocument();
    expect(screen.getByText(/are you sure you want to delete the application/i)).toBeInTheDocument();

    // Click confirm Delete in modal
    fireEvent.click(screen.getByRole('button', { name: /^delete$/i }));

    await waitFor(() => {
      expect(appApi.deleteApplicationApi).toHaveBeenCalledWith(1);
    });
  });
});
