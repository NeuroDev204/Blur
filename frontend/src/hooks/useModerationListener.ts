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
    const subscription = stompClient.subscribe(`/user/queue/moderation`, handleModerationMessage)

    return () => {
      // Unsubscribe so handlers don't accumulate across re-renders/remounts
      // (accumulated handlers caused one moderation event to fire many duplicate warnings).
      try {
        subscription?.unsubscribe?.()
      } catch (e) {
        // no-op: client may already be deactivated
      }
    }
  }, [stompClient, onModerationUpdate])
}
