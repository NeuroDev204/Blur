// src/hooks/useCall.ts
import { useState, useEffect, useRef, useCallback, RefObject } from 'react'
import { useSocket } from '../contexts/SocketContext'
import webRTCService from '../service/WebRTCService'

// ============ TYPES ============
interface CallState {
    isInCall: boolean
    isIncoming: boolean
    callId: string | null
    callerId: string | null
    callerName: string | null
    callerAvatar: string | null
    receiverId: string | null
    receiverName: string | null
    receiverAvatar: string | null
    callType: 'VOICE' | 'VIDEO' | null
    status: string | null
}

interface MediaState {
    isAudioEnabled: boolean
    isVideoEnabled: boolean
}

interface CallEndedInfo {
    callerName: string
    callerAvatar: string | null
    callType: 'VOICE' | 'VIDEO' | null
    duration: number
    endReason: string
    errorMessage?: string
}

interface ReceiverData {
    userId: string
    name: string
    avatar?: string
    currentUserName?: string
    currentUserAvatar?: string
    conversationId?: string
}

interface PendingCallInfo {
    receiverId: string
    receiverName: string
    receiverAvatar?: string
    callType: 'VOICE' | 'VIDEO'
}

interface CallEventData {
    callId?: string
    callerId?: string
    callerName?: string
    callerAvatar?: string
    callType?: 'VOICE' | 'VIDEO'
    duration?: number
    reason?: string
    from?: string
    offer?: RTCSessionDescriptionInit
    answer?: RTCSessionDescriptionInit
    candidate?: RTCIceCandidateInit
}

// ============ CONSTANTS ============
const INITIAL_CALL_STATE: CallState = {
    isInCall: false,
    isIncoming: false,
    callId: null,
    callerId: null,
    callerName: null,
    callerAvatar: null,
    receiverId: null,
    receiverName: null,
    receiverAvatar: null,
    callType: null,
    status: null,
}

const INITIAL_MEDIA_STATE: MediaState = {
    isAudioEnabled: true,
    isVideoEnabled: true,
}

// ============ CALL STATUS ============
export const CALL_STATUS = {
    INITIATING: 'INITIATING',
    RINGING: 'RINGING',
    ANSWERED: 'ANSWERED',
    CONNECTED: 'CONNECTED',
    DISCONNECTED: 'DISCONNECTED',
    ENDED: 'ENDED'
} as const

// ============ RINGTONE MANAGER (Singleton) ============
class RingtoneManager {
    private audio: HTMLAudioElement | null = null
    private isPlaying: boolean = false

    play(): void {
        if (this.isPlaying) {
            return
        }

        try {
            this.stop()

            this.audio = new Audio('/ringtone.mp3')
            this.audio.loop = true
            this.audio.volume = 0.5

            const playPromise = this.audio.play()

            if (playPromise !== undefined) {
                playPromise
                    .then(() => {
                        this.isPlaying = true
                    })
                    .catch(() => {
                        const playOnInteraction = () => {
                            if (this.audio) {
                                this.audio.play()
                                    .then(() => {
                                        this.isPlaying = true
                                    })
                                    .catch(() => { })
                            }
                            document.removeEventListener('click', playOnInteraction)
                            document.removeEventListener('touchstart', playOnInteraction)
                        }

                        document.addEventListener('click', playOnInteraction, { once: true })
                        document.addEventListener('touchstart', playOnInteraction, { once: true })
                    })
            }
        } catch {
            // Error playing ringtone
        }
    }

    stop(): void {
        if (!this.audio) return

        try {
            this.audio.pause()
            this.audio.currentTime = 0
            this.audio.src = ''
            this.audio = null
            this.isPlaying = false
        } catch {
            // Error stopping ringtone
        }
    }

    destroy(): void {
        this.stop()
    }
}

// ============ MAIN HOOK ============
export const useCall = (currentUserId: string) => {
    const socketAPI = useSocket()

    const [callState, setCallState] = useState<CallState>(INITIAL_CALL_STATE)
    const [mediaState, setMediaState] = useState<MediaState>(INITIAL_MEDIA_STATE)
    const [connectionState, setConnectionState] = useState<string>('new')
    const [callDuration, setCallDuration] = useState<number>(0)
    const [callEndedInfo, setCallEndedInfo] = useState<CallEndedInfo | null>(null)

    // ============ REFS (Stable References) ============
    const localVideoRef = useRef<HTMLVideoElement>(null)
    const remoteVideoRef = useRef<HTMLVideoElement>(null)
    const callTimerRef = useRef<ReturnType<typeof setInterval> | ReturnType<typeof setTimeout> | null>(null)
    const remoteUserIdRef = useRef<string | null>(null)
    const ringtoneManagerRef = useRef<RingtoneManager | null>(null)
    const callStateRef = useRef<CallState>(callState)
    const isCleaningUpRef = useRef<boolean>(false)
    const hasAnsweredRef = useRef<boolean>(false)
    const pendingCallInfoRef = useRef<PendingCallInfo | null>(null)

    // ============ SYNC STATE TO REF ============
    useEffect(() => {
        callStateRef.current = callState
    }, [callState])

    // ============ INITIALIZE RINGTONE MANAGER ============
    useEffect(() => {
        ringtoneManagerRef.current = new RingtoneManager()
        return () => {
            ringtoneManagerRef.current?.destroy()
        }
    }, [])

    // ============ STABLE CALLBACKS (No Dependencies) ============
    const startCallTimer = useCallback(() => {
        if (callTimerRef.current) {
            clearInterval(callTimerRef.current)
        }

        setCallDuration(0)
        callTimerRef.current = setInterval(() => {
            setCallDuration(prev => prev + 1)
        }, 1000)
    }, [])

    const stopCallTimer = useCallback(() => {
        if (callTimerRef.current) {
            clearInterval(callTimerRef.current)
            callTimerRef.current = null
        }
    }, [])

    const cleanup = useCallback(() => {
        if (isCleaningUpRef.current) {
            return
        }

        isCleaningUpRef.current = true

        ringtoneManagerRef.current?.stop()

        const currentState = callStateRef.current
        if (currentState.callId && socketAPI.isConnected) {
            socketAPI.endCall(currentState.callId)
        }

        webRTCService.cleanup()
        stopCallTimer()

        remoteUserIdRef.current = null
        hasAnsweredRef.current = false
        pendingCallInfoRef.current = null

        setCallState(INITIAL_CALL_STATE)
        setCallDuration(0)
        setConnectionState('new')
        setMediaState(INITIAL_MEDIA_STATE)

        setTimeout(() => {
            isCleaningUpRef.current = false
        }, 100)
    }, [stopCallTimer, socketAPI])

    const verifyAndEnableAudio = useCallback((): boolean => {
        const stream = webRTCService.localStream

        if (!stream) {
            return false
        }

        const audioTracks = stream.getAudioTracks()

        if (audioTracks.length === 0) {
            return false
        }

        let allEnabled = true
        audioTracks.forEach((track) => {
            track.enabled = true

            if (track.readyState !== 'live') {
                allEnabled = false
            }
        })

        setMediaState(prev => ({
            ...prev,
            isAudioEnabled: true
        }))

        return allEnabled
    }, [])

    // ============ SETUP PEER CONNECTION ============
    const setupPeerConnection = useCallback((remoteUserId: string) => {
        webRTCService.createPeerConnection(
            // ICE Candidate Callback
            (candidate) => {
                socketAPI.sendICECandidate(remoteUserId, candidate)
            },

            // Remote Stream Callback
            (stream) => {
                if (remoteVideoRef.current) {
                    remoteVideoRef.current.srcObject = stream
                    remoteVideoRef.current.muted = false
                    remoteVideoRef.current.volume = 1.0

                    remoteVideoRef.current.play()
                        .then(() => { })
                        .catch(() => { })
                }
            },

            // Connection State Callback
            (state) => {
                setConnectionState(state)

                if (state === 'connected') {
                    setCallState(prev => {
                        if (prev.status !== CALL_STATUS.CONNECTED) {
                            return {
                                ...prev,
                                status: CALL_STATUS.CONNECTED
                            }
                        }
                        return prev
                    })

                    ringtoneManagerRef.current?.stop()
                    startCallTimer()

                    setTimeout(() => {
                        verifyAndEnableAudio()
                    }, 500)

                }
            }
        )
    }, [socketAPI, startCallTimer, verifyAndEnableAudio])

    // ============ CALL ACTIONS ============
    const initiateCall = useCallback(async (receiverData: ReceiverData, callType: 'VOICE' | 'VIDEO') => {
        try {
            const stream = await webRTCService.initializeMedia(callType === 'VIDEO')

            if (localVideoRef.current) {
                localVideoRef.current.srcObject = stream
                localVideoRef.current.muted = true
            }

            remoteUserIdRef.current = receiverData.userId

            pendingCallInfoRef.current = {
                receiverId: receiverData.userId,
                receiverName: receiverData.name,
                receiverAvatar: receiverData.avatar,
                callType: callType
            }

            const success = socketAPI.initiateCall({
                callerId: currentUserId,
                callerName: receiverData.currentUserName || 'Current User',
                callerAvatar: receiverData.currentUserAvatar || null,
                receiverId: receiverData.userId,
                receiverName: receiverData.name,
                receiverAvatar: receiverData.avatar,
                callType,
                conversationId: receiverData.conversationId
            })

            if (!success) {
                throw new Error('Socket connection not ready. Please try again.')
            }

            setCallState({
                isInCall: true,
                isIncoming: false,
                callId: null,
                callerId: currentUserId,
                callerName: null,
                callerAvatar: null,
                receiverId: receiverData.userId,
                receiverName: receiverData.name,
                receiverAvatar: receiverData.avatar || null,
                callType,
                status: CALL_STATUS.INITIATING
            })

            verifyAndEnableAudio()

            const initiationTimeout = setTimeout(() => {
                if (callStateRef.current.status === CALL_STATUS.INITIATING) {
                    const callInfo: Partial<PendingCallInfo> = pendingCallInfoRef.current || {}
                    setCallEndedInfo({
                        callerName: callInfo.receiverName || 'Người nhận',
                        callerAvatar: callInfo.receiverAvatar || null,
                        callType: callInfo.callType || callType,
                        duration: 0,
                        endReason: 'FAILED',
                        errorMessage: 'Không thể kết nối tới người dùng'
                    })
                    cleanup()
                }
            }, 30000)

            callTimerRef.current = initiationTimeout

        } catch (error) {
            const err = error as Error
            const callInfo: Partial<PendingCallInfo> = pendingCallInfoRef.current || {}
            setCallEndedInfo({
                callerName: callInfo.receiverName || 'Người nhận',
                callerAvatar: callInfo.receiverAvatar || null,
                callType: callInfo.callType || callType,
                duration: 0,
                endReason: 'FAILED',
                errorMessage: err.message || 'Không thể khởi tạo cuộc gọi'
            })
            cleanup()
        }
    }, [currentUserId, socketAPI, cleanup, verifyAndEnableAudio])

    const answerCall = useCallback(async () => {
        if (hasAnsweredRef.current) {
            return
        }

        hasAnsweredRef.current = true

        try {
            const currentState = callStateRef.current

            ringtoneManagerRef.current?.stop()

            const stream = await webRTCService.initializeMedia(
                currentState.callType === 'VIDEO'
            )

            stream.getAudioTracks().forEach((track) => {
                track.enabled = true
            })

            if (localVideoRef.current) {
                localVideoRef.current.srcObject = stream
                localVideoRef.current.muted = true

                localVideoRef.current.play()
                    .then(() => { })
                    .catch(() => { })
            }

            const callerId = currentState.callerId
            if (callerId) {
                remoteUserIdRef.current = callerId
                setupPeerConnection(callerId)
            }

            await new Promise(resolve => setTimeout(resolve, 100))

            const success = socketAPI.answerCall(currentState.callId || '')
            if (!success) throw new Error('Socket answer failed')

            setCallState(prev => ({
                ...prev,
                isIncoming: false,
                isInCall: true,
                status: CALL_STATUS.ANSWERED
            }))

            setMediaState({
                isAudioEnabled: true,
                isVideoEnabled: currentState.callType === 'VIDEO'
            })

        } catch {
            hasAnsweredRef.current = false
            ringtoneManagerRef.current?.stop()

            const currentState = callStateRef.current
            setCallEndedInfo({
                callerName: currentState.callerName || 'Người gọi',
                callerAvatar: currentState.callerAvatar,
                callType: currentState.callType,
                duration: 0,
                endReason: 'FAILED'
            })
            cleanup()
        }
    }, [socketAPI, cleanup, setupPeerConnection])

    const rejectCall = useCallback(() => {
        const currentState = callStateRef.current

        ringtoneManagerRef.current?.stop()

        if (currentState.callId) {
            socketAPI.rejectCall(currentState.callId)
        }

        cleanup()
    }, [socketAPI, cleanup])

    const endCall = useCallback(() => {
        const currentState = callStateRef.current

        ringtoneManagerRef.current?.stop()

        if (currentState.callId) {
            socketAPI.endCall(currentState.callId)
        }

        cleanup()
    }, [socketAPI, cleanup])

    const toggleAudio = useCallback(() => {
        setMediaState(prev => {
            const newState = !prev.isAudioEnabled

            webRTCService.toggleAudio(newState)

            setTimeout(() => {
                const stream = webRTCService.localStream
                if (stream) {
                    stream.getAudioTracks().forEach((track) => {
                        if (track.enabled !== newState) {
                            track.enabled = newState
                        }
                    })
                }
            }, 100)

            return { ...prev, isAudioEnabled: newState }
        })
    }, [])

    const toggleVideo = useCallback(() => {
        setMediaState(prev => {
            const newState = !prev.isVideoEnabled

            webRTCService.toggleVideo(newState)

            return { ...prev, isVideoEnabled: newState }
        })
    }, [])

    const closeCallEndedModal = useCallback(() => {
        setCallEndedInfo(null)
    }, [])

    // ============ SOCKET EVENT HANDLERS (Memoized) ============
    const handleCallInitiated = useCallback((data: unknown) => {
        const eventData = data as CallEventData
        const callInfo = pendingCallInfoRef.current

        if (!callInfo) {
            return
        }

        if (callTimerRef.current) {
            clearTimeout(callTimerRef.current)
            callTimerRef.current = null
        }

        setCallState(prev => ({
            ...prev,
            callId: eventData.callId || null,
            receiverId: callInfo.receiverId,
            receiverName: callInfo.receiverName,
            receiverAvatar: callInfo.receiverAvatar || null,
            status: CALL_STATUS.RINGING
        }))

        const receiverId = remoteUserIdRef.current || callInfo.receiverId
        if (!receiverId) {
            return
        }

        setupPeerConnection(receiverId)

        setTimeout(() => {
            webRTCService.createOffer()
                .then(offer => {
                    socketAPI.sendWebRTCOffer(receiverId, offer)
                })
                .catch(() => { })
        }, 100)
    }, [setupPeerConnection, socketAPI])

    const handleIncomingCall = useCallback((data: unknown) => {
        const eventData = data as CallEventData
        remoteUserIdRef.current = eventData.callerId || null
        hasAnsweredRef.current = false

        setCallState({
            isInCall: true,
            isIncoming: true,
            callId: eventData.callId || null,
            callerId: eventData.callerId || null,
            callerName: eventData.callerName || null,
            callerAvatar: eventData.callerAvatar || null,
            receiverId: currentUserId,
            receiverName: null,
            receiverAvatar: null,
            callType: eventData.callType || null,
            status: CALL_STATUS.RINGING
        })

        ringtoneManagerRef.current?.play()
    }, [currentUserId])

    const handleCallAnswered = useCallback(() => {
        ringtoneManagerRef.current?.stop()

        setCallState(prev => {
            if (prev.status === CALL_STATUS.CONNECTED) {
                return prev
            }

            return {
                ...prev,
                status: CALL_STATUS.ANSWERED
            }
        })
    }, [])

    const handleCallRejected = useCallback(() => {
        ringtoneManagerRef.current?.stop()

        const currentState = callStateRef.current
        const callInfo: Partial<PendingCallInfo> = pendingCallInfoRef.current || {}

        setCallEndedInfo({
            callerName: currentState.receiverName || callInfo.receiverName || currentState.callerName || 'Người nhận',
            callerAvatar: currentState.receiverAvatar || callInfo.receiverAvatar || currentState.callerAvatar,
            callType: currentState.callType || callInfo.callType || null,
            duration: 0,
            endReason: 'REJECTED'
        })

        cleanup()
    }, [cleanup])

    const handleCallEnded = useCallback((data: unknown) => {
        const eventData = data as CallEventData
        ringtoneManagerRef.current?.stop()

        const currentState = callStateRef.current
        const currentDuration = callTimerRef.current ? callDuration : 0
        const callInfo: Partial<PendingCallInfo> = pendingCallInfoRef.current || {}

        setCallEndedInfo({
            callerName: currentState.receiverName || callInfo.receiverName || currentState.callerName || 'Cuộc gọi',
            callerAvatar: currentState.receiverAvatar || callInfo.receiverAvatar || currentState.callerAvatar,
            callType: currentState.callType || callInfo.callType || null,
            duration: eventData.duration || currentDuration,
            endReason: 'ENDED'
        })

        cleanup()
    }, [cleanup, callDuration])

    const handleCallFailed = useCallback((data: unknown) => {
        const eventData = data as CallEventData
        ringtoneManagerRef.current?.stop()

        const currentState = callStateRef.current

        if (currentState.callId) {
            socketAPI.endCall(currentState.callId)
        }

        const callInfo: Partial<PendingCallInfo> = pendingCallInfoRef.current || {}

        let endReason = 'FAILED'
        let errorMessage = eventData.reason || 'Không thể kết nối'

        if (eventData.reason && eventData.reason.includes('another call')) {
            endReason = 'BUSY'
            errorMessage = 'Người dùng đang bận'
        } else if (eventData.reason && eventData.reason.includes('not available')) {
            endReason = 'OFFLINE'
            errorMessage = 'Người dùng không trực tuyến'
        } else if (eventData.reason && eventData.reason.includes('offline')) {
            endReason = 'OFFLINE'
            errorMessage = 'Người dùng offline'
        }

        setCallEndedInfo({
            callerName: currentState.receiverName || callInfo.receiverName || currentState.callerName || 'Cuộc gọi',
            callerAvatar: currentState.receiverAvatar || callInfo.receiverAvatar || currentState.callerAvatar,
            callType: currentState.callType || callInfo.callType || null,
            duration: 0,
            endReason: endReason,
            errorMessage: errorMessage
        })

        cleanup()
    }, [cleanup, socketAPI])

    const handleWebRTCOffer = useCallback(async (data: unknown) => {
        try {
            const eventData = data as CallEventData
            const callerId = eventData.from || remoteUserIdRef.current
            if (!callerId) {
                return
            }

            let retries = 0
            const maxRetries = 30

            while (!webRTCService.localStream && retries < maxRetries) {
                await new Promise(resolve => setTimeout(resolve, 100))
                retries++
            }

            if (!webRTCService.localStream) {
                return
            }

            try {
                if (eventData.offer) {
                    const answer = await webRTCService.createAnswer(eventData.offer)
                    socketAPI.sendWebRTCAnswer(callerId, answer)
                }

                setTimeout(() => {
                    verifyAndEnableAudio()
                }, 500)

            } catch {
                // Error creating answer
            }

        } catch {
            // Error handling WebRTC offer
        }
    }, [socketAPI, verifyAndEnableAudio])

    const handleWebRTCAnswer = useCallback(async (data: unknown) => {
        try {
            const eventData = data as CallEventData
            if (eventData.answer) {
                await webRTCService.setRemoteAnswer(eventData.answer)
            }

            setTimeout(() => {
                verifyAndEnableAudio()
            }, 500)

        } catch {
            // Error handling WebRTC answer
        }
    }, [verifyAndEnableAudio])

    const handleICECandidate = useCallback(async (data: unknown) => {
        try {
            const eventData = data as CallEventData
            if (eventData.candidate) {
                await webRTCService.addIceCandidate(eventData.candidate)
            }
        } catch {
            // Error adding ICE candidate
        }
    }, [])

    // ============ REGISTER CALLBACKS ============
    useEffect(() => {
        if (!socketAPI.isConnected) return

        const callbacks = {
            onCallInitiated: handleCallInitiated,
            onIncomingCall: handleIncomingCall,
            onCallAnswered: handleCallAnswered,
            onCallRejected: handleCallRejected,
            onCallEnded: handleCallEnded,
            onCallFailed: handleCallFailed,
            onWebRTCOffer: handleWebRTCOffer,
            onWebRTCAnswer: handleWebRTCAnswer,
            onICECandidate: handleICECandidate,
        }

        socketAPI.registerCallCallbacks(callbacks)

        return () => {
            socketAPI.registerCallCallbacks({
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
        }
    }, [
        socketAPI.isConnected,
        socketAPI,
        handleCallInitiated,
        handleIncomingCall,
        handleCallAnswered,
        handleCallRejected,
        handleCallEnded,
        handleCallFailed,
        handleWebRTCOffer,
        handleWebRTCAnswer,
        handleICECandidate
    ])

    // ============ CLEANUP ON UNMOUNT ============
    useEffect(() => {
        return () => {
            ringtoneManagerRef.current?.destroy()
            cleanup()
        }
    }, [cleanup])

    return {
        callState,
        mediaState,
        connectionState,
        callDuration,
        localVideoRef,
        remoteVideoRef,
        isConnected: socketAPI.isConnected,
        initiateCall,
        answerCall,
        rejectCall,
        endCall,
        toggleAudio,
        toggleVideo,
        callEndedInfo,
        closeCallEndedModal
    }
}

export const formatDuration = (seconds: number): string => {
    const hrs = Math.floor(seconds / 3600)
    const mins = Math.floor((seconds % 3600) / 60)
    const secs = seconds % 60

    if (hrs > 0) {
        return `${hrs}:${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
    }
    return `${mins}:${secs.toString().padStart(2, '0')}`
}

export default useCall
