import { API_BASE, PROFILE_API } from '../utils/constants'

interface RequestOptions extends RequestInit {
    headers?: Record<string, string>
}

export const apiCall = async <T = unknown>(endpoint: string, options: RequestOptions = {}): Promise<T> => {
    const response = await fetch(`${API_BASE}${endpoint}`, {
        ...options,
        credentials: 'include', // ⭐ Gửi cookie tự động
        headers: {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            ...options.headers
        }
    })

    if (!response.ok) throw new Error(`API Error: ${response.status}`)
    return response.json()
}

export const profileApiCall = async <T = unknown>(endpoint: string, options: RequestOptions = {}): Promise<T> => {
    const response = await fetch(`${PROFILE_API}${endpoint}`, {
        ...options,
        credentials: 'include', // ⭐ Gửi cookie tự động
        headers: {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            ...options.headers
        }
    })

    if (!response.ok) throw new Error(`API Error: ${response.status}`)
    return response.json()
}
