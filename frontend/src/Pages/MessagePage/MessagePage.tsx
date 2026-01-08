import React, { useState, useEffect, useRef, useCallback, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import toast, { Toaster } from 'react-hot-toast';
import { getUserId, getToken } from "../../utils/auth";
import { apiCall, profileApiCall } from "../../service/api";
import { useSocket } from "../../contexts/SocketContext";
import { useNotification, requestNotificationPermission } from "../../contexts/NotificationContext";
import { markConversationAsRead } from "../../service/chatApi";
import ConnectionStatus from "../../Components/Message/ConnectionStatus";
import ConversationList from "../../Components/Message/ConversationList";
import ChatArea from "../../Components/Message/ChatArea";

interface Participant {
    userId: string;
    firstName?: string;
    lastName?: string;
    username?: string;
    imageUrl?: string;
    avatar?: string;
}

interface Conversation {
    id: string;
    conversationName?: string;
    conversationAvatar?: string;
    lastMessage?: string;
    lastMessageTime?: string;
    participants?: Participant[];
    userId?: string;
    isTemporary?: boolean;
}

interface Message {
    id: string;
    message: string;
    senderId: string;
    conversationId: string;
    createdDate: string;
    sender?: Participant;
    messageType?: string;
    attachments?: Attachment[];
    isPending?: boolean;
    isFailed?: boolean;
    isRead?: boolean;
    isAiMessage?: boolean;
}

interface Attachment {
    url: string;
    type?: string;
    [key: string]: unknown;
}

interface User {
    userId: string;
    firstName?: string;
    lastName?: string;
    username?: string;
    imageUrl?: string;
    avatar?: string;
}

interface MessageEventData {
    id: string;
    tempMessageId?: string;
    message: string;
    senderId?: string;
    conversationId: string;
    createdDate?: string;
    sender?: Participant;
    messageType?: string;
    attachments?: Attachment[];
    isRead?: boolean;
    isAiMessage?: boolean;
}

interface NotificationData {
    id: string;
    conversationId: string;
    senderName: string;
    senderUsername?: string;
    avatar?: string | null;
    message: string;
    attachments?: Attachment[];
    createdDate?: string;
    isAiMessage?: boolean;
    onClick: (notification: NotificationData) => void;
}

declare global {
    interface Window {
        toast: typeof toast;
    }
}

const MessagePage: React.FC = () => {
    const [conversations, setConversations] = useState<Conversation[]>([]);
    const [selectedChat, setSelectedChat] = useState<Conversation | null>(null);
    const [messages, setMessages] = useState<Message[]>([]);
    const [currentUserId, setCurrentUserId] = useState<string | null>(null);
    const [currentUser, setCurrentUser] = useState<User | null>(null);
    const [loadingMessages, setLoadingMessages] = useState(false);
    const [messagesError, setMessagesError] = useState<string | null>(null);
    const currentConversationRef = useRef<string | null>(null);
    const messagesFetchedRef = useRef(new Set<string>());

    const navigate = useNavigate();
    const { sendMessage, isConnected, error, registerMessageCallbacks } = useSocket();
    const { addNotification } = useNotification();

    // === MAKE TOAST AVAILABLE GLOBALLY ===
    useEffect(() => {
        window.toast = toast;
        return () => {
            delete (window as { toast?: typeof toast }).toast;
        };
    }, []);

    // === REQUEST NOTIFICATION PERMISSION ===
    useEffect(() => {
        requestNotificationPermission();
    }, []);

    // === INIT USER ID ===
    useEffect(() => {
        const userId = getUserId();
        setCurrentUserId(userId);
    }, []);

    // === FETCH CURRENT USER INFO ===
    useEffect(() => {
        const fetchCurrentUserInfo = async () => {
            if (!currentUserId) return;

            try {
                try {
                    const response = await profileApiCall<{ result?: User }>('/users/myInfo');

                    if (response?.result) {
                        setCurrentUser(response.result);
                        return;
                    }
                } catch (apiError) {
                    // API not available
                }

                if (conversations.length > 0) {
                    for (const conv of conversations) {
                        if (conv.participants && Array.isArray(conv.participants)) {
                            const currentUserParticipant = conv.participants.find(
                                p => p.userId === currentUserId
                            );

                            if (currentUserParticipant) {
                                setCurrentUser(currentUserParticipant as User);
                                return;
                            }
                        }
                    }
                }

            } catch (error) {
                // Error fetching user info
            }
        };

        fetchCurrentUserInfo();
    }, [currentUserId, conversations]);

    // === FETCH CONVERSATIONS WITH LAST MESSAGES ===
    const fetchConversations = useCallback(async () => {
        try {
            const data = await apiCall<{ result?: Conversation[] }>("/conversations/my-conversations");
            const convs = data.result || [];

            console.log('✅ Fetched conversations with last messages:', convs);

            // ✅ Backend đã trả về lastMessage và lastMessageTime
            // Sort: unread first, then by lastMessageTime
            const sortedConvs = convs.sort((a, b) => {
                // Priority 1: Unread conversations first (handled by ConversationList)

                // Priority 2: Sort by lastMessageTime
                const timeA = a.lastMessageTime ? new Date(a.lastMessageTime).getTime() : 0;
                const timeB = b.lastMessageTime ? new Date(b.lastMessageTime).getTime() : 0;
                return timeB - timeA;
            });

            setConversations(sortedConvs);
        } catch (err) {
            console.error('❌ Error fetching conversations:', err);
            toast.error('Không thể tải danh sách trò chuyện', {
                duration: 2000,
                style: { borderRadius: '12px', fontSize: '14px' }
            });
        }
    }, []);

    // ✅ Initial load
    useEffect(() => {
        fetchConversations();
    }, [fetchConversations]);

    // === FETCH MESSAGES ===
    const fetchMessages = useCallback(async (conversationId: string) => {
        setLoadingMessages(true);
        setMessagesError(null);
        try {
            const data = await apiCall<{ result?: Message[] }>(`/messages?conversationId=${conversationId}`);

            if (!data) {
                throw new Error('No response data from server');
            }

            const msgs: Message[] = (data.result || []).map((msg) => ({
                id: msg.id,
                message: msg.message,
                senderId: msg.sender?.userId || msg.senderId || '',
                conversationId: msg.conversationId,
                createdDate: msg.createdDate,
                sender: msg.sender,
                messageType: msg.messageType,
                attachments: msg.attachments,
                isPending: false,
                isRead: msg.isRead,
            }));

            const sortedMsgs = msgs.sort((a, b) => {
                const timeA = new Date(a.createdDate).getTime();
                const timeB = new Date(b.createdDate).getTime();
                return timeA - timeB;
            });

            setMessages(sortedMsgs);
            messagesFetchedRef.current.add(conversationId);
            setLoadingMessages(false);
        } catch (err) {
            const error = err as Error;
            console.error('❌ Error fetching messages:', error);
            setMessagesError(error.message || 'Failed to load messages');
            setLoadingMessages(false);
            toast.error('Không thể tải tin nhắn. Vui lòng thử lại.', {
                duration: 2000,
                style: { borderRadius: '12px', fontSize: '14px' }
            });
        }
    }, []);

    // === HANDLE SELECT CONVERSATION ===
    const handleSelectConversation = useCallback(async (conv: Conversation) => {
        if (!currentUserId || !conv) return;

        setSelectedChat(conv);
        currentConversationRef.current = conv.id;
        setMessages([]);
        setMessagesError(null);

        // Always fetch messages fresh (don't use cache)
        await fetchMessages(conv.id);

        try {
            const token = getToken();
            if (token) {
                markConversationAsRead(conv.id, token).catch(() => { });
            }
        } catch (err) {
            // Error marking as read
        }
    }, [currentUserId, fetchMessages]);

    // === HANDLE CONVERSATION DELETED ===
    const handleConversationDeleted = useCallback((deletedConversationId: string) => {
        setConversations((prev) =>
            prev.filter((conv) => conv.id !== deletedConversationId)
        );

        if (selectedChat?.id === deletedConversationId) {
            setSelectedChat(null);
            setMessages([]);
            currentConversationRef.current = null;
        }
    }, [selectedChat]);

    // === CALLBACK: MESSAGE SENT ===
    const handleMessageSent = useCallback((data: MessageEventData) => {
        console.log('📤 Message sent event:', data);

        // ✅ Refresh conversations để lấy lastMessage mới
        fetchConversations();

        // Update messages if current conversation
        if (data.conversationId === currentConversationRef.current) {
            setMessages((prev) => {
                const tempIdx = prev.findIndex((m) => m.id === data.tempMessageId);

                if (tempIdx === -1) {
                    return prev;
                }

                const updated = [...prev];
                updated[tempIdx] = {
                    id: data.id,
                    message: data.message,
                    senderId: data.senderId || data.sender?.userId || '',
                    conversationId: data.conversationId,
                    createdDate: data.createdDate || new Date().toISOString(),
                    sender: data.sender,
                    messageType: data.messageType,
                    attachments: data.attachments,
                    isPending: false,
                    isRead: data.isRead,
                };

                return updated;
            });
        }
    }, [fetchConversations]);

    // === CALLBACK: MESSAGE RECEIVED ===
    const handleMessageReceived = useCallback((data: MessageEventData) => {
        console.log('📥 Message received event:', data);

        const messageSenderId = data.senderId || data.sender?.userId;
        const isCurrentConversation = data.conversationId === currentConversationRef.current;

        // ✅ Check if it's AI message
        const isAiMessage = messageSenderId === 'AI_BOT' || data.isAiMessage === true;

        // ✅ Refresh conversations to update last message
        fetchConversations();

        // Show notification if needed
        if (!isCurrentConversation || document.hidden) {
            // ✅ Special handling for AI messages
            const senderName = isAiMessage
                ? '🤖 AI Assistant'
                : `${data.sender?.firstName || ''} ${data.sender?.lastName || ''}`.trim()
                || data.sender?.username
                || 'Người dùng';

            addNotification({
                id: data.id,
                conversationId: data.conversationId,
                senderName,
                senderUsername: data.sender?.username,
                avatar: isAiMessage
                    ? null // AI will have gradient avatar
                    : data.sender?.avatar,
                message: data.message,
                attachments: data.attachments,
                createdDate: data.createdDate,
                isAiMessage, // ✅ ADD THIS
                onClick: (notification: NotificationData) => {
                    setConversations(prev => {
                        const conv = prev.find(c => c.id === notification.conversationId);
                        if (conv) {
                            handleSelectConversation(conv);
                        }
                        return prev;
                    });

                    if (window.location.pathname !== '/messages') {
                        navigate('/messages');
                    }
                },
            } as unknown as Parameters<typeof addNotification>[0]);

            // ✅ Show toast for AI messages even if tab is active
            if (isAiMessage && isCurrentConversation && !document.hidden) {
                toast('🤖 AI đã trả lời', {
                    duration: 2000,
                    style: {
                        borderRadius: '12px',
                        background: 'linear-gradient(135deg, #10b981, #059669)',
                        color: '#fff',
                        fontSize: '14px',
                    }
                });
            }
        }

        // Add message to current conversation
        if (isCurrentConversation) {
            setMessages((prev) => {
                if (prev.some((m) => m.id === data.id)) {
                    return prev;
                }

                const newMessage: Message = {
                    id: data.id,
                    message: data.message,
                    senderId: messageSenderId || '',
                    conversationId: data.conversationId,
                    createdDate: data.createdDate || new Date().toISOString(),
                    sender: data.sender,
                    messageType: data.messageType,
                    attachments: data.attachments,
                    isPending: false,
                    isRead: data.isRead,
                    isAiMessage, // ✅ ADD THIS FLAG
                };

                return [...prev, newMessage];
            });

            try {
                const token = getToken();
                if (token) {
                    markConversationAsRead(data.conversationId, token).catch(() => { });
                }
            } catch (err) {
                // Error auto-marking as read
            }
        }
    }, [addNotification, navigate, handleSelectConversation, fetchConversations]);

    // === REGISTER CALLBACKS ===
    useEffect(() => {
        registerMessageCallbacks({
            onMessageSent: handleMessageSent as (data: unknown) => void,
            onMessageReceived: handleMessageReceived as (data: unknown) => void,
        });
    }, [registerMessageCallbacks, handleMessageSent, handleMessageReceived]);

    // === HANDLE CONVERSATION CREATED (from UserSearchBar) ===
    const handleConversationCreated = useCallback(
        (conversation: Conversation) => {
            if (conversation) {
                // Immediately add to conversations list and select it
                setConversations((prev) => {
                    // Check if already exists
                    if (!prev.find((c) => c.id === conversation.id)) {
                        return [conversation, ...prev];
                    }
                    return prev;
                });
                // Immediately select the newly created conversation
                handleSelectConversation(conversation);
            }
        },
        [handleSelectConversation]
    );

    // === HANDLE SELECT USER (Fallback if no conversation created) ===
    const handleSelectUser = useCallback(
        async (user: User) => {
            const existingConv = conversations.find(
                (conv) =>
                    conv.conversationName === `${user.firstName} ${user.lastName}` ||
                    conv.conversationName === user.username
            );

            if (existingConv) {
                handleSelectConversation(existingConv);
            } else {
                const tempConv: Conversation = {
                    id: `temp-${user.userId}`,
                    conversationName: `${user.firstName} ${user.lastName}`,
                    conversationAvatar: user.imageUrl || user.avatar,
                    userId: user.userId,
                    isTemporary: true,
                };
                setSelectedChat(tempConv);
                currentConversationRef.current = tempConv.id;
                setMessages([]);
            }
        },
        [conversations, handleSelectConversation]
    );

    // === HANDLE SEND MESSAGE ===
    const handleSendMessage = useCallback(
        async (text: string, attachments: Attachment[] = []) => {
            if ((!text.trim() && attachments.length === 0) || !selectedChat || !currentUserId) {
                return;
            }

            if (!isConnected) {
                toast.error("Không có kết nối. Vui lòng thử lại.", {
                    duration: 2000,
                    style: { borderRadius: '12px', fontSize: '14px' }
                });
                return;
            }

            const tempId = `temp-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;

            const validAttachments = attachments.filter(
                (att) => att && att.url && att.url.trim() !== ""
            );

            const tempMessage: Message = {
                id: tempId,
                message: text,
                senderId: currentUserId,
                conversationId: selectedChat.id,
                createdDate: new Date().toISOString(),
                isPending: true,
                attachments: validAttachments,
                sender: undefined,
            };

            setMessages((prev) => [...prev, tempMessage]);

            const payload = {
                conversationId: selectedChat.id,
                message: text,
                messageId: tempId,
                attachments: validAttachments,
                recipientUserId: selectedChat.isTemporary ? selectedChat.userId : undefined,
            };

            const success = sendMessage(payload);

            if (!success) {
                setMessages((prev) =>
                    prev.map((m) =>
                        m.id === tempId ? { ...m, isPending: false, isFailed: true } : m
                    )
                );
            }
        },
        [selectedChat, currentUserId, isConnected, sendMessage]
    );

    // === HANDLE BACK (Mobile) ===
    const handleBack = useCallback(() => {
        setSelectedChat(null);
        currentConversationRef.current = null;
    }, []);

    // Memoize conversations
    const sortedConversations = useMemo(() => conversations, [conversations]);

    return (
        <div className="flex h-screen bg-gradient-to-br from-gray-50 to-blue-50 overflow-hidden">
            <Toaster position="top-center" />
            <ConnectionStatus error={error} />

            {/* Conversation List - Hidden on mobile when chat selected */}
            <div className={`${selectedChat ? 'hidden md:flex' : 'flex'} w-full md:w-80`}>
                <ConversationList
                    conversations={sortedConversations}
                    selected={selectedChat}
                    onSelect={handleSelectConversation}
                    onSelectUser={handleSelectUser}
                    onConversationDeleted={handleConversationDeleted}
                    onConversationCreated={handleConversationCreated}
                />
            </div>

            {/* Chat Area - Hidden on mobile when no chat selected */}
            <div className={`${!selectedChat ? 'hidden md:flex' : 'flex'} flex-1`}>
                <ChatArea
                    conversation={selectedChat}
                    messages={messages}
                    onSendMessage={handleSendMessage}
                    isConnected={isConnected}
                    currentUserId={currentUserId}
                    currentUser={currentUser}
                    loadingMessages={loadingMessages}
                    messagesError={messagesError}
                />
            </div>
        </div>
    );
};

export default MessagePage;
