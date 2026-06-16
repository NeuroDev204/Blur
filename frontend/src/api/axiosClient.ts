import axios, { AxiosError, AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

const axiosClient: AxiosInstance = axios.create({
    baseURL: API_BASE_URL,
    timeout: 30000,
    headers: {
        'Content-Type': 'application/json',
    },
    withCredentials: true,
})

let isRefreshing = false
let refreshQueue: Array<{ resolve: () => void; reject: (err: unknown) => void }> = []

axiosClient.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => config,
    (error: AxiosError) => Promise.reject(error)
)

axiosClient.interceptors.response.use(
    (response: AxiosResponse) => response,
    async (error: AxiosError<{ message?: string; code?: number }>) => {
        const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean }

        if (error.response?.status === 401 && !originalRequest._retry) {
            // Refresh endpoint itself returning 401 means session is truly expired
            if (originalRequest.url?.includes('/auth/')) {
                return Promise.reject(error)
            }

            originalRequest._retry = true

            if (isRefreshing) {
                return new Promise((resolve, reject) => {
                    refreshQueue.push({
                        resolve: () => resolve(axiosClient(originalRequest)),
                        reject,
                    })
                })
            }

            isRefreshing = true
            try {
                await axiosClient.post('/auth/refresh')
                refreshQueue.forEach(({ resolve }) => resolve())
                refreshQueue = []
                return axiosClient(originalRequest)
            } catch {
                refreshQueue.forEach(({ reject: rej }) => rej(new Error('Session expired')))
                refreshQueue = []
                return Promise.reject(error)
            } finally {
                isRefreshing = false
            }
        }

        return Promise.reject(error)
    }
)

export default axiosClient
export { API_BASE_URL }
