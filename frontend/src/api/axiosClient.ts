import axios, { AxiosError, AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios'

const API_BASE_URL = ((import.meta as any).env?.VITE_API_BASE_URL as string) || 'http://localhost:8888/api'

const axiosClient: AxiosInstance = axios.create({
    baseURL: API_BASE_URL,
    timeout: 30000,
    withCredentials: true, // Cookie sẽ tự động được gửi
    headers: {
        'Content-Type': 'application/json',
    },
})

// Request interceptor - no need to add Authorization header, cookie is sent automatically
axiosClient.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
        return config
    },
    (error: AxiosError) => {
        return Promise.reject(error)
    }
)

// Response interceptor
axiosClient.interceptors.response.use(
    (response: AxiosResponse) => {
        return response
    },
    (error: AxiosError<{ message?: string; code?: number }>) => {
        if (error.response) {
            const { status } = error.response
            switch (status) {
                case 401:
                    // Clear auth flag and redirect to login
                    localStorage.removeItem('token')
                    window.location.href = '/login'
                    break
                case 403:
                case 404:
                case 500:
                default:
                    break
            }
        }
        return Promise.reject(error)
    }
)

export default axiosClient
export { API_BASE_URL }
