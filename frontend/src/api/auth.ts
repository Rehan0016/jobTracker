import { apiClient } from './client';
import { AuthResponse, LoginRequest, RegisterRequest } from '../types/auth';

export async function loginApi(data: LoginRequest): Promise<AuthResponse> {
  const response = await apiClient.post<AuthResponse>('/auth/login', data);
  return response.data;
}

export async function registerApi(data: RegisterRequest): Promise<AuthResponse> {
  const response = await apiClient.post<AuthResponse>('/auth/register', data);
  return response.data;
}
