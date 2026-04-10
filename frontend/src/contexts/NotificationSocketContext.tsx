import React, { createContext, useContext, useEffect, useRef, useState, ReactNode } from "react"
import { useNotification } from "../contexts/NotificationContext"
import { introspectToken } from "../api/authAPI"
import { WS_URL } from "../utils/constants"

interface NotificationSocketProviderProps {
    children: ReactNode
}

interface NotificationPayload {
    id: string
    senderName?: string
    content?: string
    senderImageUrl?: string
    timestamp?: string
    type?: string
    postId?: string
    conversationId?: string
    read?: boolean
}

interface StompClient {
    subscribe: (destination: string, callback: (message: { body: string }) => void) => void
    activate: () => void
    deactivate: () => void
}

const NotificationSocketContext = createContext<StompClient | null>(null)

export const NotificationSocketProvider: React.FC<NotificationSocketProviderProps> = ({ children }) => {
    const stompClientRef = useRef<StompClient | null>(null)
    const [, setIsConnected] = useState(false)
    const { addNotification } = useNotification()

    useEffect(() => {
        let client: StompClient | null = null
        let isMounted = true

        const initSocket = async () => {
            try {
                const isAuth = await introspectToken()
                if (!isAuth) {
                    return
                }

                const [{ Client }, { default: SockJS }] = await Promise.all([
                    import("@stomp/stompjs"),
                    import("sockjs-client")
                ])

                if (!isMounted) return

                const sockJsOptions = {
                    transports: ['websocket', 'xhr-streaming', 'xhr-polling'],
                }

                client = new Client({
                    webSocketFactory: () =>
                        new SockJS(WS_URL, null, sockJsOptions) as WebSocket,
                    reconnectDelay: 5000,

                    onConnect: () => {
                        setIsConnected(true)

                        const handleNotificationMessage = (message: { body: string }) => {
                            try {
                                const data: NotificationPayload = JSON.parse(message.body)

                                addNotification({
                                    id: data.id,
                                    senderName: data.senderName,
                                    message: data.content,
                                    avatar: data.senderImageUrl,
                                    createdDate: data.timestamp,
                                    type: data.type || "general",
                                    postId: data.postId,
                                    conversationId: data.conversationId,
                                    read: data.read ?? false,
                                })
                            } catch (e) {
                            }
                        }

                        // Subscribe to both destinations while backend/client paths are being aligned.
                        client?.subscribe(`/user/notification`, handleNotificationMessage)
                        client?.subscribe(`/user/queue/notification`, handleNotificationMessage)
                    },

                    onStompError: (frame) => {
                        setIsConnected(false)
                    },

                    onDisconnect: () => {
                        setIsConnected(false)
                    },
                }) as StompClient

                client.activate()
                stompClientRef.current = client

            } catch (error) {
            }
        }

        initSocket()

        return () => {
            isMounted = false
            if (client) {
                try {
                    client.deactivate()
                } catch (e) {
                }
            }
        }
    }, [addNotification])

    return (
        <NotificationSocketContext.Provider value={stompClientRef.current}>
            {children}
        </NotificationSocketContext.Provider>
    )
}

export const useNotificationSocket = (): StompClient | null =>
    useContext(NotificationSocketContext)
