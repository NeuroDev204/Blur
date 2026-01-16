import { jwtDecode, JwtPayload } from "jwt-decode"
import { getToken } from "./LocalStorageService"
import httpClient from "./httpClient"

interface UserDetails {
    userId?: string
    email?: string
    name?: string
}

export const getUserDetails = async (): Promise<UserDetails | null> => {
    try {
        const response = await httpClient.get('/identity/users/me');
        return response.data.result;
    } catch (error) {
        console.log('failed to get user info', error);
        return null;

    }
}
