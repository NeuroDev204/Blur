export const getToken = (): string | null => localStorage.getItem("token")

interface DecodedToken {
    sub?: string
    userId?: string
    user_id?: string
    id?: string
}

export const getUserId = (): string | null => {
    const token = localStorage.getItem("token")
    if (!token) return null

    try {
        const payload = token.split('.')[1]
        const decoded: DecodedToken = JSON.parse(atob(payload))
        return decoded.sub || decoded.userId || decoded.user_id || decoded.id || null
    } catch (error) {
        console.error("Cannot decode token:", error)
        return null
    }
}
