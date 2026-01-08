import axiosClient from './axiosClient'
import { ApiResponse, Notification } from '../types/api.types'

export const getAllNotifications = async (userId: string): Promise<Notification[]> => {
    try {
        const res = await axiosClient.get<ApiResponse<Notification[]>>(`/notification/${userId}`)
        return res.data.result || []
    } catch {
        return []
    }
}

export const markNotificationAsRead = async (notificationId: string): Promise<unknown> => {
    const res = await axiosClient.put(`/notification/markAsRead/${notificationId}`, {})
    return res.data
}

export const markAllNotificationsAsRead = async (): Promise<unknown> => {
    const res = await axiosClient.put('/notification/markAllAsRead', {})
    return res.data
}
