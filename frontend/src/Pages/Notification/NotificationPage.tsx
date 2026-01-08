import { useState, useEffect, useMemo, ChangeEvent } from "react";
import { useNotification } from "../../contexts/NotificationContext";
import { Bell } from "lucide-react";
import Header from "../../Components/Notification/Header";
import NotificationItem from "../../Components/Notification/NotificationItem";
import { getToken } from "../../service/LocalStorageService";
import { fetchPostById } from "../../api/postApi";
import { jwtDecode } from "jwt-decode";
import {
    getAllNotifications,
    markAllNotificationsAsRead,
    markNotificationAsRead,
} from "../../api/notificationAPI";
import { useToast } from "@chakra-ui/react";
import { useNavigate } from "react-router-dom";

interface Notification {
    id: string;
    senderName?: string;
    senderImageUrl?: string;
    avatar?: string;
    content?: string;
    message?: string;
    timestamp?: string;
    createdDate?: string;
    type?: string;
    postId?: string;
    post_id?: string;
    entityId?: string;
    senderId?: string;
    seen?: boolean;
}

interface DecodedToken {
    sub: string;
    [key: string]: unknown;
}

interface RealtimeNotification {
    id: string;
    senderName?: string;
    senderImageUrl?: string;
    avatar?: string;
    content?: string;
    message?: string;
    timestamp?: string;
    createdDate?: string;
    type?: string;
    postId?: string;
    senderId?: string;
    seen?: boolean;
}

const NotificationsPage: React.FC = () => {
    const [notifications, setNotifications] = useState<Notification[]>([]);
    const [searchTerm, setSearchTerm] = useState("");
    const [isLoading, setIsLoading] = useState(true);

    const toast = useToast();
    const navigate = useNavigate();
    const token = getToken();

    // ✅ Lấy realtime noti từ Context
    const { notifications: realtimeNotifications, notificationCounter } =
        useNotification();

    // ✅ Giải mã token để lấy userId
    const userId = useMemo(() => {
        if (!token) return "";
        try {
            const decoded = jwtDecode<DecodedToken>(token);
            return decoded.sub;
        } catch {
            return "";
        }
    }, [token]);

    // ✅ Lấy danh sách ban đầu từ API
    useEffect(() => {
        const getNotifications = async () => {
            try {
                setIsLoading(true);
                const result = await getAllNotifications(token || "", userId);
                setNotifications(result || []);
            } catch (error) {
                console.error("Error fetching notifications:", error);
            } finally {
                setIsLoading(false);
            }
        };
        if (token && userId) getNotifications();
    }, [token, userId]);

    // ✅ Realtime notification handler
    useEffect(() => {
        console.log("🔄 Notification counter changed:", notificationCounter);

        if (!realtimeNotifications || realtimeNotifications.length === 0) {
            console.log("⚠️ No realtime notifications");
            return;
        }

        const latest = realtimeNotifications[0] as RealtimeNotification;
        console.log("📥 Processing latest notification:", latest);

        const newNotification: Notification = {
            id: latest.id,
            senderName: latest.senderName, // ✅ Dùng lại tên đã chuẩn trong Provider
            senderImageUrl: latest.senderImageUrl || latest.avatar,
            content: latest.content || latest.message,
            timestamp:
                latest.timestamp || latest.createdDate || new Date().toISOString(),
            type: latest.type || "general",
            postId: latest.postId,
            senderId: latest.senderId,
            seen: latest.seen ?? false,
        };

        setNotifications((prev) => {
            const exists = prev.some((n) => n.id === newNotification.id);

            if (exists) {
                console.log("⚠️ Notification already in list:", newNotification.id);
                return prev;
            }

            console.log("✅ Adding notification to page list");
            return [newNotification, ...prev];
        });
    }, [notificationCounter, realtimeNotifications]);

    // ✅ Mark 1 thông báo là đã đọc
    const handleMarkRead = async (id: string) => {
        try {
            await markNotificationAsRead(token || "", id);
            setNotifications((prev) =>
                prev.map((n) => (n.id === id ? { ...n, seen: true } : n))
            );
        } catch (error) {
            console.error("Error marking notification as read:", error);
        }
    };

    // ✅ Mark tất cả đã đọc
    const handleMarkAllRead = async () => {
        try {
            await markAllNotificationsAsRead(token || "");
            toast({
                title: "All marked as read",
                status: "success",
                duration: 2000,
                isClosable: true,
                position: "top-right",
            });
            setNotifications((prev) => prev.map((n) => ({ ...n, seen: true })));
        } catch (error) {
            console.error("Error marking all as read:", error);
            toast({
                title: "Failed to mark all as read",
                status: "error",
                duration: 2000,
                isClosable: true,
                position: "top-right",
            });
        }
    };

    // ✅ Khi click vào notification → navigate tới post page
    const handleNotificationClick = async (notification: Notification) => {
        const postId =
            notification.postId || notification.post_id || notification.entityId;

        console.log("🔍 Notification object:", notification);
        console.log("🔍 Extracted Post ID:", postId);

        if (!postId) {
            toast({
                title: "Notification không có bài viết liên kết",
                status: "info",
                duration: 2000,
                isClosable: true,
                position: "top-right",
            });
            return;
        }

        try {
            // Mark as read
            if (!notification.seen) {
                await markNotificationAsRead(token || "", notification.id);
                setNotifications((prev) =>
                    prev.map((n) => (n.id === notification.id ? { ...n, seen: true } : n))
                );
            }

            // Fetch post
            const post = await fetchPostById(postId, token || "");
            console.log("✅ Post fetched successfully:", post);

            if (!post) {
                toast({
                    title: "Không tìm thấy bài viết",
                    description: "Bài viết có thể đã bị xóa",
                    status: "warning",
                    duration: 2000,
                    isClosable: true,
                    position: "top-right",
                });
                return;
            }

            // ✅ Navigate với post data
            navigate(`/post/${postId}`, { state: { post } });
        } catch (error) {
            console.error("❌ Error opening post:", error);

            const err = error as { response?: { data?: { message?: string }; status?: number } };
            const errorMessage =
                err.response?.data?.message || err.response?.status === 404
                    ? "Bài viết không tồn tại hoặc đã bị xóa"
                    : "Không thể mở bài viết";

            toast({
                title: errorMessage,
                status: "error",
                duration: 2000,
                isClosable: true,
                position: "top-right",
            });
        }
    };

    // ✅ Lọc & sắp xếp
    const filteredNotifications = useMemo(() => {
        return notifications.filter(
            (notification) =>
                (notification.senderName &&
                    notification.senderName
                        .toLowerCase()
                        .includes(searchTerm.toLowerCase())) ||
                (notification.content &&
                    notification.content.toLowerCase().includes(searchTerm.toLowerCase()))
        );
    }, [notifications, searchTerm]);

    const sortedNotifications = useMemo(() => {
        return [...filteredNotifications].sort((a, b) => {
            if (a.seen === b.seen) return 0;
            return a.seen ? 1 : -1;
        });
    }, [filteredNotifications]);

    const unreadCount = notifications.filter((n) => !n.seen).length;

    // ✅ Giao diện Loading
    const LoadingSkeleton = () => (
        <div className="space-y-3 p-4">
            {[...Array(5)].map((_, index) => (
                <div
                    key={index}
                    className="flex items-start gap-4 p-4 bg-white rounded-xl border border-gray-100 animate-pulse"
                >
                    <div className="w-12 h-12 rounded-full bg-gradient-to-br from-sky-100 to-blue-100"></div>
                    <div className="flex-1 space-y-2">
                        <div className="h-4 bg-gray-200 rounded w-3/4"></div>
                        <div className="h-3 bg-gray-100 rounded w-1/2"></div>
                    </div>
                </div>
            ))}
        </div>
    );

    // ✅ Giao diện Empty
    const EmptyState = () => (
        <div className="flex flex-col items-center justify-center h-full p-8 text-center">
            <div className="w-24 h-24 bg-gradient-to-br from-sky-100 to-blue-100 rounded-full flex items-center justify-center mb-6 animate-pulse">
                <Bell size={40} className="text-sky-500" />
            </div>
            <h3 className="text-xl font-bold text-gray-800 mb-2">
                {searchTerm ? "No matching notifications" : "All caught up!"}
            </h3>
            <p className="text-gray-500 text-sm max-w-sm mb-4">
                {searchTerm
                    ? `No notifications found for "${searchTerm}"`
                    : "You're all up to date. New notifications will appear here."}
            </p>
            {searchTerm && (
                <button
                    onClick={() => setSearchTerm("")}
                    className="px-6 py-2 bg-gradient-to-r from-sky-400 to-blue-500 text-white rounded-xl font-semibold hover:from-sky-500 hover:to-blue-600 transition-all shadow-md hover:shadow-lg"
                >
                    Clear search
                </button>
            )}
        </div>
    );

    return (
        <div className="max-w-full min-h-screen bg-gradient-to-b from-gray-50 to-white flex flex-col">
            <Header
                unreadCount={unreadCount}
                onMarkAllRead={handleMarkAllRead}
                searchTerm={searchTerm}
                setSearchTerm={setSearchTerm}
            />

            <div className="flex-grow overflow-auto">
                {isLoading ? (
                    <LoadingSkeleton />
                ) : sortedNotifications.length > 0 ? (
                    <div className="p-4 space-y-2">
                        {sortedNotifications.map((notification) => (
                            <NotificationItem
                                key={notification.id}
                                notification={notification}
                                onMarkRead={handleMarkRead}
                                onClick={() => handleNotificationClick(notification)}
                            />
                        ))}
                    </div>
                ) : (
                    <EmptyState />
                )}
            </div>
        </div>
    );
};

export default NotificationsPage;
