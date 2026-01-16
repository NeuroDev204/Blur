import { Client, IMessage } from "@stomp/stompjs"
import SockJS from "sockjs-client"
import { isAuthenticated } from "./LocalStorageService"

interface NotificationPayload {
    id: string
    type?: string
    message?: string
    [key: string]: unknown
}

let stompClient: Client | null = null

/**
 * Connect to notification WebSocket
 * Cookie-based auth: token is sent via SockJS cookie automatically
 */
export const connectNotificationSocket = (onMessageReceived: (notification: NotificationPayload) => void): void => {
    if (!isAuthenticated()) {
        console.warn("Cannot connect to notification socket: not authenticated")
        return
    }

    stompClient = new Client({
        webSocketFactory: () =>
            new SockJS(
                `/api/notification/ws/ws-notification`,
                null,
                { withCredentials: true } // Cookie tự động được gửi
            ) as WebSocket,
        onConnect: () => {
            console.log("Connected to notification socket")
            stompClient?.subscribe(`/topic/notification`, (message: IMessage) => {
                const notification: NotificationPayload = JSON.parse(message.body)
                onMessageReceived(notification)
            })
        },
        onStompError: (frame) => {
            console.error("STOMP error", frame)
        },
        onDisconnect: () => {
            console.log("Disconnected from notification socket")
        },
    })

    stompClient.activate()
}

export const disconnectNotificationSocket = (): void => {
    if (stompClient && stompClient.connected) {
        stompClient.deactivate()
        console.log("Disconnected from notification socket")
    }
}
