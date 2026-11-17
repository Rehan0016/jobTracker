import { apiClient } from './client';
import {
  ApplicationStatus,
  DashboardStats,
  JobApplication,
  JobApplicationRequest,
  PagedResponse,
} from '../types/application';

export interface PagedQueryParams {
  page?: number;
  size?: number;
  search?: string;
  company?: string;
  title?: string;
  status?: ApplicationStatus;
  sort?: string;
}

export async function getApplicationsApi(params?: {
  status?: ApplicationStatus;
  company?: string;
  title?: string;
  search?: string;
}): Promise<JobApplication[]> {
  const response = await apiClient.get<JobApplication[]>('/applications', { params });
  return response.data;
}

export async function getApplicationsPagedApi(
  params: PagedQueryParams
): Promise<PagedResponse<JobApplication>> {
  const response = await apiClient.get<PagedResponse<JobApplication>>('/applications/paged', {
    params,
  });
  return response.data;
}

export async function getApplicationByIdApi(id: number): Promise<JobApplication> {
  const response = await apiClient.get<JobApplication>(`/applications/${id}`);
  return response.data;
}

export async function createApplicationApi(
  data: JobApplicationRequest
): Promise<JobApplication> {
  const response = await apiClient.post<JobApplication>('/applications', data);
  return response.data;
}

export async function updateApplicationApi(
  id: number,
  data: JobApplicationRequest
): Promise<JobApplication> {
  const response = await apiClient.put<JobApplication>(`/applications/${id}`, data);
  return response.data;
}

export async function deleteApplicationApi(id: number): Promise<void> {
  await apiClient.delete(`/applications/${id}`);
}

export async function getDashboardStatsApi(): Promise<DashboardStats> {
  const response = await apiClient.get<DashboardStats>('/applications/stats');
  return response.data;
}

export async function searchApplicationsApi(query: string): Promise<JobApplication[]> {
  const response = await apiClient.get<JobApplication[]>('/applications/search', {
    params: { query },
  });
  return response.data;
}

export async function getUpcomingInterviewsApi(): Promise<JobApplication[]> {
  const response = await apiClient.get<JobApplication[]>('/applications/upcoming-interviews');
  return response.data;
}
