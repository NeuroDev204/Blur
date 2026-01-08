import axiosClient from './axiosClient'
import { ApiResponse, Story, StoryData } from '../types/api.types'

export const fetchAllStories = async (): Promise<Story[]> => {
    try {
        const res = await axiosClient.get<ApiResponse<Story[]>>('/stories/all')
        const data = res.data.result || []
        return data.map(story => ({
            ...story,
            id: story.id,
            media: story.mediaUrl,
            username: `${story.firstName} ${story.lastName}`,
        }))
    } catch {
        return []
    }
}

export const fetchMyStories = async (): Promise<Story[]> => {
    try {
        const res = await axiosClient.get<ApiResponse<Story[]>>('/stories/my-stories')
        return res.data.result || []
    } catch {
        return []
    }
}

export const fetchStoriesByUserId = async (userId: string): Promise<Story[]> => {
    try {
        const res = await axiosClient.get<ApiResponse<Story[]>>(`/stories/user/${userId}`)
        return res.data.result || []
    } catch {
        return []
    }
}

export const fetchStoryById = async (storyId: string): Promise<Story | null> => {
    try {
        const res = await axiosClient.get<ApiResponse<Story>>(`/stories/${storyId}`)
        return res.data.result || null
    } catch {
        return null
    }
}

export const createStory = async (storyData: StoryData): Promise<Story | null> => {
    try {
        const res = await axiosClient.post<ApiResponse<Story>>('/stories/create', storyData)
        return res.data.result || null
    } catch {
        return null
    }
}

export const updateStory = async (storyId: string, storyData: Partial<StoryData>): Promise<Story | null> => {
    try {
        const res = await axiosClient.put<ApiResponse<Story>>(`/stories/${storyId}`, storyData)
        return res.data.result || null
    } catch {
        return null
    }
}

export const deleteStory = async (storyId: string): Promise<unknown> => {
    try {
        const res = await axiosClient.delete<ApiResponse<unknown>>(`/stories/${storyId}`)
        return res.data.result
    } catch {
        return null
    }
}

export const likeStory = async (storyId: string): Promise<unknown> => {
    try {
        const res = await axiosClient.put<ApiResponse<unknown>>(`/stories/like/${storyId}`, {})
        return res.data.result
    } catch {
        return null
    }
}

export const unlikeStory = async (storyId: string): Promise<unknown> => {
    try {
        const res = await axiosClient.put<ApiResponse<unknown>>(`/stories/unlike/${storyId}`, {})
        return res.data.result
    } catch {
        return null
    }
}
