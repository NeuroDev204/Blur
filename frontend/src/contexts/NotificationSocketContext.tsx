import React, { createContext, useContext, useEffect, useRef, useState, ReactNode } from "react"
import { useNotification } from "../contexts/NotificationContext"
import { introspectToken } from "../api/authAPI"

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
}

interface StompClient {
    subscribe: (destination: string, callback: (message: { body: string }) => void) => void
    activate: () => void
    deactivate: () => void
}

const NotificationSocketContext = createContext<StompClient | null>(null)

export const NotificationSocketProvider: React.FC<NotificationSocketProviderProps> = ({ children }) => {
    const stompClientRef = useRef<StompClient | null>(null)
    const [isConnected, setIsConnected] = useState(false)
    const { addNotification } = useNotification()

    useEffect(() => {
        let client: StompClient | null = null
        let isMounted = true

        // Lazy load sockjs-client and @stomp/stompjs to prevent module-level crash
        const initSocket = async () => {
            try {
                // ⭐ Kiểm tra auth bằng introspect thay vì getToken
                const isAuth = await introspectToken()
                if (!isAuth) {
                    console.log("📵 Not authenticated, skipping notification socket connection")
                    return
                }

                console.log("🔌 Loading notification socket libraries...")

                // Dynamic imports to prevent blank page issue
                const [{ Client }, { default: SockJS }] = await Promise.all([
                    import("@stomp/stompjs"),
                    import("sockjs-client")
                ])

                if (!isMounted) return

                // ⭐ SockJS với cookie auth
                // Note: SockJS tự động gửi cookie khi cùng origin
                console.log("👤 Connecting notification socket with cookie auth...")

                const sockJsOptions = {
                    transports: ['websocket', 'xhr-streaming', 'xhr-polling'],
                }

                client = new Client({
                    webSocketFactory: () =>
                        new SockJS(`http://localhost:8082/notification/ws-notification`, null, sockJsOptions) as WebSocket,
                    // ⭐ Không cần Authorization header, cookie sẽ được gửi tự động
                    reconnectDelay: 5000,
                    debug: (str) => console.log("[STOMP]", str),

                    onConnect: () => {
                        console.log("✅ Connected to /ws-notification")
                        setIsConnected(true)

                        // ⭐ Subscribe với userId từ session (server sẽ xác định từ cookie)
                        client?.subscribe(`/user/queue/notification`, (message) => {
                            try {
                                const data: NotificationPayload = JSON.parse(message.body)
                                console.log("🔔 Realtime notification received:", data)

                                addNotification({
                                    id: data.id,
                                    senderName: data.senderName,
                                    message: data.content,
                                    avatar: data.senderImageUrl,
                                    createdDate: data.timestamp,
                                    type: data.type || "general",
                                    postId: data.postId,
                                    seen: false,
                                })
                            } catch (e) {
                                console.error("❌ Failed to parse notification message:", e)
                            }
                        })
                    },

                    onStompError: (frame) => {
                        console.error("❌ STOMP Error:", frame.headers["message"])
                        setIsConnected(false)
                    },

                    onDisconnect: () => {
                        console.log("📴 Disconnected from notification socket")
                        setIsConnected(false)
                    },
                }) as StompClient

                client.activate()
                stompClientRef.current = client

            } catch (error) {
                console.error("❌ Failed to initialize notification socket:", error)
                // Don't crash the app - just log the error
            }
        }

        initSocket()

        return () => {
            isMounted = false
            if (client) {
                try {
                    client.deactivate()
                } catch (e) {
                    console.error("Error deactivating notification socket:", e)
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
