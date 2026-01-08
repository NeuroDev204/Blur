import { useEffect, useRef, useState, MutableRefObject } from 'react'
import { SOCKET_URL } from '../utils/constants'
import { getToken } from '../utils/auth'

// Socket.IO functionality - Window.io is declared elsewhere
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const getIO = (): ((url: string, options: Record<string, unknown>) => SocketType) | undefined => (window as unknown as { io?: (url: string, options: Record<string, unknown>) => SocketType }).io;

interface SocketType {
    on: (event: string, callback: (data?: unknown) => void) => void
    disconnect: () => void
}

interface UseSocketReturn {
    socketRef: MutableRefObject<SocketType | null>
    isConnected: boolean
    error: string
}

export const useSocketHook = (onMessageReceived: (data: unknown) => void): UseSocketReturn => {
    const [isConnected, setIsConnected] = useState(false)
    const [error, setError] = useState("")
    const socketRef = useRef<SocketType | null>(null)

    useEffect(() => {
        const token = getToken()
        if (!token) {
            setError("Vui lòng đăng nhập")
            return
        }

        let isSubscribed = true
        const script = document.createElement('script')
        script.src = 'https://cdn.socket.io/4.5.4/socket.io.min.js'
        script.async = true

        script.onload = () => {
            if (!isSubscribed) return

            const io = getIO()
            if (!io) {
                setError("Socket.IO not loaded")
                return
            }

            const socket = io(SOCKET_URL, {
                query: { token },
                autoConnect: true,
                reconnection: true,
                reconnectionDelay: 1000,
                reconnectionAttempts: 10,
                timeout: 20000,
                transports: ['websocket', 'polling']
            })

            socketRef.current = socket

            socket.on("connect", () => {
                console.log("🟢 Socket connected")
                setIsConnected(true)
                setError("")
            })

            socket.on("disconnect", (reason) => {
                console.log("🔴 Socket disconnected:", reason)
                setIsConnected(false)
            })

            socket.on("connect_error", (err) => {
                console.error("❌ Socket connection error:", err)
                setError("Không thể kết nối")
                setIsConnected(false)
            })

            socket.on("message_received", onMessageReceived)
        }

        script.onerror = () => setError("Không thể tải Socket.IO")
        document.head.appendChild(script)

        return () => {
            isSubscribed = false
            if (socketRef.current) {
                socketRef.current.disconnect()
                socketRef.current = null
            }
            if (script.parentNode) {
                script.parentNode.removeChild(script)
            }
        }
    }, [onMessageReceived])

    return { socketRef, isConnected, error }
}
