import { introspectToken } from '../api/authAPI'

// Không thể lấy token từ client-side nữa vì HttpOnly Cookie
// Sử dụng API call để verify authentication status

export const isAuthenticated = async (): Promise<boolean> => {
    try {
        return await introspectToken()
    } catch {
        return false
    }
}

// Để lấy userId, cần endpoint mới hoặc lưu userId riêng
export const getUserId = (): string | null => {
    // Option 1: Lưu userId trong sessionStorage khi login
    return sessionStorage.getItem('userId')
}

export const setUserId = (userId: string): void => {
    sessionStorage.setItem('userId', userId)
}

export const clearUserId = (): void => {
    sessionStorage.removeItem('userId')
}
