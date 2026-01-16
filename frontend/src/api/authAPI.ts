import axiosClient from './axiosClient'
import { ApiResponse, RegistrationData } from '../types/api.types'
import httpClient from '@/service/httpClient'

export const registerUser = async <T = unknown>(data: RegistrationData): Promise<T> => {
    const response = await axiosClient.post<ApiResponse<T>>('/identity/users/registration', data)
    if (response.data?.code !== 1000) {
        throw new Error(response.data?.message || 'Registration failed')
    }
    return response.data?.result as T
}

export const loginUser = async (username: string, password: string): Promise<string> => {
    const response = await httpClient.post('/identity/auth/login', { username, password });
    // cookie se duoc set tu dong boi browser
    console.log(response.data);

    return response.data;
}

export const logoutUser = async (): Promise<void> => {
    await httpClient.post("/identity/auth/logout");
    // Xóa flag authenticated từ localStorage
    localStorage.removeItem("token");
    // cookie duoc xoa boi server
    window.location.href = "/login";
}

export const introspectToken = async (token: string): Promise<boolean> => {
    try {
        const response = await httpClient.post('/api/auth/introspect');
        return response.data.result?.valid === true;
    } catch {
        return false;
    }
}
