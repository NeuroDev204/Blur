import { useEffect, useCallback } from "react"
import { useNotificationSocket } from "../contexts/NotificationSocketContext"

interface ModerationUpdate {
  commentId: string
  postId: string
  status: "APPROVED" | "REJECTED" | "FLAGGED" | "PENDING_MODERATION"
  timestamp?: number
}

type ModerationCallback = (update: ModerationUpdate) => void

export const useModerationListener = (onModerationUpdate: ModerationCallback) => {
  const stompClient = useNotificationSocket()

  useEffect(() => {
    if (!stompClient) return

    const handleModerationMessage = (message: { body: string }) => {
      try {
        const data: ModerationUpdate = JSON.parse(message.body)
        onModerationUpdate(data)
      } catch (e) {
        console.error("Error parsing moderation message:", e)
      }
    }

    // Subscribe to moderation updates from communication-service WebSocket
    stompClient.subscribe(`/user/queue/moderation`, handleModerationMessage)

    return () => {
      // Note: STOMP client unsubscribe is handled by the context
    }
  }, [stompClient, onModerationUpdate])
}
