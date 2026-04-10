import { fetchUserInfo } from "../api/userApi"

interface UserDetails {
    userId?: string
    email?: string
    name?: string
    firstName?: string
    lastName?: string
    imageUrl?: string
    id?: string
    [key: string]: unknown
}

// ⭐ Sử dụng API call thay vì decode JWT từ localStorage
// Token giờ được quản lý bởi HttpOnly Cookie
export const getUserDetails = async (): Promise<UserDetails | null> => {
    try {
        const userInfo = await fetchUserInfo()
        return userInfo as unknown as UserDetails
    } catch (error) {
        return null
    }
}
