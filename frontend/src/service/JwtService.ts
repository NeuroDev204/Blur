import { jwtDecode, JwtPayload } from "jwt-decode"
import { getToken } from "./LocalStorageService"

interface UserDetails extends JwtPayload {
    userId?: string
    email?: string
    name?: string
}

export const getUserDetails = async (): Promise<UserDetails | null> => {
    const token = getToken()
    if (token !== null) {
        try {
            const decode = jwtDecode<UserDetails>(token)
            return decode
        } catch (error) {
            console.log("Invalid token", error)
            return null
        }
    }
    return null
}
