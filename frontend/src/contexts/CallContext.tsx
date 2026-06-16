// src/contexts/CallContext.tsx
//
// Provider goi dien o tang ung dung. Truoc day useCall chi duoc mount trong ChatArea (trang message),
// nen neu nguoi nhan dang o trang home / profile / bat ky dau ngoai message thi khong co handler
// onIncomingCall -> cuoc goi den bi bo qua. Dua useCall len day de DU NGUOI NHAN O DAU cung nhan duoc cuoc goi.
//
// Luu y: SocketContext.registerCallCallbacks chi giu MOT bo callback, nen chi duoc co DUY NHAT mot useCall
// trong toan app. Cac component (vd ChatArea) phai goi useCallContext() thay vi tu goi useCall.

import React, { createContext, useContext, useEffect, useState, ReactNode } from "react"
import { useCall, type CallState, type ReceiverData } from "../hooks/useCall"
import { getUserId, setUserId } from "../utils/auth"
import { profileApiCall } from "../service/api"
import IncomingCallModal from "../Components/Call/IncommingCallModal"
import CallWindow from "../Components/Call/CallWindow"
import CallEndedModal from "../Components/Call/CallendedModal"

interface CallContextValue {
    callState: CallState
    initiateCall: (receiverData: ReceiverData, callType: "VOICE" | "VIDEO") => void
    currentUserId: string | null
}

const CallContext = createContext<CallContextValue | null>(null)

export const CallProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
    const [currentUserId, setCurrentUserId] = useState<string | null>(getUserId())

    // Xac dinh user dang dang nhap (de useCall biet callerId / receiverId). Resolve khi mount va sau khi login.
    useEffect(() => {
        let mounted = true

        const resolve = async () => {
            const stored = getUserId()
            if (stored && mounted) setCurrentUserId(stored)

            try {
                const res = await profileApiCall<{ result?: { userId?: string } }>("/users/myInfo")
                if (mounted && res?.result?.userId) {
                    setCurrentUserId(res.result.userId)
                    setUserId(res.result.userId)
                }
            } catch {
                // Chua dang nhap hoac chua co session -> bo qua, se thu lai khi co su kien login.
            }
        }

        resolve()
        const onLogin = () => resolve()
        window.addEventListener("auth-login-success", onLogin)
        return () => {
            mounted = false
            window.removeEventListener("auth-login-success", onLogin)
        }
    }, [])

    const {
        callState,
        mediaState,
        connectionState,
        callDuration,
        localVideoRef,
        remoteVideoRef,
        initiateCall,
        answerCall,
        rejectCall,
        endCall,
        toggleAudio,
        toggleVideo,
        callEndedInfo,
        closeCallEndedModal,
    } = useCall(currentUserId || "")

    return (
        <CallContext.Provider value={{ callState, initiateCall, currentUserId }}>
            {children}

            {/* ============ CALL MODALS (toan app) ============ */}

            {/* Cuoc goi den */}
            {callState.isIncoming && (
                <IncomingCallModal
                    callerName={callState.callerName}
                    callerAvatar={callState.callerAvatar}
                    callType={callState.callType}
                    onAnswer={answerCall}
                    onReject={rejectCall}
                />
            )}

            {/* Cua so cuoc goi dang dien ra */}
            {callState.isInCall && !callState.isIncoming && (
                <CallWindow
                    callState={callState}
                    mediaState={mediaState}
                    connectionState={connectionState}
                    callDuration={callDuration}
                    localVideoRef={localVideoRef}
                    remoteVideoRef={remoteVideoRef}
                    onEndCall={endCall}
                    onToggleAudio={toggleAudio}
                    onToggleVideo={toggleVideo}
                />
            )}

            {/* Modal ket thuc cuoc goi */}
            {callEndedInfo && (
                <CallEndedModal
                    callerName={callEndedInfo.callerName}
                    callerAvatar={callEndedInfo.callerAvatar}
                    callType={callEndedInfo.callType}
                    duration={callEndedInfo.duration}
                    endReason={callEndedInfo.endReason as never}
                    onClose={closeCallEndedModal}
                />
            )}
        </CallContext.Provider>
    )
}

export const useCallContext = (): CallContextValue => {
    const context = useContext(CallContext)
    if (!context) {
        throw new Error("useCallContext must be used within CallProvider")
    }
    return context
}
