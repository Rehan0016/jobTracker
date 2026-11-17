import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { DashboardPage } from '../pages/DashboardPage';
import { AuthProvider } from '../context/AuthContext';
import * as appApi from '../api/applications';
import { DashboardStats, PagedResponse, JobApplication } from '../types/application';

vi.mock('../api/applications', () => ({
  getDashboardStatsApi: vi.fn(),
  getApplicationsPagedApi: vi.fn(),
  getUpcomingInterviewsApi: vi.fn().mockResolvedValue([]),
  createApplicationApi: vi.fn(),
  updateApplicationApi: vi.fn(),
  deleteApplicationApi: vi.fn(),
}));

describe('Frontend Multi-User Data Isolation', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    localStorage.clear();
  });

  it('displays only User A applications when authenticated as User A', async () => {
    localStorage.setItem('token', 'token-user-a');
    localStorage.setItem('user', JSON.stringify({ id: 1, name: 'Alice A', email: 'alice@example.com', role: 'USER' }));

    const statsUserA: DashboardStats = {
      totalApplications: 1,
      applied: 1,
      interviews: 0,
      offers: 0,
      selected: 0,
      rejected: 0,
    };

    const appsUserA: PagedResponse<JobApplication> = {
      content: [
        {
          id: 10,
          companyName: 'Company A Only',
          jobTitle: 'Backend Dev',
          location: 'Remote',
          jobUrl: null,
          status: 'APPLIED',
          applicationDate: '2026-01-10',
          notes: 'Private to User A',
        },
      ],
      currentPage: 0,
      pageSize: 5,
      totalElements: 1,
      totalPages: 1,
      first: true,
      last: true,
    };

    vi.mocked(appApi.getDashboardStatsApi).mockResolvedValue(statsUserA);
    vi.mocked(appApi.getApplicationsPagedApi).mockResolvedValue(appsUserA);

    render(
      <MemoryRouter>
        <AuthProvider>
          <DashboardPage />
        </AuthProvider>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Company A Only')).toBeInTheDocument();
      expect(screen.queryByText('Company B Only')).not.toBeInTheDocument();
      expect(screen.getByText('Total Applications').parentElement).toHaveTextContent('1');
    });
  });

  it('switches to User B data cleanly when authenticated as User B', async () => {
    localStorage.setItem('token', 'token-user-b');
    localStorage.setItem('user', JSON.stringify({ id: 2, name: 'Bob B', email: 'bob@example.com', role: 'USER' }));

    const statsUserB: DashboardStats = {
      totalApplications: 2,
      applied: 0,
      interviews: 2,
      offers: 0,
      selected: 0,
      rejected: 0,
    };

    const appsUserB: PagedResponse<JobApplication> = {
      content: [
        {
          id: 20,
          companyName: 'Company B Only',
          jobTitle: 'Frontend Dev',
          location: 'New York',
          jobUrl: null,
          status: 'INTERVIEW',
          applicationDate: '2026-02-15',
          notes: 'Private to User B',
        },
      ],
      currentPage: 0,
      pageSize: 5,
      totalElements: 2,
      totalPages: 1,
      first: true,
      last: true,
    };

    vi.mocked(appApi.getDashboardStatsApi).mockResolvedValue(statsUserB);
    vi.mocked(appApi.getApplicationsPagedApi).mockResolvedValue(appsUserB);

    render(
      <MemoryRouter>
        <AuthProvider>
          <DashboardPage />
        </AuthProvider>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Company B Only')).toBeInTheDocument();
      expect(screen.queryByText('Company A Only')).not.toBeInTheDocument();
      expect(screen.getByText('Total Applications').parentElement).toHaveTextContent('2');
    });
  });
});
