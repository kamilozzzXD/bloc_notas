import apiClient from './apiClient';

export interface AuthRequest {
  username: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  tipo: string;
  username: string;
}

export interface MensajeResponse {
  mensaje: string;
}

/**
 * Llama a POST /api/auth/login y devuelve el token JWT.
 */
export async function login(data: AuthRequest): Promise<AuthResponse> {
  const response = await apiClient.post<AuthResponse>('/api/auth/login', data);
  return response.data;
}

/**
 * Llama a POST /api/auth/register y devuelve el mensaje de confirmación.
 */
export async function register(data: AuthRequest): Promise<MensajeResponse> {
  const response = await apiClient.post<MensajeResponse>('/api/auth/register', data);
  return response.data;
}
