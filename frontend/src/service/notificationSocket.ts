import { Client, IMessage } from "@stomp/stompjs"
import { getToken } from "./LocalStorageService"
import SockJS from "sockjs-client"

interface NotificationPayload {
    id: string
    type?: string
    message?: string
    [key: string]: unknown
}

let stompClient: Client | null = null

export const connectNotificationSocket = (onMessageReceived: (notification: NotificationPayload) => void): void => {
    const token = getToken()

    stompClient = new Client({
        webSocketFactory: () =>
            new SockJS(
                `/api/notification/ws/ws-notification?token=${token}`
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
