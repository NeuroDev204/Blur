// src/contexts/SocketContext.tsx
import React, { createContext, useContext, useEffect, useRef, useState, useCallback, useMemo, ReactNode } from "react"
import { Client, IMessage } from "@stomp/stompjs"
import SockJS from "sockjs-client"
import { WS_URL } from "../utils/constants"
import { introspectToken } from "../api/authAPI"

interface MessageCallbacks {
    onMessageSent: ((data: unknown) => void) | null
    onMessageReceived: ((data: unknown) => void) | null
    onMessagesRead: ((data: unknown) => void) | null
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
    socket: Client | null
    isConnected: boolean
    error: string
    sendMessage: (messageData: unknown) => boolean
    sendTypingIndicator: (conversationId: string, isTyping: boolean) => void
    registerMessageCallbacks: (callbacks: Partial<MessageCallbacks>) => () => void
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
    const clientRef = useRef<Client | null>(null)
    const [isConnected, setIsConnected] = useState(false)
    const [error, setError] = useState("")

    const messageCallbacksRef = useRef<Set<Partial<MessageCallbacks>>>(new Set())

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
        messageCallbacksRef.current.add(callbacks)

        return () => {
            messageCallbacksRef.current.delete(callbacks)
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
        let client: Client | null = null
        let isMounted = true

        const initSocket = async () => {
            try {
                const isAuth = await introspectToken()
                if (!isAuth || !isMounted) return

                client = new Client({
                    webSocketFactory: () =>
                        new SockJS(WS_URL, null, {
                            transports: ['websocket', 'xhr-streaming', 'xhr-polling'],
                        }) as WebSocket,
                    reconnectDelay: 3000,

                    onConnect: () => {
                        if (!isMounted) return
                        setIsConnected(true)
                        setError("")

                        // Subscribe to chat messages
                        client?.subscribe("/user/queue/chat/message", (message: IMessage) => {
                            const data = JSON.parse(message.body)
                            if (data?.tempMessageId) {
                                messageCallbacksRef.current.forEach((callbacks) => {
                                    callbacks.onMessageSent?.(data)
                                })
                                return
                            }

                            messageCallbacksRef.current.forEach((callbacks) => {
                                callbacks.onMessageReceived?.(data)
                            })
                        })

                        client?.subscribe("/user/queue/chat/read", (message: IMessage) => {
                            const data = JSON.parse(message.body)
                            messageCallbacksRef.current.forEach((callbacks) => {
                                callbacks.onMessagesRead?.(data)
                            })
                        })

                        // Subscribe to typing indicators
                        client?.subscribe("/user/queue/chat/typing", (message: IMessage) => {
                            // Typing indicator received
                        })

                        // Subscribe to call events
                        client?.subscribe("/user/queue/call", (message: IMessage) => {
                            const data = JSON.parse(message.body) as { event?: string; [key: string]: unknown }
                            const event = data.event
                            switch (event) {
                                case "call:initiated":
                                    callCallbacksRef.current.onCallInitiated?.(data)
                                    break
                                case "call:incoming":
                                    callCallbacksRef.current.onIncomingCall?.(data)
                                    break
                                case "call:answered":
                                    callCallbacksRef.current.onCallAnswered?.(data)
                                    break
                                case "call:rejected":
                                    callCallbacksRef.current.onCallRejected?.(data)
                                    break
                                case "call:ended":
                                    callCallbacksRef.current.onCallEnded?.(data)
                                    break
                                case "call:failed":
                                    callCallbacksRef.current.onCallFailed?.(data)
                                    break
                            }
                        })

                        // Subscribe to WebRTC signaling
                        client?.subscribe("/user/queue/webrtc", (message: IMessage) => {
                            const data = JSON.parse(message.body) as { event?: string; data?: unknown }
                            const event = data.event
                            switch (event) {
                                case "webrtc:offer":
                                    callCallbacksRef.current.onWebRTCOffer?.(data.data)
                                    break
                                case "webrtc:answer":
                                    callCallbacksRef.current.onWebRTCAnswer?.(data.data)
                                    break
                                case "webrtc:ice-candidate":
                                    callCallbacksRef.current.onICECandidate?.(data.data)
                                    break
                            }
                        })
                    },

                    onStompError: (frame) => {
                        setError("WebSocket error")
                        setIsConnected(false)
                    },

                    onDisconnect: () => {
                        setIsConnected(false)
                    },
                })

                client.activate()
                clientRef.current = client
            } catch (err) {
                setError("Khong the ket noi socket")
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
            clientRef.current = null
        }
    }, [])

    const sendMessage = useCallback((messageData: unknown): boolean => {
        if (!clientRef.current || !isConnected) {
            return false
        }

        try {
            clientRef.current.publish({
                destination: "/app/chat.send",
                body: JSON.stringify(messageData),
            })
            return true
        } catch {
            return false
        }
    }, [isConnected])

    const sendTypingIndicator = useCallback((conversationId: string, isTyping: boolean): void => {
        if (!clientRef.current || !isConnected) {
            return
        }

        try {
            clientRef.current.publish({
                destination: "/app/chat.typing",
                body: JSON.stringify({ conversationId, isTyping }),
            })
        } catch {
            // Error sending typing indicator
        }
    }, [isConnected])

    const initiateCall = useCallback((callData: unknown): boolean => {
        if (!clientRef.current || !isConnected) {
            return false
        }

        try {
            clientRef.current.publish({
                destination: "/app/call.initiate",
                body: JSON.stringify(callData),
            })
            return true
        } catch {
            return false
        }
    }, [isConnected])

    const answerCall = useCallback((callId: string): boolean => {
        if (!clientRef.current || !isConnected) return false
        try {
            clientRef.current.publish({
                destination: "/app/call.answer",
                body: JSON.stringify({ callId }),
            })
            return true
        } catch { return false }
    }, [isConnected])

    const rejectCall = useCallback((callId: string): boolean => {
        if (!clientRef.current || !isConnected) return false
        try {
            clientRef.current.publish({
                destination: "/app/call.reject",
                body: JSON.stringify({ callId }),
            })
            return true
        } catch { return false }
    }, [isConnected])

    const endCall = useCallback((callId: string): boolean => {
        if (!clientRef.current || !isConnected) return false
        try {
            clientRef.current.publish({
                destination: "/app/call.end",
                body: JSON.stringify({ callId }),
            })
            return true
        } catch { return false }
    }, [isConnected])

    const sendWebRTCOffer = useCallback((toUserId: string, offer: unknown): boolean => {
        if (!clientRef.current || !isConnected) return false
        try {
            clientRef.current.publish({
                destination: "/app/webrtc.offer",
                body: JSON.stringify({ to: toUserId, offer }),
            })
            return true
        } catch { return false }
    }, [isConnected])

    const sendWebRTCAnswer = useCallback((toUserId: string, answer: unknown): boolean => {
        if (!clientRef.current || !isConnected) return false
        try {
            clientRef.current.publish({
                destination: "/app/webrtc.answer",
                body: JSON.stringify({ to: toUserId, answer }),
            })
            return true
        } catch { return false }
    }, [isConnected])

    const sendICECandidate = useCallback((toUserId: string, candidate: unknown): boolean => {
        if (!clientRef.current || !isConnected) return false
        try {
            clientRef.current.publish({
                destination: "/app/webrtc.ice-candidate",
                body: JSON.stringify({ to: toUserId, candidate }),
            })
            return true
        } catch { return false }
    }, [isConnected])

    const contextValue = useMemo<SocketContextValue>(() => ({
        socket: clientRef.current,
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
