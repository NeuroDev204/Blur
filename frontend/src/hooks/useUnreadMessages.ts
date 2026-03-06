import { useState, useEffect, useCallback } from 'react'
import axiosClient from '../api/axiosClient'
import { useSocket } from '../contexts/SocketContext'
import { getUserId } from '../utils/auth'

interface UseUnreadMessagesOptions {
    autoRefresh?: boolean
    refreshInterval?: number
}

interface UnreadByConversation {
    [conversationId: string]: number
}

interface MessageData {
    conversationId: string
    senderId?: string
    sender?: {
        userId?: string
    }
    readByUserId?: string
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

export const useUnreadMessages = (options: UseUnreadMessagesOptions = {}): UseUnreadMessagesReturn => {
    const {
        autoRefresh = true,
        refreshInterval = 30000,
    } = options

    const [totalUnread, setTotalUnread] = useState(0)
    const [unreadByConversation, setUnreadByConversation] = useState<UnreadByConversation>({})
    const [isLoading, setIsLoading] = useState(false)

    const { registerMessageCallbacks } = useSocket()
    const currentUserId = getUserId()

    const fetchAllUnreadCounts = useCallback(async () => {
        setIsLoading(true)

        try {
            // ⭐ Sử dụng axiosClient (withCredentials: true) thay vì manual token
            const conversationsRes = await axiosClient.get<ApiResponse<Conversation[]>>(
                '/chat/conversations/my-conversations'
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
                    const unreadRes = await axiosClient.get<ApiResponse<number>>(
                        `/chat/conversations/${conv.id}/unread-count`
                    )
                    return {
                        conversationId: conv.id,
                        count: unreadRes.data?.result || 0,
                    }
                } catch (error) {
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

        } catch (error) {
        } finally {
            setIsLoading(false)
        }
    }, [])

    const markAsRead = useCallback(async (conversationId: string) => {
        if (!conversationId) return

        try {
            await axiosClient.put(
                '/chat/conversations/mark-as-read',
                null,
                {
                    params: { conversationId },
                }
            )


            setUnreadByConversation(prev => ({
                ...prev,
                [conversationId]: 0,
            }))

            setTotalUnread(prev => Math.max(0, prev - (unreadByConversation[conversationId] || 0)))

            setTimeout(() => {
                fetchAllUnreadCounts()
            }, 500)
        } catch (error) {
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
            const senderId = messageData.senderId || messageData.sender?.userId

            if (senderId && senderId === currentUserId) {
                return
            }


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

            setTimeout(() => {
                fetchAllUnreadCounts()
            }, 500)
        }

        const handleMessagesRead = (data: unknown) => {
            const messageData = data as MessageData
            if (!messageData.conversationId) {
                return
            }

            if (messageData.readByUserId === currentUserId) {
                setUnreadByConversation(prev => ({
                    ...prev,
                    [messageData.conversationId]: 0,
                }))

                setTimeout(() => {
                    fetchAllUnreadCounts()
                }, 300)
            }
        }

        const unregister = registerMessageCallbacks({
            onMessageReceived: handleMessageReceived,
            onMessageSent: handleMessageSent,
            onMessagesRead: handleMessagesRead,
        })
        return unregister
    }, [currentUserId, registerMessageCallbacks, fetchAllUnreadCounts])

    return {
        totalUnread,
        unreadByConversation,
        isLoading,
        refreshUnreadCount,
        markAsRead,
    }
}

export default useUnreadMessages
