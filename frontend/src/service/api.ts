import { API_BASE, PROFILE_API } from '../utils/constants'
import { getToken } from '../utils/auth'

interface RequestOptions extends RequestInit {
    headers?: Record<string, string>
}

export const apiCall = async <T = unknown>(endpoint: string, options: RequestOptions = {}): Promise<T> => {
    const token = getToken()
    const response = await fetch(`${API_BASE}${endpoint}`, {
        ...options,
        headers: {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`,
            ...options.headers
        }
    })

    if (!response.ok) throw new Error(`API Error: ${response.status}`)
    return response.json()
}

export const profileApiCall = async <T = unknown>(endpoint: string, options: RequestOptions = {}): Promise<T> => {
    const token = getToken()
    const response = await fetch(`${PROFILE_API}${endpoint}`, {
        ...options,
        headers: {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`,
            ...options.headers
        }
    })

    if (!response.ok) throw new Error(`API Error: ${response.status}`)
    return response.json()
}
