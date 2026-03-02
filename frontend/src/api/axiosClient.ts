import axios, { AxiosError, AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios'

const API_BASE_URL = ((import.meta as any).env?.VITE_API_BASE_URL as string) || 'http://localhost:8888/api'

const axiosClient: AxiosInstance = axios.create({
    baseURL: API_BASE_URL,
    timeout: 30000,
    headers: {
        'Content-Type': 'application/json',
    },
    withCredentials: true, // ⭐ QUAN TRỌNG: Cho phép gửi/nhận cookies
})

// Không cần interceptor để gắn token vào header nữa
// Browser sẽ tự động gửi HttpOnly Cookie

axiosClient.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
        // Không cần gắn Authorization header nữa
        // Cookie sẽ được browser tự động gửi với withCredentials: true
        return config
    },
    (error: AxiosError) => {
        return Promise.reject(error)
    }
)

axiosClient.interceptors.response.use(
    (response: AxiosResponse) => {
        return response
    },
    (error: AxiosError<{ message?: string; code?: number }>) => {
        // ⭐ KHÔNG tự động redirect về login khi 401
        // Để Router xử lý authentication check
        // Chỉ reject error để caller xử lý
        return Promise.reject(error)
    }
)

export default axiosClient
export { API_BASE_URL }
