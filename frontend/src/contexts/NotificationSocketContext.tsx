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

interface StompSubscription {
    unsubscribe: () => void
}

interface StompClient {
    subscribe: (destination: string, callback: (message: { body: string }) => void) => StompSubscription
    activate: () => void
    deactivate: () => void
}

const NotificationSocketContext = createContext<StompClient | null>(null)

export const NotificationSocketProvider: React.FC<NotificationSocketProviderProps> = ({ children }) => {
    const stompClientRef = useRef<StompClient | null>(null)
    const [, setIsConnected] = useState(false)
    const { addNotification } = useNotification()

    useEffect(() => {
        let isMounted = true
        let isInitializing = false

        // Goi khi mount VA sau khi login thanh cong: lan dau truy cap web provider mount
        // truoc khi dang nhap -> introspectToken fail -> phai thu lai khi co auth-login-success,
        // neu khong socket se khong ket noi cho den khi reload trang.
        const initSocket = async () => {
            if (stompClientRef.current || isInitializing) return
            isInitializing = true

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

                let client: StompClient | null = null

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
            } finally {
                isInitializing = false
            }
        }

        initSocket()

        // Sau khi login thanh cong (SPA khong reload trang) -> thu ket noi lai
        const onLoginSuccess = () => initSocket()
        window.addEventListener("auth-login-success", onLoginSuccess)

        return () => {
            isMounted = false
            window.removeEventListener("auth-login-success", onLoginSuccess)
            if (stompClientRef.current) {
                try {
                    stompClientRef.current.deactivate()
                } catch (e) {
                }
            }
            stompClientRef.current = null
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
