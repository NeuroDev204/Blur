import axiosClient from './axiosClient'
import { ApiResponse, UserProfile, UpdateProfileData } from '../types/api.types'

export const fetchUserInfo = async (): Promise<UserProfile> => {
    const response = await axiosClient.get<ApiResponse<UserProfile>>('/profile/users/myInfo')
    if (response.data?.code !== 1000) {
        throw new Error(response.data?.message || 'Failed to fetch user info')
    }
    return response.data?.result as UserProfile
}

export const fetchUserByUserId = async (userId: string): Promise<UserProfile> => {
    const response = await axiosClient.get<ApiResponse<UserProfile>>(`/profile/internal/users/${userId}`)
    if (response.data?.code !== 1000) {
        throw new Error(response.data?.message || 'Failed to fetch user')
    }
    return response.data?.result as UserProfile
}

export const fetchAllUserProfiles = async (): Promise<UserProfile[]> => {
    const response = await axiosClient.get<ApiResponse<UserProfile[]>>('/profile/users/')
    if (response.data?.code !== 1000) {
        throw new Error(response.data?.message || 'Failed to fetch users')
    }
    return response.data?.result as UserProfile[]
}

export const updateUserProfile = async (userProfileId: string, data: UpdateProfileData): Promise<UserProfile> => {
    const response = await axiosClient.put<ApiResponse<UserProfile>>(`/profile/users/${userProfileId}`, data)
    if (response.data?.code !== 1000) {
        throw new Error(response.data?.message || 'Failed to update profile')
    }
    return response.data?.result as UserProfile
}

export const deleteUserProfile = async (userProfileId: string): Promise<unknown> => {
    const response = await axiosClient.delete<ApiResponse<unknown>>(`/profile/users/${userProfileId}`)
    if (response.data?.code !== 1000) {
        throw new Error(response.data?.message || 'Failed to delete profile')
    }
    return response.data?.result
}

export const followUser = async (userId: string): Promise<unknown> => {
    const response = await axiosClient.put<ApiResponse<unknown>>(`/profile/users/follow/${userId}`, {})
    return response.data?.result
}

export const unfollowUser = async (userId: string): Promise<unknown> => {
    const response = await axiosClient.put<ApiResponse<unknown>>(`/profile/users/unfollow/${userId}`, {})
    return response.data?.result
}

export const searchUsersByFirstName = async (firstName: string): Promise<UserProfile[]> => {
    const response = await axiosClient.get<ApiResponse<UserProfile[]>>(`/profile/users/search/${firstName}`)
    return response.data?.result || []
}

export const searchUsersByUserName = async (userName: string): Promise<UserProfile[]> => {
    const response = await axiosClient.post<ApiResponse<UserProfile[]>>('/profile/users/search', { keyword: userName })
    return response.data?.result || []
}

export const fetchUserProfileById = async (profileId: string): Promise<UserProfile> => {
    const response = await axiosClient.get<ApiResponse<UserProfile>>(`/profile/users/${profileId}`)
    if (response.data?.code !== 1000) {
        throw new Error(response.data?.message || 'Failed to fetch profile')
    }
    return response.data?.result as UserProfile
}

export const getFollowers = async (profileId: string): Promise<UserProfile[]> => {
    const response = await axiosClient.get<ApiResponse<UserProfile[]>>(`/profile/users/follower/${profileId}`)
    return response.data?.result || []
}

export const getFollowings = async (profileId: string): Promise<UserProfile[]> => {
    const response = await axiosClient.get<ApiResponse<UserProfile[]>>(`/profile/users/following/${profileId}`)
    return response.data?.result || []
}
