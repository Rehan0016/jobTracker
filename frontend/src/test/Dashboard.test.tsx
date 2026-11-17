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

describe('Dashboard Page Component', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.setItem('token', 'mock-token');
    localStorage.setItem('user', JSON.stringify({ id: 1, name: 'Alice', email: 'alice@example.com', role: 'USER' }));
  });

  it('renders all KPI cards with accurate values and status distribution', async () => {
    const mockStats: DashboardStats = {
      totalApplications: 12,
      applied: 5,
      interviews: 3,
      offers: 2,
      selected: 1,
      rejected: 1,
    };

    const mockRecentApps: PagedResponse<JobApplication> = {
      content: [
        {
          id: 101,
          companyName: 'Apple',
          jobTitle: 'iOS Engineer',
          location: 'Cupertino, CA',
          jobUrl: 'https://apple.com/jobs',
          status: 'INTERVIEW',
          applicationDate: '2026-02-01',
          notes: 'Second round technical',
        },
      ],
      currentPage: 0,
      pageSize: 5,
      totalElements: 1,
      totalPages: 1,
      first: true,
      last: true,
    };

    vi.mocked(appApi.getDashboardStatsApi).mockResolvedValueOnce(mockStats);
    vi.mocked(appApi.getApplicationsPagedApi).mockResolvedValueOnce(mockRecentApps);

    render(
      <MemoryRouter>
        <AuthProvider>
          <DashboardPage />
        </AuthProvider>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Total Applications')).toBeInTheDocument();
      expect(screen.getByText('12')).toBeInTheDocument();
      expect(screen.getByText('Apple')).toBeInTheDocument();
      expect(screen.getByText('iOS Engineer')).toBeInTheDocument();
      expect(screen.getByText('Application Pipeline')).toBeInTheDocument();
    });
  });
});
