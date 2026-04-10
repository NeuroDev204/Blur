import axiosClient from './axiosClient'
import { ApiResponse, RegistrationData } from '../types/api.types'

export const registerUser = async <T = unknown>(data: RegistrationData): Promise<T> => {
    const response = await axiosClient.post<ApiResponse<T>>('/users/registration', data)
    if (response.data?.code !== 1000) {
        throw new Error(response.data?.message || 'Registration failed')
    }
    return response.data?.result as T
}

export const loginUser = async (username: string, password: string): Promise<boolean> => {
    const response = await axiosClient.post<ApiResponse<{ authenticated: boolean }>>('/auth/token', {
        username,
        password,
    })
    if (response.data?.code !== 1000) {
        throw new Error(response.data?.message || 'Login failed')
    }
    // Token is now set in HttpOnly cookie by the server
    // Return authentication status instead of token
    return response.data?.result?.authenticated ?? false
}

export const logoutUser = async (): Promise<void> => {
    // Server sẽ đọc token từ cookie và xóa cookie
    await axiosClient.post('/auth/logout')
}

export const introspectToken = async (): Promise<boolean> => {
    try {
        // Server sẽ đọc token từ cookie
        const response = await axiosClient.post<ApiResponse<{ valid: boolean }>>('/auth/introspect')
        return response.data?.result?.valid ?? false
    } catch {
        // Nếu có lỗi (401, network error, etc.), return false
        return false
    }
}

export const refreshToken = async (): Promise<boolean> => {
    const response = await axiosClient.post<ApiResponse<{ authenticated: boolean }>>('/auth/refresh')
    return response.data?.result?.authenticated ?? false
}
