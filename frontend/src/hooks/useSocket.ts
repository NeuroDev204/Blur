import { useEffect, useRef, useState, MutableRefObject } from 'react'
import { Client, IMessage } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { WS_URL } from '../utils/constants'

interface UseSocketReturn {
    socketRef: MutableRefObject<Client | null>
    isConnected: boolean
    error: string
}

export const useSocketHook = (onMessageReceived: (data: unknown) => void): UseSocketReturn => {
    const [isConnected, setIsConnected] = useState(false)
    const [error, setError] = useState("")
    const socketRef = useRef<Client | null>(null)

    useEffect(() => {
        let isMounted = true

        const client = new Client({
            webSocketFactory: () =>
                new SockJS(WS_URL, null, {
                    transports: ['websocket', 'xhr-streaming', 'xhr-polling'],
                }) as WebSocket,
            reconnectDelay: 3000,

            onConnect: () => {
                if (!isMounted) return
                setIsConnected(true)
                setError("")

                client.subscribe("/user/queue/chat/message", (message: IMessage) => {
                    onMessageReceived(JSON.parse(message.body))
                })
            },

            onStompError: () => {
                setError("Khong the ket noi")
                setIsConnected(false)
            },

            onDisconnect: () => {
                setIsConnected(false)
            },
        })

        client.activate()
        socketRef.current = client

        return () => {
            isMounted = false
            if (client) {
                client.deactivate()
            }
            socketRef.current = null
        }
    }, [onMessageReceived])

    return { socketRef, isConnected, error }
}
