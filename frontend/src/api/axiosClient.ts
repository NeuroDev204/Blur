import axios, { AxiosError, AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios'

const API_BASE_URL = ((import.meta as any).env?.VITE_API_BASE_URL as string) || 'http://localhost:8888/api'

const axiosClient: AxiosInstance = axios.create({
    baseURL: API_BASE_URL,
    timeout: 30000,
    headers: {
        'Content-Type': 'application/json',
    },
})

const getToken = (): string | null => {
    return localStorage.getItem('token')
}

axiosClient.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
        const token = getToken()
        if (token && config.headers) {
            config.headers.Authorization = `Bearer ${token}`
        }
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
        if (error.response) {
            const { status } = error.response
            switch (status) {
                case 401:
                    localStorage.removeItem('token')
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
