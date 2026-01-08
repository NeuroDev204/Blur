import axiosClient from './axiosClient'
import { ApiResponse, RegistrationData } from '../types/api.types'

export const registerUser = async <T = unknown>(data: RegistrationData): Promise<T> => {
    const response = await axiosClient.post<ApiResponse<T>>('/identity/users/registration', data)
    if (response.data?.code !== 1000) {
        throw new Error(response.data?.message || 'Registration failed')
    }
    return response.data?.result as T
}

export const loginUser = async (username: string, password: string): Promise<string> => {
    const response = await axiosClient.post<ApiResponse<{ token: string }>>('/identity/auth/token', {
        username,
        password,
    })
    if (response.data?.code !== 1000) {
        throw new Error(response.data?.message || 'Login failed')
    }
    return response.data?.result?.token || ''
}

export const logoutUser = async (): Promise<void> => {
    const token = localStorage.getItem('token')
    if (token) {
        await axiosClient.post('/identity/auth/logout', { token })
    }
    localStorage.removeItem('token')
}

export const introspectToken = async (token: string): Promise<boolean> => {
    const response = await axiosClient.post<ApiResponse<{ valid: boolean }>>('/identity/auth/introspect', { token })
    return response.data?.result?.valid ?? false
}
