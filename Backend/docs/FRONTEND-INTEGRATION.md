# 📱 FRONTEND INTEGRATION

## React + STOMP.js WebSocket Integration

---

## 1. Install Dependencies

```bash
cd frontend
npm install @stomp/stompjs sockjs-client
npm install @types/sockjs-client --save-dev  # nếu dùng TypeScript
```

---

## 2. WebSocket Context

📁 **File:** `frontend/src/contexts/WebSocketContext.jsx`
```jsx
import React, { createContext, useContext, useEffect, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useAuth } from './AuthContext';

const WebSocketContext = createContext(null);

export const useWebSocket = () => useContext(WebSocketContext);

export const WebSocketProvider = ({ children }) => {
    const { token, user } = useAuth();
    const clientRef = useRef(null);
    const [isConnected, setIsConnected] = useState(false);
    const [onlineUsers, setOnlineUsers] = useState([]);

    useEffect(() => {
        if (!token) return;

        const client = new Client({
            webSocketFactory: () => new SockJS('http://localhost:8082/ws'),
            connectHeaders: {
                Authorization: `Bearer ${token}`
            },
            debug: (str) => console.log('[STOMP]', str),
            reconnectDelay: 5000,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,
        });

        client.onConnect = () => {
            console.log('WebSocket Connected');
            setIsConnected(true);

            // Subscribe to online status
            client.subscribe('/topic/online', (message) => {
                setOnlineUsers(JSON.parse(message.body));
            });
        };

        client.onDisconnect = () => {
            console.log('WebSocket Disconnected');
            setIsConnected(false);
        };

        client.activate();
        clientRef.current = client;

        return () => {
            if (client.active) {
                client.deactivate();
            }
        };
    }, [token]);

    const value = {
        client: clientRef.current,
        isConnected,
        onlineUsers,
    };

    return (
        <WebSocketContext.Provider value={value}>
            {children}
        </WebSocketContext.Provider>
    );
};
```

---

## 3. Chat Hook

📁 **File:** `frontend/src/hooks/useChat.js`
```jsx
import { useEffect, useState, useCallback } from 'react';
import { useWebSocket } from '../contexts/WebSocketContext';

export const useChat = (conversationId) => {
    const { client, isConnected } = useWebSocket();
    const [messages, setMessages] = useState([]);
    const [typingUsers, setTypingUsers] = useState([]);

    useEffect(() => {
        if (!client || !isConnected || !conversationId) return;

        // Subscribe to chat messages
        const messageSub = client.subscribe(
            `/topic/chat/${conversationId}`,
            (message) => {
                const newMessage = JSON.parse(message.body);
                setMessages(prev => [...prev, newMessage]);
            }
        );

        // Subscribe to typing indicator
        const typingSub = client.subscribe(
            `/topic/chat/${conversationId}/typing`,
            (message) => {
                const typing = JSON.parse(message.body);
                setTypingUsers(prev => {
                    if (typing.isTyping) {
                        return [...new Set([...prev, typing.userId])];
                    } else {
                        return prev.filter(id => id !== typing.userId);
                    }
                });
            }
        );

        return () => {
            messageSub.unsubscribe();
            typingSub.unsubscribe();
        };
    }, [client, isConnected, conversationId]);

    const sendMessage = useCallback((content, attachments = []) => {
        if (!client || !isConnected) return;

        client.publish({
            destination: `/app/chat.send/${conversationId}`,
            body: JSON.stringify({ content, attachments })
        });
    }, [client, isConnected, conversationId]);

    const sendTyping = useCallback((isTyping) => {
        if (!client || !isConnected) return;

        client.publish({
            destination: `/app/chat.typing/${conversationId}`,
            body: JSON.stringify({ isTyping })
        });
    }, [client, isConnected, conversationId]);

    return {
        messages,
        typingUsers,
        sendMessage,
        sendTyping
    };
};
```

---

## 4. Feed Hook (Real-time Posts)

📁 **File:** `frontend/src/hooks/useFeed.js`
```jsx
import { useEffect, useState } from 'react';
import { useWebSocket } from '../contexts/WebSocketContext';

export const useFeed = () => {
    const { client, isConnected } = useWebSocket();
    const [newPosts, setNewPosts] = useState([]);

    useEffect(() => {
        if (!client || !isConnected) return;

        // Subscribe to personal feed queue
        const feedSub = client.subscribe('/user/queue/feed', (message) => {
            const feedItem = JSON.parse(message.body);
            setNewPosts(prev => [feedItem, ...prev]);
        });

        return () => feedSub.unsubscribe();
    }, [client, isConnected]);

    const clearNewPosts = () => setNewPosts([]);

    return { newPosts, clearNewPosts };
};
```

---

## 5. Notification Hook

📁 **File:** `frontend/src/hooks/useNotifications.js`
```jsx
import { useEffect, useState } from 'react';
import { useWebSocket } from '../contexts/WebSocketContext';

export const useNotifications = () => {
    const { client, isConnected } = useWebSocket();
    const [notifications, setNotifications] = useState([]);
    const [unreadCount, setUnreadCount] = useState(0);

    useEffect(() => {
        if (!client || !isConnected) return;

        const notifSub = client.subscribe('/user/queue/notifications', (message) => {
            const notification = JSON.parse(message.body);
            setNotifications(prev => [notification, ...prev]);
            setUnreadCount(prev => prev + 1);
        });

        return () => notifSub.unsubscribe();
    }, [client, isConnected]);

    const markAsRead = (notificationId) => {
        setNotifications(prev =>
            prev.map(n => n.id === notificationId ? { ...n, read: true } : n)
        );
        setUnreadCount(prev => Math.max(0, prev - 1));
    };

    return { notifications, unreadCount, markAsRead };
};
```

---

## 6. Call Hook (WebRTC)

📁 **File:** `frontend/src/hooks/useCall.js`
```jsx
import { useEffect, useState, useCallback, useRef } from 'react';
import { useWebSocket } from '../contexts/WebSocketContext';

export const useCall = () => {
    const { client, isConnected } = useWebSocket();
    const [incomingCall, setIncomingCall] = useState(null);
    const [activeCall, setActiveCall] = useState(null);
    const peerConnection = useRef(null);
    const localStream = useRef(null);

    useEffect(() => {
        if (!client || !isConnected) return;

        const callSub = client.subscribe('/user/queue/call', (message) => {
            const event = JSON.parse(message.body);
            
            switch (event.type) {
                case 'INCOMING_CALL':
                    setIncomingCall(event);
                    break;
                case 'CALL_ANSWERED':
                    handleCallAnswered(event);
                    break;
                case 'CALL_REJECTED':
                    setActiveCall(null);
                    break;
                case 'WEBRTC_OFFER':
                    handleOffer(event);
                    break;
                case 'WEBRTC_ANSWER':
                    handleAnswer(event);
                    break;
                case 'ICE_CANDIDATE':
                    handleIceCandidate(event);
                    break;
            }
        });

        return () => callSub.unsubscribe();
    }, [client, isConnected]);

    const initiateCall = useCallback(async (targetUserId, isVideo) => {
        // Get local media
        localStream.current = await navigator.mediaDevices.getUserMedia({
            audio: true,
            video: isVideo
        });

        // Create peer connection
        peerConnection.current = new RTCPeerConnection({
            iceServers: [{ urls: 'stun:stun.l.google.com:19302' }]
        });

        localStream.current.getTracks().forEach(track => {
            peerConnection.current.addTrack(track, localStream.current);
        });

        // Send call initiation
        client.publish({
            destination: '/app/call.initiate',
            body: JSON.stringify({ targetUserId, isVideo })
        });

        setActiveCall({ targetUserId, isVideo, status: 'calling' });
    }, [client]);

    const answerCall = useCallback(async () => {
        if (!incomingCall) return;

        client.publish({
            destination: '/app/call.answer',
            body: JSON.stringify({
                callId: incomingCall.callId,
                callerId: incomingCall.callerId
            })
        });

        setActiveCall({ ...incomingCall, status: 'connected' });
        setIncomingCall(null);
    }, [client, incomingCall]);

    const rejectCall = useCallback(() => {
        if (!incomingCall) return;

        client.publish({
            destination: '/app/call.reject',
            body: JSON.stringify({
                callId: incomingCall.callId,
                callerId: incomingCall.callerId
            })
        });

        setIncomingCall(null);
    }, [client, incomingCall]);

    return {
        incomingCall,
        activeCall,
        initiateCall,
        answerCall,
        rejectCall,
        localStream: localStream.current
    };
};
```

---

## 7. Search Component

📁 **File:** `frontend/src/components/UserSearch.jsx`
```jsx
import React, { useState, useEffect, useCallback } from 'react';
import debounce from 'lodash/debounce';
import { searchUsers } from '../services/api';

export const UserSearch = ({ onSelect }) => {
    const [query, setQuery] = useState('');
    const [results, setResults] = useState([]);
    const [loading, setLoading] = useState(false);

    const debouncedSearch = useCallback(
        debounce(async (q) => {
            if (q.length < 2) {
                setResults([]);
                return;
            }
            setLoading(true);
            try {
                const response = await searchUsers(q);
                setResults(response.data.result);
            } catch (error) {
                console.error('Search error:', error);
            } finally {
                setLoading(false);
            }
        }, 300),
        []
    );

    useEffect(() => {
        debouncedSearch(query);
        return () => debouncedSearch.cancel();
    }, [query, debouncedSearch]);

    return (
        <div className="user-search">
            <input
                type="text"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder="Tìm kiếm người dùng..."
                className="search-input"
            />
            
            {loading && <div className="loading">Đang tìm...</div>}
            
            <ul className="search-results">
                {results.map(user => (
                    <li key={user.id} onClick={() => onSelect(user)}>
                        <img src={user.avatarUrl} alt="" />
                        <div>
                            <strong>{user.displayName}</strong>
                            <span>@{user.username}</span>
                        </div>
                    </li>
                ))}
            </ul>
        </div>
    );
};
```

---

## 8. Chat Component Example

📁 **File:** `frontend/src/components/Chat.jsx`
```jsx
import React, { useState, useEffect, useRef } from 'react';
import { useChat } from '../hooks/useChat';

export const Chat = ({ conversationId }) => {
    const { messages, typingUsers, sendMessage, sendTyping } = useChat(conversationId);
    const [input, setInput] = useState('');
    const typingTimeout = useRef(null);
    const messagesEndRef = useRef(null);

    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [messages]);

    const handleInputChange = (e) => {
        setInput(e.target.value);
        
        // Send typing indicator
        sendTyping(true);
        
        // Clear typing after 2 seconds of no input
        clearTimeout(typingTimeout.current);
        typingTimeout.current = setTimeout(() => {
            sendTyping(false);
        }, 2000);
    };

    const handleSend = () => {
        if (!input.trim()) return;
        sendMessage(input);
        setInput('');
        sendTyping(false);
    };

    return (
        <div className="chat">
            <div className="messages">
                {messages.map(msg => (
                    <div key={msg.id} className={`message ${msg.isMine ? 'mine' : ''}`}>
                        <span className="content">{msg.content}</span>
                        <span className="time">{msg.createdAt}</span>
                    </div>
                ))}
                <div ref={messagesEndRef} />
            </div>

            {typingUsers.length > 0 && (
                <div className="typing-indicator">
                    {typingUsers.join(', ')} đang gõ...
                </div>
            )}

            <div className="input-area">
                <input
                    value={input}
                    onChange={handleInputChange}
                    onKeyPress={(e) => e.key === 'Enter' && handleSend()}
                    placeholder="Nhập tin nhắn..."
                />
                <button onClick={handleSend}>Gửi</button>
            </div>
        </div>
    );
};
```

---

## 9. Feed Component với Real-time

📁 **File:** `frontend/src/components/Feed.jsx`
```jsx
import React, { useState, useEffect } from 'react';
import { useFeed } from '../hooks/useFeed';
import { fetchPosts } from '../services/api';
import { Post } from './Post';

export const Feed = () => {
    const { newPosts, clearNewPosts } = useFeed();
    const [posts, setPosts] = useState([]);
    const [page, setPage] = useState(1);

    useEffect(() => {
        loadPosts();
    }, [page]);

    const loadPosts = async () => {
        const response = await fetchPosts(page);
        setPosts(prev => [...prev, ...response.data.result.posts]);
    };

    const showNewPosts = () => {
        setPosts(prev => [...newPosts, ...prev]);
        clearNewPosts();
    };

    return (
        <div className="feed">
            {newPosts.length > 0 && (
                <button className="new-posts-btn" onClick={showNewPosts}>
                    {newPosts.length} bài viết mới
                </button>
            )}

            {posts.map(post => (
                <Post key={post.id} post={post} />
            ))}

            <button onClick={() => setPage(p => p + 1)}>
                Xem thêm
            </button>
        </div>
    );
};
```

---

## 10. App.jsx với Providers

📁 **File:** `frontend/src/App.jsx`
```jsx
import React from 'react';
import { BrowserRouter } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';
import { WebSocketProvider } from './contexts/WebSocketContext';
import { Routes } from './Routes';

function App() {
    return (
        <BrowserRouter>
            <AuthProvider>
                <WebSocketProvider>
                    <Routes />
                </WebSocketProvider>
            </AuthProvider>
        </BrowserRouter>
    );
}

export default App;
```

---

## ✅ FRONTEND CHECKLIST

- [ ] Install @stomp/stompjs and sockjs-client
- [ ] Create WebSocketContext
- [ ] Create useChat hook
- [ ] Create useFeed hook
- [ ] Create useNotifications hook
- [ ] Create useCall hook
- [ ] Update Chat component
- [ ] Update Feed component
- [ ] Add UserSearch component
- [ ] Test real-time features
