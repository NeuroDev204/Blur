// src/contexts/SocketContext.tsx
import React, { createContext, useContext, useEffect, useRef, useState, useCallback, useMemo, ReactNode } from "react"
import { SOCKET_URL } from "../utils/constants"
import { getToken } from "../utils/auth"

// Extend Window interface for Socket.IO
declare global {
    interface Window {
        io: (url: string, options: Record<string, unknown>) => SocketType
    }
}

interface SocketType {
    on: (event: string, callback: (data?: unknown) => void) => void
    off: (event: string) => void
    emit: (event: string, data?: unknown) => void
    disconnect: () => void
}

interface MessageCallbacks {
    onMessageSent: ((data: unknown) => void) | null
    onMessageReceived: ((data: unknown) => void) | null
}

interface CallCallbacks {
    onCallInitiated: ((data: unknown) => void) | null
    onIncomingCall: ((data: unknown) => void) | null
    onCallAnswered: ((data: unknown) => void) | null
    onCallRejected: ((data: unknown) => void) | null
    onCallEnded: ((data: unknown) => void) | null
    onCallFailed: ((data: unknown) => void) | null
    onWebRTCOffer: ((data: unknown) => void) | null
    onWebRTCAnswer: ((data: unknown) => void) | null
    onICECandidate: ((data: unknown) => void) | null
}

interface SocketContextValue {
    socket: SocketType | null
    isConnected: boolean
    error: string
    sendMessage: (messageData: unknown) => boolean
    sendTypingIndicator: (conversationId: string, isTyping: boolean) => void
    registerMessageCallbacks: (callbacks: Partial<MessageCallbacks>) => void
    initiateCall: (callData: unknown) => boolean
    answerCall: (callId: string) => boolean
    rejectCall: (callId: string) => boolean
    endCall: (callId: string) => boolean
    sendWebRTCOffer: (toUserId: string, offer: unknown) => boolean
    sendWebRTCAnswer: (toUserId: string, answer: unknown) => boolean
    sendICECandidate: (toUserId: string, candidate: unknown) => boolean
    registerCallCallbacks: (callbacks: Partial<CallCallbacks>) => void
}

interface SocketProviderProps {
    children: ReactNode
}

const SocketContext = createContext<SocketContextValue | null>(null)

export const SocketProvider: React.FC<SocketProviderProps> = ({ children }) => {
    const socketRef = useRef<SocketType | null>(null)
    const [isConnected, setIsConnected] = useState(false)
    const [error, setError] = useState("")

    const messageCallbacksRef = useRef<MessageCallbacks>({
        onMessageSent: null,
        onMessageReceived: null,
    })

    const callCallbacksRef = useRef<CallCallbacks>({
        onCallInitiated: null,
        onIncomingCall: null,
        onCallAnswered: null,
        onCallRejected: null,
        onCallEnded: null,
        onCallFailed: null,
        onWebRTCOffer: null,
        onWebRTCAnswer: null,
        onICECandidate: null,
    })

    const registerMessageCallbacks = useCallback((callbacks: Partial<MessageCallbacks>) => {
        messageCallbacksRef.current = {
            ...messageCallbacksRef.current,
            ...callbacks
        }
    }, [])

    const registerCallCallbacks = useCallback((callbacks: Partial<CallCallbacks>) => {
        const hasChanged = Object.keys(callbacks).some(key =>
            callCallbacksRef.current[key as keyof CallCallbacks] !== callbacks[key as keyof CallCallbacks]
        )

        if (!hasChanged) {
            return
        }

        callCallbacksRef.current = {
            ...callCallbacksRef.current,
            ...callbacks
        }
    }, [])

    useEffect(() => {
        const token = getToken()
        if (!token) {
            return
        }

        const script = document.createElement("script")
        script.src = "https://cdn.socket.io/4.5.4/socket.io.min.js"
        script.async = true

        script.onload = () => {
            if (socketRef.current) {
                return
            }
            const socket = window.io(SOCKET_URL, {
                query: { token },
                autoConnect: true,
                reconnection: true,
                reconnectionDelay: 1000,
                reconnectionAttempts: 10,
                timeout: 20000,
                transports: ["websocket", "polling"],
            })

            socketRef.current = socket

            socket.on("connect", () => {
                setIsConnected(true)
                setError("")
            })

            socket.on("disconnect", () => {
                setIsConnected(false)
            })

            socket.on("connect_error", () => {
                setError("Không thể kết nối socket")
                setIsConnected(false)
            })

            socket.on("reconnect_attempt", () => {
                // Reconnecting...
            })

            socket.on("reconnect", () => {
                setIsConnected(true)
                setError("")
            })

            socket.on("connected", () => {
                // Connected event received
            })

            // Chat events
            socket.on("message_sent", (data) => {
                if (messageCallbacksRef.current.onMessageSent) {
                    messageCallbacksRef.current.onMessageSent(data)
                }
            })

            socket.on("message_received", (data) => {
                if (messageCallbacksRef.current.onMessageReceived) {
                    messageCallbacksRef.current.onMessageReceived(data)
                }
            })

            socket.on("user_typing", () => {
                // User typing event
            })

            socket.on("message_error", (error) => {
                const err = error as { message?: string }
                setError(err?.message || "Lỗi khi gửi tin nhắn")
            })

            socket.on("auth_error", () => {
                setError("Lỗi xác thực. Vui lòng đăng nhập lại.")
            })

            // Call events
            socket.on("call:initiated", (data) => {
                console.log("✅ Received call:initiated event:", data)
                if (callCallbacksRef.current.onCallInitiated) {
                    callCallbacksRef.current.onCallInitiated(data)
                }
            })

            socket.on("call:incoming", (data) => {
                console.log("📞 Received call:incoming event:", data)
                if (callCallbacksRef.current.onIncomingCall) {
                    callCallbacksRef.current.onIncomingCall(data)
                }
            })

            socket.on("call:answered", (data) => {
                console.log("✅ Received call:answered event:", data)
                if (callCallbacksRef.current.onCallAnswered) {
                    callCallbacksRef.current.onCallAnswered(data)
                }
            })

            socket.on("call:rejected", (data) => {
                if (callCallbacksRef.current.onCallRejected) {
                    callCallbacksRef.current.onCallRejected(data)
                }
            })

            socket.on("call:ended", (data) => {
                if (callCallbacksRef.current.onCallEnded) {
                    callCallbacksRef.current.onCallEnded(data)
                }
            })

            socket.on("call:failed", (data) => {
                if (callCallbacksRef.current.onCallFailed) {
                    callCallbacksRef.current.onCallFailed(data)
                }
            })

            socket.on("webrtc:offer", (data) => {
                if (callCallbacksRef.current.onWebRTCOffer) {
                    callCallbacksRef.current.onWebRTCOffer(data)
                }
            })

            socket.on("webrtc:answer", (data) => {
                if (callCallbacksRef.current.onWebRTCAnswer) {
                    callCallbacksRef.current.onWebRTCAnswer(data)
                }
            })

            socket.on("webrtc:ice-candidate", (data) => {
                if (callCallbacksRef.current.onICECandidate) {
                    callCallbacksRef.current.onICECandidate(data)
                }
            })
        }

        script.onerror = () => {
            setError("Không thể tải thư viện Socket.IO")
        }

        document.head.appendChild(script)

        return () => {
            if (socketRef.current) {
                socketRef.current.off("connect")
                socketRef.current.off("disconnect")
                socketRef.current.off("connect_error")
                socketRef.current.off("reconnect_attempt")
                socketRef.current.off("reconnect")
                socketRef.current.off("connected")
                socketRef.current.off("message_sent")
                socketRef.current.off("message_received")
                socketRef.current.off("user_typing")
                socketRef.current.off("message_error")
                socketRef.current.off("auth_error")
                socketRef.current.off("call:initiated")
                socketRef.current.off("call:incoming")
                socketRef.current.off("call:answered")
                socketRef.current.off("call:rejected")
                socketRef.current.off("call:ended")
                socketRef.current.off("call:failed")
                socketRef.current.off("webrtc:offer")
                socketRef.current.off("webrtc:answer")
                socketRef.current.off("webrtc:ice-candidate")

                socketRef.current.disconnect()
                socketRef.current = null
            }

            if (script.parentNode) {
                script.parentNode.removeChild(script)
            }
        }
    }, [])

    const sendMessage = useCallback((messageData: unknown): boolean => {
        if (!socketRef.current || !isConnected) {
            return false
        }

        try {
            socketRef.current.emit("send_message", messageData)
            return true
        } catch {
            return false
        }
    }, [isConnected])

    const sendTypingIndicator = useCallback((conversationId: string, isTyping: boolean): void => {
        if (!socketRef.current || !isConnected) {
            return
        }

        try {
            socketRef.current.emit("typing", { conversationId, isTyping })
        } catch {
            // Error sending typing indicator
        }
    }, [isConnected])

    const initiateCall = useCallback((callData: unknown): boolean => {
        if (!socketRef.current || !isConnected) {
            console.error("❌ Socket not connected or socketRef is null")
            return false
        }

        try {
            console.log("📡 Emitting call:initiate event:", callData)
            socketRef.current.emit("call:initiate", callData)
            return true
        } catch (error) {
            console.error("❌ Error emitting call:initiate:", error)
            return false
        }
    }, [isConnected])

    const answerCall = useCallback((callId: string): boolean => {
        if (!socketRef.current || !isConnected) {
            return false
        }

        try {
            socketRef.current.emit("call:answer", { callId })
            return true
        } catch {
            return false
        }
    }, [isConnected])

    const rejectCall = useCallback((callId: string): boolean => {
        if (!socketRef.current || !isConnected) {
            return false
        }

        try {
            socketRef.current.emit("call:reject", { callId })
            return true
        } catch {
            return false
        }
    }, [isConnected])

    const endCall = useCallback((callId: string): boolean => {
        if (!socketRef.current || !isConnected) {
            return false
        }

        try {
            socketRef.current.emit("call:end", { callId })
            return true
        } catch {
            return false
        }
    }, [isConnected])

    const sendWebRTCOffer = useCallback((toUserId: string, offer: unknown): boolean => {
        if (!socketRef.current || !isConnected) {
            return false
        }

        try {
            socketRef.current.emit("webrtc:offer", { to: toUserId, offer })
            return true
        } catch {
            return false
        }
    }, [isConnected])

    const sendWebRTCAnswer = useCallback((toUserId: string, answer: unknown): boolean => {
        if (!socketRef.current || !isConnected) {
            return false
        }

        try {
            socketRef.current.emit("webrtc:answer", { to: toUserId, answer })
            return true
        } catch {
            return false
        }
    }, [isConnected])

    const sendICECandidate = useCallback((toUserId: string, candidate: unknown): boolean => {
        if (!socketRef.current || !isConnected) {
            return false
        }

        try {
            socketRef.current.emit("webrtc:ice-candidate", { to: toUserId, candidate })
            return true
        } catch {
            return false
        }
    }, [isConnected])

    const contextValue = useMemo<SocketContextValue>(() => ({
        socket: socketRef.current,
        isConnected,
        error,
        sendMessage,
        sendTypingIndicator,
        registerMessageCallbacks,
        initiateCall,
        answerCall,
        rejectCall,
        endCall,
        sendWebRTCOffer,
        sendWebRTCAnswer,
        sendICECandidate,
        registerCallCallbacks,
    }), [
        isConnected,
        error,
        sendMessage,
        sendTypingIndicator,
        registerMessageCallbacks,
        initiateCall,
        answerCall,
        rejectCall,
        endCall,
        sendWebRTCOffer,
        sendWebRTCAnswer,
        sendICECandidate,
        registerCallCallbacks,
    ])

    return (
        <SocketContext.Provider value={contextValue}>
            {children}
        </SocketContext.Provider>
    )
}

export const useSocket = (): SocketContextValue => {
    const context = useContext(SocketContext)
    if (!context) {
        throw new Error("useSocket must be used within SocketProvider")
    }
    return context
}
