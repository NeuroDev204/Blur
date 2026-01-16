/**
 * LocalStorage Service for cookie-based authentication
 * Token is stored in HTTP-only cookie by backend
 * We only store auth flag in localStorage for route protection
 */

// Set authenticated flag (called after successful login)
export const setToken = (token: string): void => {
    // We don't store actual token anymore, just a flag
    localStorage.setItem("token", "authenticated")
}

// Check if authenticated (for route guards)
export const getToken = (): string | null => {
    return localStorage.getItem("token")
}

// Check if user is authenticated
export const isAuthenticated = (): boolean => {
    return localStorage.getItem("token") === "authenticated"
}

// Clear auth state (called on logout)
export const removeToken = (): void => {
    localStorage.removeItem("token")
    localStorage.removeItem("userId")
}

// Store user ID from API response
export const setUserId = (userId: string): void => {
    localStorage.setItem("userId", userId)
}

// Get stored user ID
export const getUserId = (): string | null => {
    return localStorage.getItem("userId")
}
