import axios, { AxiosError } from 'axios'

const API_BASE_URL = 'http://localhost:8888/api'

// Types
interface ConversationData {
    type: 'DIRECT' | 'GROUP'
    participantIds: string[]
    name?: string
}

interface MessageData {
    content: string
    type?: string
}

interface Conversation {
    id: string
    type: string
    participants: unknown[]
    [key: string]: unknown
}

interface Message {
    id: string
    content: string
    senderId: string
    timestamp: string
    [key: string]: unknown
}

interface PaginatedMessages {
    content: Message[]
    totalPages: number
    totalElements: number
    [key: string]: unknown
}

interface UnreadCountResponse {
    count?: number
}

/**
 * Create a new conversation
 */
export const createConversation = async (data: ConversationData, token: string): Promise<Conversation> => {
    try {
        const response = await axios.post<Conversation>(
            `${API_BASE_URL}/chat/conversations/create`,
            data,
            {
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                }
            }
        )
        return response.data
    } catch (error) {
        const axiosError = error as AxiosError<{ message?: string }>
        if (axiosError.response) {
            const { status, data } = axiosError.response

            if (status === 400) {
                throw new Error(data?.message || 'Invalid conversation data')
            } else if (status === 401) {
                throw new Error('Unauthorized. Please log in again.')
            } else if (status === 404) {
                throw new Error('User not found')
            } else if (status === 409) {
                return data as unknown as Conversation
            }
        } else if (axiosError.request) {
            throw new Error('Network error. Please check your connection.')
        }

        throw new Error(axiosError.message || 'Failed to create conversation')
    }
}

/**
 * Get all conversations for current user
 */
export const getConversations = async (token: string): Promise<Conversation[]> => {
    try {
        const response = await axios.get<Conversation[]>(
            `${API_BASE_URL}/chat/conversations`,
            {
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            }
        )
        return response.data
    } catch (error) {
        console.error('Error fetching conversations:', error)
        throw error
    }
}

/**
 * Get conversation by ID
 */
export const getConversationById = async (conversationId: string, token: string): Promise<Conversation> => {
    try {
        const response = await axios.get<Conversation>(
            `${API_BASE_URL}/chat/conversations/${conversationId}`,
            {
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            }
        )
        return response.data
    } catch (error) {
        console.error('Error fetching conversation:', error)
        throw error
    }
}

/**
 * Send a message in a conversation
 */
export const sendMessage = async (conversationId: string, messageData: MessageData, token: string): Promise<Message> => {
    try {
        const response = await axios.post<Message>(
            `${API_BASE_URL}/chat/conversations/${conversationId}/messages`,
            messageData,
            {
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                }
            }
        )
        return response.data
    } catch (error) {
        console.error('Error sending message:', error)
        throw error
    }
}

/**
 * Get messages in a conversation with pagination
 */
export const getMessages = async (
    conversationId: string,
    token: string,
    page: number = 0,
    size: number = 50
): Promise<PaginatedMessages> => {
    try {
        const response = await axios.get<PaginatedMessages>(
            `${API_BASE_URL}/chat/conversations/${conversationId}/messages`,
            {
                headers: {
                    'Authorization': `Bearer ${token}`
                },
                params: { page, size }
            }
        )
        return response.data
    } catch (error) {
        console.error('Error fetching messages:', error)
        throw error
    }
}

/**
 * Mark a conversation as read
 */
export const markConversationAsRead = async (conversationId: string, token: string): Promise<Conversation | null> => {
    try {
        const response = await axios.put<Conversation>(
            `${API_BASE_URL}/chat/conversations/mark-as-read?conversationId=${conversationId}`,
            {},
            {
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                }
            }
        )
        return response.data
    } catch (error) {
        console.error('Error marking conversation as read:', error)
        return null
    }
}

/**
 * Delete a conversation
 */
export const deleteConversation = async (conversationId: string, token: string): Promise<void> => {
    try {
        await axios.delete(
            `${API_BASE_URL}/chat/conversations/${conversationId}`,
            {
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            }
        )
    } catch (error) {
        console.error('Error deleting conversation:', error)
        throw error
    }
}

/**
 * Mark a message as read
 */
export const markMessageAsRead = async (
    conversationId: string,
    messageId: string,
    token: string
): Promise<Message | null> => {
    try {
        const response = await axios.put<Message>(
            `${API_BASE_URL}/chat/conversations/${conversationId}/messages/${messageId}/read`,
            {},
            {
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                }
            }
        )
        return response.data
    } catch (error) {
        console.error('Error marking message as read:', error)
        return null
    }
}

/**
 * Get unread message count
 */
export const getUnreadCount = async (token: string): Promise<number> => {
    try {
        const response = await axios.get<UnreadCountResponse>(
            `${API_BASE_URL}/chat/unread-count`,
            {
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            }
        )
        return response.data.count || 0
    } catch (error) {
        console.error('Error fetching unread count:', error)
        return 0
    }
}
