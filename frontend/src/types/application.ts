export type ApplicationStatus =
  | 'APPLIED'
  | 'ASSESSMENT'
  | 'INTERVIEW'
  | 'OFFER'
  | 'REJECTED'
  | 'SELECTED'
  | 'WITHDRAWN';

export interface JobApplication {
  id: number;
  companyName: string;
  jobTitle: string;
  location?: string | null;
  jobUrl?: string | null;
  status: ApplicationStatus;
  applicationDate: string;
  notes?: string | null;
  jobDescription?: string | null;
  deadline?: string | null;
}

export interface JobApplicationRequest {
  companyName: string;
  jobTitle: string;
  location?: string | null;
  jobUrl?: string | null;
  status: ApplicationStatus;
  applicationDate?: string | null;
  notes?: string | null;
  jobDescription?: string | null;
  deadline?: string | null;
}

export interface PagedResponse<T> {
  content: T[];
  currentPage: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface DashboardStats {
  totalApplications: number;
  applied: number;
  interviews: number;
  rejected: number;
  selected: number;
  offers: number;
  assessment?: number;
  withdrawn?: number;
  successRate?: number;
}
