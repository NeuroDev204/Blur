import axiosClient from './axiosClient'
import { Conversation, Message, MessageData, PaginatedMessages, ConversationData } from '../types/api.types'

export const getMyConversations = async (): Promise<Conversation[]> => {
    const response = await axiosClient.get<Conversation[]>('/chat/conversations/my-conversations')
    return response.data
}

export const createConversation = async (data: ConversationData): Promise<Conversation> => {
    const response = await axiosClient.post<Conversation>('/chat/conversations/create', {
        type: data.type,
        participantIds: data.participantIds,
        name: data.name,
    })
    return response.data
}

export const createChatMessage = async (conversationId: string, message: string): Promise<Message> => {
    const response = await axiosClient.post<Message>('/chat/messages/create', {
        conversationId,
        message,
    })
    return response.data
}

export const getConversations = async (): Promise<Conversation[]> => {
    const response = await axiosClient.get<Conversation[]>('/chat/conversations')
    return response.data
}

export const getConversationById = async (conversationId: string): Promise<Conversation> => {
    const response = await axiosClient.get<Conversation>(`/chat/conversations/${conversationId}`)
    return response.data
}

export const sendMessage = async (conversationId: string, messageData: MessageData): Promise<Message> => {
    const response = await axiosClient.post<Message>(
        `/chat/conversations/${conversationId}/messages`,
        messageData
    )
    return response.data
}

export const getMessages = async (
    conversationId: string,
    page: number = 0,
    size: number = 50
): Promise<PaginatedMessages> => {
    const response = await axiosClient.get<PaginatedMessages>(
        `/chat/conversations/${conversationId}/messages`,
        { params: { page, size } }
    )
    return response.data
}

export const markConversationAsRead = async (conversationId: string): Promise<Conversation | null> => {
    try {
        const response = await axiosClient.put<Conversation>(
            `/chat/conversations/mark-as-read?conversationId=${conversationId}`,
            {}
        )
        return response.data
    } catch {
        return null
    }
}

export const deleteConversation = async (conversationId: string): Promise<void> => {
    await axiosClient.delete(`/chat/conversations/${conversationId}`)
}

export const markMessageAsRead = async (conversationId: string, messageId: string): Promise<Message | null> => {
    try {
        const response = await axiosClient.put<Message>(
            `/chat/conversations/${conversationId}/messages/${messageId}/read`,
            {}
        )
        return response.data
    } catch {
        return null
    }
}

export const getUnreadCount = async (): Promise<number> => {
    try {
        const response = await axiosClient.get<{ count?: number }>('/chat/unread-count')
        return response.data.count || 0
    } catch {
        return 0
    }
}
