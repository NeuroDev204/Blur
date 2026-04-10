import { Client, IMessage } from "@stomp/stompjs"
import SockJS from "sockjs-client"
import { WS_URL } from "../utils/constants"

interface NotificationPayload {
    id: string
    type?: string
    message?: string
    conversationId?: string
    [key: string]: unknown
}

let stompClient: Client | null = null

export const connectNotificationSocket = (onMessageReceived: (notification: NotificationPayload) => void): void => {
    stompClient = new Client({
        webSocketFactory: () =>
            new SockJS(WS_URL, null, {
                transports: ['websocket', 'xhr-streaming', 'xhr-polling'],
            }) as WebSocket,
        onConnect: () => {
            const handleMessage = (message: IMessage) => {
                const notification: NotificationPayload = JSON.parse(message.body)
                onMessageReceived(notification)
            }

            stompClient?.subscribe(`/user/notification`, handleMessage)
            stompClient?.subscribe(`/user/queue/notification`, handleMessage)
        },
        onStompError: (frame) => {
        },
        onDisconnect: () => {
        },
    })

    stompClient.activate()
}

export const disconnectNotificationSocket = (): void => {
    if (stompClient) {
        stompClient.deactivate()
    }
}
