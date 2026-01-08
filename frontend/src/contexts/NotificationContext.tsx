import React, { createContext, useContext, useState, useCallback, useMemo, ReactNode } from "react"
import { X } from "lucide-react"
import { useNavigate } from "react-router-dom"

// ===== TYPES =====
export interface NotificationData {
    id: string
    senderName?: string
    senderFirstName?: string
    senderLastName?: string
    avatar?: string
    senderImageUrl?: string
    message?: string
    content?: string
    createdDate?: string
    timestamp?: string
    type?: string
    seen?: boolean
    read?: boolean
    postId?: string
    senderId?: string
    onClick?: (notification: NotificationItem) => void
}

export interface NotificationItem {
    id: string
    senderName: string
    senderFirstName?: string
    senderLastName?: string
    avatar?: string
    message?: string
    createdDate: string
    type: string
    seen: boolean
    postId?: string
    senderId?: string
    onClick?: (notification: NotificationItem) => void
}

interface NotificationContextValue {
    notifications: NotificationItem[]
    addNotification: (notificationData: NotificationData) => void
    removeNotification: (notificationId: string) => void
    setNotificationsList: (list: NotificationItem[]) => void
    setMode: (mode: "toast" | "silent") => void
    mode: "toast" | "silent"
    notificationCounter: number
}

interface NotificationProviderProps {
    children: ReactNode
}

interface MessageToastProps {
    notification: NotificationItem
    onClose: (id: string) => void
    onClick: (notification: NotificationItem) => void
}

// ===== CONTEXT =====
const NotificationContext = createContext<NotificationContextValue | null>(null)

export const useNotification = (): NotificationContextValue => {
    const context = useContext(NotificationContext)
    if (!context) {
        throw new Error("useNotification must be used within NotificationProvider")
    }
    return context
}

export const requestNotificationPermission = async (): Promise<boolean> => {
    if (!("Notification" in window)) {
        console.log("Browser không hỗ trợ notification")
        return false
    }

    if (Notification.permission === "granted") return true
    if (Notification.permission !== "denied") {
        const permission = await Notification.requestPermission()
        return permission === "granted"
    }
    return false
}

// ===== TOAST COMPONENT =====
const MessageToast: React.FC<MessageToastProps> = ({ notification, onClose, onClick }) => {
    const displayName = useMemo(() => {
        if (notification.senderName && notification.senderName !== "Unknown User") {
            return notification.senderName
        }

        const firstName = notification.senderFirstName || ""
        const lastName = notification.senderLastName || ""
        const fullName = `${firstName} ${lastName}`.trim()

        return fullName || "Unknown User"
    }, [
        notification.senderFirstName,
        notification.senderLastName,
        notification.senderName,
    ])

    const handleClick = () => {
        onClick(notification)
        onClose(notification.id)
    }

    return (
        <div
            onClick={handleClick}
            className="w-[320px] bg-white rounded-xl shadow-lg border border-sky-100 p-3 cursor-pointer hover:shadow-xl hover:-translate-y-0.5 transition-all duration-200"
        >
            <div className="flex items-start gap-3">
                {/* Avatar */}
                <img
                    src={
                        notification.avatar ||
                        "https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_640.png"
                    }
                    alt={displayName}
                    className="w-10 h-10 rounded-full object-cover border border-sky-200 flex-shrink-0"
                />

                {/* Nội dung */}
                <div className="flex-1 min-w-0">
                    {/* Dòng chính: Tên + message */}
                    <p className="text-sm text-gray-800 leading-snug">
                        <span className="font-semibold mr-1">{displayName}</span>
                        {notification.message || "đã gửi một thông báo cho bạn."}
                    </p>

                    {/* Thời gian */}
                    <p className="text-xs text-gray-400 mt-1">
                        {formatTime(notification.createdDate)}
                    </p>

                    {/* Click để xem */}
                    {notification.postId && (
                        <p className="text-xs text-sky-600 font-medium mt-1">
                            Click để xem →
                        </p>
                    )}
                </div>

                {/* Nút đóng */}
                <button
                    onClick={(e) => {
                        e.stopPropagation()
                        onClose(notification.id)
                    }}
                    className="ml-1 text-gray-400 hover:text-gray-600"
                >
                    <X size={14} />
                </button>
            </div>
        </div>
    )
}

// ===== PROVIDER =====
export const NotificationProvider: React.FC<NotificationProviderProps> = ({ children }) => {
    const [notifications, setNotifications] = useState<NotificationItem[]>([])
    const [mode, setMode] = useState<"toast" | "silent">("toast")
    const [notificationCounter, setNotificationCounter] = useState(0)
    const navigate = useNavigate()

    const addNotification = useCallback((notificationData: NotificationData) => {
        console.log("🔔 Adding notification:", notificationData)

        setNotifications((prev) => {
            if (!notificationData?.id) {
                console.warn("⚠️ Notification missing ID")
                return prev
            }

            const exists = prev.some((n) => n.id === notificationData.id)
            if (exists) {
                console.log("⚠️ Notification already exists:", notificationData.id)
                return prev
            }

            const fullName = [notificationData.senderFirstName, notificationData.senderLastName]
                .filter(Boolean)
                .join(" ")
            const senderName = fullName || notificationData.senderName || "Unknown User"

            const notification: NotificationItem = {
                id: notificationData.id,
                senderFirstName: notificationData.senderFirstName,
                senderLastName: notificationData.senderLastName,
                senderName,
                avatar: notificationData.avatar || notificationData.senderImageUrl,
                message: notificationData.content || notificationData.message,
                createdDate:
                    notificationData.createdDate ||
                    notificationData.timestamp ||
                    new Date().toISOString(),
                type: notificationData.type || "general",
                seen: notificationData.seen ?? notificationData.read ?? false,
                postId: notificationData.postId,
                senderId: notificationData.senderId,
            }

            playNotificationSound()
            if (document.hidden) showBrowserNotification(notification)
            setNotificationCounter((c) => c + 1)

            console.log("✅ Notification added to state")
            return [notification, ...prev]
        })
    }, [])

    const setNotificationsList = useCallback((list: NotificationItem[]) => {
        setNotifications(list)
    }, [])

    const removeNotification = useCallback((notificationId: string) => {
        setNotifications((prev) => prev.filter((n) => n.id !== notificationId))
    }, [])

    const handleNotificationClick = useCallback(
        (notification: NotificationItem) => {
            if (notification.onClick) {
                notification.onClick(notification)
            }

            if (notification.postId) {
                navigate(`/post/${notification.postId}`)
            }

            removeNotification(notification.id)
        },
        [navigate, removeNotification]
    )

    const value = useMemo<NotificationContextValue>(() => ({
        notifications,
        addNotification,
        removeNotification,
        setNotificationsList,
        setMode,
        mode,
        notificationCounter,
    }), [notifications, addNotification, removeNotification, setNotificationsList, mode, notificationCounter])

    return (
        <NotificationContext.Provider value={value}>
            {children}
            <div className="fixed top-4 right-4 z-[9999] space-y-3 max-h-screen overflow-y-auto pointer-events-none">
                <div className="pointer-events-auto">
                    {mode === "toast" &&
                        notifications.map((notification) => (
                            <MessageToast
                                key={notification.id}
                                notification={notification}
                                onClose={removeNotification}
                                onClick={handleNotificationClick}
                            />
                        ))}
                </div>
            </div>
        </NotificationContext.Provider>
    )
}

// ===== Helpers =====
const formatTime = (dateString?: string): string => {
    if (!dateString) return "Vừa xong"
    const date = new Date(dateString)
    const now = new Date()
    const diff = Math.floor((now.getTime() - date.getTime()) / 1000)
    if (diff < 60) return "Vừa xong"
    if (diff < 3600) return `${Math.floor(diff / 60)} phút trước`
    if (diff < 86400) return `${Math.floor(diff / 3600)} giờ trước`
    return date.toLocaleTimeString("vi-VN", {
        hour: "2-digit",
        minute: "2-digit",
        day: "2-digit",
        month: "2-digit",
    })
}

const playNotificationSound = (): void => {
    try {
        const audio = new Audio("/notification.mp3")
        audio.play().catch(() => { })
    } catch {
        // Ignore audio errors
    }
}

const showBrowserNotification = (notification: NotificationItem): void => {
    if ("Notification" in window && Notification.permission === "granted") {
        const n = new Notification(notification.senderName, {
            body: notification.message,
            icon: notification.avatar,
        })
        setTimeout(() => n.close(), 5000)
    }
}
