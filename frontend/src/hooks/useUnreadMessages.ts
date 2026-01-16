import { useState, useEffect, useCallback } from 'react'
import axios from 'axios'
import { isAuthenticated } from '../service/LocalStorageService'
import { useSocket } from '../contexts/SocketContext'

interface UseUnreadMessagesOptions {
    autoRefresh?: boolean
    refreshInterval?: number
}

interface UnreadByConversation {
    [conversationId: string]: number
}

interface MessageData {
    conversationId: string
    [key: string]: unknown
}

interface UseUnreadMessagesReturn {
    totalUnread: number
    unreadByConversation: UnreadByConversation
    isLoading: boolean
    refreshUnreadCount: () => void
    markAsRead: (conversationId: string) => Promise<void>
}

interface Conversation {
    id: string
    [key: string]: unknown
}

interface ApiResponse<T> {
    result?: T
}

// Axios instance với cookie-based auth
const api = axios.create({
    baseURL: 'http://localhost:8888/api',
    withCredentials: true, // Cookie tự động được gửi
    headers: {
        'Accept': 'application/json',
        'Content-Type': 'application/json'
    }
})

export const useUnreadMessages = (options: UseUnreadMessagesOptions = {}): UseUnreadMessagesReturn => {
    const {
        autoRefresh = true,
        refreshInterval = 30000,
    } = options

    const [totalUnread, setTotalUnread] = useState(0)
    const [unreadByConversation, setUnreadByConversation] = useState<UnreadByConversation>({})
    const [isLoading, setIsLoading] = useState(false)

    const { registerMessageCallbacks } = useSocket()

    const fetchAllUnreadCounts = useCallback(async () => {
        if (!isAuthenticated()) {
            console.warn('⚠️ Not authenticated, cannot fetch unread counts')
            return
        }

        setIsLoading(true)

        try {
            const conversationsRes = await api.get<ApiResponse<Conversation[]>>(
                `/chat/conversations/my-conversations`
            )

            const conversations = conversationsRes.data?.result || []

            if (conversations.length === 0) {
                setTotalUnread(0)
                setUnreadByConversation({})
                setIsLoading(false)
                return
            }

            const unreadPromises = conversations.map(async (conv) => {
                try {
                    const unreadRes = await api.get<ApiResponse<number>>(
                        `/chat/conversations/${conv.id}/unread-count`
                    )
                    return {
                        conversationId: conv.id,
                        count: unreadRes.data?.result || 0,
                    }
                } catch (error) {
                    console.error(`❌ Error fetching unread for ${conv.id}:`, error)
                    return {
                        conversationId: conv.id,
                        count: 0,
                    }
                }
            })

            const results = await Promise.all(unreadPromises)

            let total = 0
            const unreadMap: UnreadByConversation = {}

            results.forEach(({ conversationId, count }) => {
                total += count
                unreadMap[conversationId] = count
            })

            setTotalUnread(total)
            setUnreadByConversation(unreadMap)

            console.log('📊 Unread counts updated:', {
                total,
                byConversation: unreadMap,
            })
        } catch (error) {
            console.error('❌ Error fetching unread counts:', error)
        } finally {
            setIsLoading(false)
        }
    }, [])

    const markAsRead = useCallback(async (conversationId: string) => {
        if (!isAuthenticated() || !conversationId) return

        try {
            await api.put(
                `/chat/conversations/mark-as-read`,
                null,
                { params: { conversationId } }
            )

            console.log(`✅ Marked conversation ${conversationId} as read`)

            setUnreadByConversation(prev => ({
                ...prev,
                [conversationId]: 0,
            }))

            setTotalUnread(prev => Math.max(0, prev - (unreadByConversation[conversationId] || 0)))

            setTimeout(() => {
                fetchAllUnreadCounts()
            }, 500)
        } catch (error) {
            console.error('❌ Error marking as read:', error)
        }
    }, [unreadByConversation, fetchAllUnreadCounts])

    const refreshUnreadCount = useCallback(() => {
        fetchAllUnreadCounts()
    }, [fetchAllUnreadCounts])

    useEffect(() => {
        fetchAllUnreadCounts()
    }, [fetchAllUnreadCounts])

    useEffect(() => {
        if (!autoRefresh) return

        const interval = setInterval(() => {
            fetchAllUnreadCounts()
        }, refreshInterval)

        return () => clearInterval(interval)
    }, [autoRefresh, refreshInterval, fetchAllUnreadCounts])

    useEffect(() => {
        if (!registerMessageCallbacks) return

        const handleMessageReceived = (data: unknown) => {
            const messageData = data as MessageData
            console.log('📨 New message received, updating unread count')

            setUnreadByConversation(prev => ({
                ...prev,
                [messageData.conversationId]: (prev[messageData.conversationId] || 0) + 1,
            }))

            setTotalUnread(prev => prev + 1)

            setTimeout(() => {
                fetchAllUnreadCounts()
            }, 500)
        }

        const handleMessageSent = () => {
            console.log('✅ Message sent, may need to refresh unread count')

            setTimeout(() => {
                fetchAllUnreadCounts()
            }, 500)
        }

        registerMessageCallbacks({
            onMessageReceived: handleMessageReceived,
            onMessageSent: handleMessageSent,
        })
    }, [registerMessageCallbacks, fetchAllUnreadCounts])

    return {
        totalUnread,
        unreadByConversation,
        isLoading,
        refreshUnreadCount,
        markAsRead,
    }
}

export default useUnreadMessages
