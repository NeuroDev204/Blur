import { API_BASE, PROFILE_API } from '../utils/constants'

interface RequestOptions extends RequestInit {
    headers?: Record<string, string>
}

/**
 * Generic API call with cookie-based authentication
 * Credentials are sent automatically via cookies
 */
export const apiCall = async <T = unknown>(endpoint: string, options: RequestOptions = {}): Promise<T> => {
    const response = await fetch(`${API_BASE}${endpoint}`, {
        ...options,
        credentials: 'include', // Cookie tự động được gửi
        headers: {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            ...options.headers
        }
    })

    if (!response.ok) {
        if (response.status === 401) {
            localStorage.removeItem('token')
            window.location.href = '/login'
        }
        throw new Error(`API Error: ${response.status}`)
    }
    return response.json()
}

/**
 * Profile API call with cookie-based authentication
 */
export const profileApiCall = async <T = unknown>(endpoint: string, options: RequestOptions = {}): Promise<T> => {
    const response = await fetch(`${PROFILE_API}${endpoint}`, {
        ...options,
        credentials: 'include', // Cookie tự động được gửi
        headers: {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            ...options.headers
        }
    })

    if (!response.ok) {
        if (response.status === 401) {
            localStorage.removeItem('token')
            window.location.href = '/login'
        }
        throw new Error(`API Error: ${response.status}`)
    }
    return response.json()
}
