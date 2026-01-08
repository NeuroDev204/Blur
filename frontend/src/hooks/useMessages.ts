// hooks/useMessages.ts
import { useState, useEffect } from 'react'
import { createChatMessage } from '../api/messageApi'
import httpClient from '../service/httpClient'
import { getToken } from '../service/LocalStorageService'
import { API } from '../service/configuration'
import { AxiosError } from 'axios'

interface Message {
    id: string | number
    sender: 'me' | 'other'
    text: string
    timestamp: string
}

interface MessageResponse {
    id: string
    me: boolean
    message: string
    timestamp?: string
    createdAt?: string
}

interface SelectedChat {
    id: string
    [key: string]: unknown
}

interface UseMessagesReturn {
    messages: Message[]
    loading: boolean
    error: string | null
    addMessage: (message: Message) => void
    sendMessage: (
        conversationId: string,
        messageText: string,
        socketEmit: (event: string, data: unknown) => void
    ) => Promise<boolean>
}

interface ApiResponse {
    result?: MessageResponse[]
}

export const useMessages = (selectedChat: SelectedChat | null): UseMessagesReturn => {
    const [messages, setMessages] = useState<Message[]>([])
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState<string | null>(null)

    const loadMessages = async (conversationId: string) => {
        if (!conversationId) {
            setMessages([])
            return
        }

        try {
            setLoading(true)
            setError(null)

            const token = getToken()
            if (!token) {
                setError("Authentication required")
                return
            }

            const res = await httpClient.get<ApiResponse>(`${API.GET_MESSAGES}`, {
                params: { conversationId },
                headers: { Authorization: `Bearer ${token}` },
            })

            console.log("Messages response:", res)

            const list: Message[] = (res.data?.result || []).map((m) => ({
                id: m.id,
                sender: m.me ? "me" : "other",
                text: m.message,
                timestamp: m.timestamp || m.createdAt || new Date().toISOString()
            }))

            setMessages(list)
        } catch (err) {
            console.error("Error loading messages:", err)
            const axiosError = err as AxiosError<{ message?: string }>

            if (axiosError.response?.status === 403) {
                setError("Access forbidden to messages")
            } else if (axiosError.response?.status === 401) {
                setError("Authentication failed")
            } else {
                setError(`Failed to load messages: ${axiosError.response?.data?.message || axiosError.message}`)
            }
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => {
        if (selectedChat?.id) {
            loadMessages(selectedChat.id)
        }
    }, [selectedChat])

    const addMessage = (message: Message) => {
        setMessages(prev => [...prev, message])
    }

    const sendMessage = async (
        conversationId: string,
        messageText: string,
        socketEmit: (event: string, data: unknown) => void
    ): Promise<boolean> => {
        if (!messageText.trim() || !conversationId) return false

        const newMsg: Message = {
            id: Date.now(),
            sender: "me",
            text: messageText,
            timestamp: new Date().toISOString()
        }

        addMessage(newMsg)

        try {
            const token = getToken()
            if (!token) {
                throw new Error("No authentication token")
            }

            await createChatMessage({
                conversationId,
                message: messageText
            })

            socketEmit("send_message", {
                conversationId,
                message: messageText,
            })

            console.log("Message sent successfully")
            return true
        } catch (err) {
            console.error("Failed to send message:", err)
            const axiosError = err as AxiosError<{ message?: string }>

            setMessages(prev => prev.filter(msg => msg.id !== newMsg.id))

            if (axiosError.response?.status === 403) {
                setError("Access forbidden to send message")
            } else if (axiosError.response?.status === 401) {
                setError("Authentication failed")
            } else {
                setError(`Failed to send message: ${axiosError.response?.data?.message || axiosError.message}`)
            }

            return false
        }
    }

    return {
        messages,
        loading,
        error,
        addMessage,
        sendMessage
    }
}
