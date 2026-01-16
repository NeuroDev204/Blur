/**
 * Authentication utilities for cookie-based auth
 * Token is stored in HTTP-only cookie, so we only track auth state via flag
 */

// Check if user has authenticated (flag in localStorage, actual token in cookie)
export const isAuthenticated = (): boolean => {
    return localStorage.getItem("token") === "authenticated"
}

// Legacy function - returns flag for backwards compatibility
export const getToken = (): string | null => localStorage.getItem("token")

// Get user ID from API call (no longer from JWT in localStorage)
// This will be handled by the /users/me API call which has cookie auth
export const getUserId = (): string | null => {
    // Legacy: try to get from localStorage if stored separately
    return localStorage.getItem("userId")
}

// Store user ID when fetched from API
export const setUserId = (userId: string): void => {
    localStorage.setItem("userId", userId)
}

// Clear auth state on logout
export const clearAuth = (): void => {
    localStorage.removeItem("token")
    localStorage.removeItem("userId")
}
