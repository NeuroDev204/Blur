(globalThis.TURBOPACK || (globalThis.TURBOPACK = [])).push([typeof document === "object" ? document.currentScript : undefined,
"[project]/src/lib/utils/constants.ts [app-client] (ecmascript)", ((__turbopack_context__) => {
"use strict";

__turbopack_context__.s([
    "AI_API",
    ()=>AI_API,
    "API_BASE",
    ()=>API_BASE,
    "API_BASE_URL",
    ()=>API_BASE_URL,
    "PROFILE_API",
    ()=>PROFILE_API,
    "SOCKET_URL",
    ()=>SOCKET_URL
]);
var __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$build$2f$polyfills$2f$process$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = /*#__PURE__*/ __turbopack_context__.i("[project]/node_modules/next/dist/build/polyfills/process.js [app-client] (ecmascript)");
const API_BASE_URL = ("TURBOPACK compile-time value", "http://localhost:8888/api") || 'http://localhost:8888/api';
const API_BASE = ("TURBOPACK compile-time value", "http://localhost:8888/api/chat") || 'http://localhost:8888/api/chat';
const PROFILE_API = ("TURBOPACK compile-time value", "http://localhost:8888/api/profile") || 'http://localhost:8888/api/profile';
const SOCKET_URL = ("TURBOPACK compile-time value", "http://localhost:8099") || 'http://localhost:8099';
const AI_API = ("TURBOPACK compile-time value", "http://localhost:9090") || 'http://localhost:9090';
if (typeof globalThis.$RefreshHelpers$ === 'object' && globalThis.$RefreshHelpers !== null) {
    __turbopack_context__.k.registerExports(__turbopack_context__.m, globalThis.$RefreshHelpers$);
}
}),
"[project]/src/lib/utils/auth.ts [app-client] (ecmascript)", ((__turbopack_context__) => {
"use strict";

__turbopack_context__.s([
    "getToken",
    ()=>getToken,
    "getUserId",
    ()=>getUserId,
    "isAuthenticated",
    ()=>isAuthenticated,
    "removeToken",
    ()=>removeToken,
    "setToken",
    ()=>setToken
]);
const getToken = ()=>{
    if ("TURBOPACK compile-time falsy", 0) //TURBOPACK unreachable
    ;
    return localStorage.getItem("token");
};
const setToken = (token)=>{
    if ("TURBOPACK compile-time truthy", 1) {
        localStorage.setItem("token", token);
    }
};
const removeToken = ()=>{
    if ("TURBOPACK compile-time truthy", 1) {
        localStorage.removeItem("token");
    }
};
const getUserId = ()=>{
    const token = getToken();
    if (!token) return null;
    try {
        const payload = token.split('.')[1];
        const decoded = JSON.parse(atob(payload));
        return decoded.sub || decoded.userId || decoded.user_id || decoded.id || null;
    } catch (error) {
        console.error("Cannot decode token:", error);
        return null;
    }
};
const isAuthenticated = ()=>{
    return !!getToken();
};
if (typeof globalThis.$RefreshHelpers$ === 'object' && globalThis.$RefreshHelpers !== null) {
    __turbopack_context__.k.registerExports(__turbopack_context__.m, globalThis.$RefreshHelpers$);
}
}),
"[project]/src/contexts/SocketContext.tsx [app-client] (ecmascript)", ((__turbopack_context__) => {
"use strict";

__turbopack_context__.s([
    "SocketProvider",
    ()=>SocketProvider,
    "useSocket",
    ()=>useSocket
]);
var __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/node_modules/next/dist/compiled/react/jsx-dev-runtime.js [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/node_modules/next/dist/compiled/react/index.js [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$lib$2f$utils$2f$constants$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/lib/utils/constants.ts [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$lib$2f$utils$2f$auth$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/lib/utils/auth.ts [app-client] (ecmascript)");
;
var _s = __turbopack_context__.k.signature(), _s1 = __turbopack_context__.k.signature();
'use client';
;
;
;
const SocketContext = /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["createContext"])(null);
const SocketProvider = ({ children })=>{
    _s();
    const socketRef = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useRef"])(null);
    const [isConnected, setIsConnected] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])(false);
    const [error, setError] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])("");
    const messageCallbacksRef = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useRef"])({
        onMessageSent: null,
        onMessageReceived: null
    });
    const callCallbacksRef = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useRef"])({
        onCallInitiated: null,
        onIncomingCall: null,
        onCallAnswered: null,
        onCallRejected: null,
        onCallEnded: null,
        onCallFailed: null,
        onWebRTCOffer: null,
        onWebRTCAnswer: null,
        onICECandidate: null
    });
    const registerMessageCallbacks = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useCallback"])({
        "SocketProvider.useCallback[registerMessageCallbacks]": (callbacks)=>{
            messageCallbacksRef.current = {
                ...messageCallbacksRef.current,
                ...callbacks
            };
        }
    }["SocketProvider.useCallback[registerMessageCallbacks]"], []);
    const registerCallCallbacks = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useCallback"])({
        "SocketProvider.useCallback[registerCallCallbacks]": (callbacks)=>{
            const hasChanged = Object.keys(callbacks).some({
                "SocketProvider.useCallback[registerCallCallbacks].hasChanged": (key)=>callCallbacksRef.current[key] !== callbacks[key]
            }["SocketProvider.useCallback[registerCallCallbacks].hasChanged"]);
            if (!hasChanged) {
                return;
            }
            callCallbacksRef.current = {
                ...callCallbacksRef.current,
                ...callbacks
            };
        }
    }["SocketProvider.useCallback[registerCallCallbacks]"], []);
    (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useEffect"])({
        "SocketProvider.useEffect": ()=>{
            if ("TURBOPACK compile-time falsy", 0) //TURBOPACK unreachable
            ;
            const token = (0, __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$lib$2f$utils$2f$auth$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__["getToken"])();
            if (!token) {
                return;
            }
            const script = document.createElement("script");
            script.src = "https://cdn.socket.io/4.5.4/socket.io.min.js";
            script.async = true;
            script.onload = ({
                "SocketProvider.useEffect": ()=>{
                    if (socketRef.current) {
                        return;
                    }
                    const socket = window.io(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$lib$2f$utils$2f$constants$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__["SOCKET_URL"], {
                        query: {
                            token
                        },
                        autoConnect: true,
                        reconnection: true,
                        reconnectionDelay: 1000,
                        reconnectionAttempts: 10,
                        timeout: 20000,
                        transports: [
                            "websocket",
                            "polling"
                        ]
                    });
                    socketRef.current = socket;
                    socket.on("connect", {
                        "SocketProvider.useEffect": ()=>{
                            setIsConnected(true);
                            setError("");
                        }
                    }["SocketProvider.useEffect"]);
                    socket.on("disconnect", {
                        "SocketProvider.useEffect": ()=>{
                            setIsConnected(false);
                        }
                    }["SocketProvider.useEffect"]);
                    socket.on("connect_error", {
                        "SocketProvider.useEffect": ()=>{
                            setError("Không thể kết nối socket");
                            setIsConnected(false);
                        }
                    }["SocketProvider.useEffect"]);
                    socket.on("reconnect_attempt", {
                        "SocketProvider.useEffect": ()=>{
                        // Reconnecting...
                        }
                    }["SocketProvider.useEffect"]);
                    socket.on("reconnect", {
                        "SocketProvider.useEffect": ()=>{
                            setIsConnected(true);
                            setError("");
                        }
                    }["SocketProvider.useEffect"]);
                    socket.on("connected", {
                        "SocketProvider.useEffect": ()=>{
                        // Connected event received
                        }
                    }["SocketProvider.useEffect"]);
                    // Chat events
                    socket.on("message_sent", {
                        "SocketProvider.useEffect": (data)=>{
                            if (messageCallbacksRef.current.onMessageSent) {
                                messageCallbacksRef.current.onMessageSent(data);
                            }
                        }
                    }["SocketProvider.useEffect"]);
                    socket.on("message_received", {
                        "SocketProvider.useEffect": (data)=>{
                            if (messageCallbacksRef.current.onMessageReceived) {
                                messageCallbacksRef.current.onMessageReceived(data);
                            }
                        }
                    }["SocketProvider.useEffect"]);
                    socket.on("user_typing", {
                        "SocketProvider.useEffect": ()=>{
                        // User typing event
                        }
                    }["SocketProvider.useEffect"]);
                    socket.on("message_error", {
                        "SocketProvider.useEffect": (error)=>{
                            const err = error;
                            setError(err?.message || "Lỗi khi gửi tin nhắn");
                        }
                    }["SocketProvider.useEffect"]);
                    socket.on("auth_error", {
                        "SocketProvider.useEffect": ()=>{
                            setError("Lỗi xác thực. Vui lòng đăng nhập lại.");
                        }
                    }["SocketProvider.useEffect"]);
                    // Call events
                    socket.on("call:initiated", {
                        "SocketProvider.useEffect": (data)=>{
                            console.log("✅ Received call:initiated event:", data);
                            if (callCallbacksRef.current.onCallInitiated) {
                                callCallbacksRef.current.onCallInitiated(data);
                            }
                        }
                    }["SocketProvider.useEffect"]);
                    socket.on("call:incoming", {
                        "SocketProvider.useEffect": (data)=>{
                            console.log("📞 Received call:incoming event:", data);
                            if (callCallbacksRef.current.onIncomingCall) {
                                callCallbacksRef.current.onIncomingCall(data);
                            }
                        }
                    }["SocketProvider.useEffect"]);
                    socket.on("call:answered", {
                        "SocketProvider.useEffect": (data)=>{
                            console.log("✅ Received call:answered event:", data);
                            if (callCallbacksRef.current.onCallAnswered) {
                                callCallbacksRef.current.onCallAnswered(data);
                            }
                        }
                    }["SocketProvider.useEffect"]);
                    socket.on("call:rejected", {
                        "SocketProvider.useEffect": (data)=>{
                            if (callCallbacksRef.current.onCallRejected) {
                                callCallbacksRef.current.onCallRejected(data);
                            }
                        }
                    }["SocketProvider.useEffect"]);
                    socket.on("call:ended", {
                        "SocketProvider.useEffect": (data)=>{
                            if (callCallbacksRef.current.onCallEnded) {
                                callCallbacksRef.current.onCallEnded(data);
                            }
                        }
                    }["SocketProvider.useEffect"]);
                    socket.on("call:failed", {
                        "SocketProvider.useEffect": (data)=>{
                            if (callCallbacksRef.current.onCallFailed) {
                                callCallbacksRef.current.onCallFailed(data);
                            }
                        }
                    }["SocketProvider.useEffect"]);
                    socket.on("webrtc:offer", {
                        "SocketProvider.useEffect": (data)=>{
                            if (callCallbacksRef.current.onWebRTCOffer) {
                                callCallbacksRef.current.onWebRTCOffer(data);
                            }
                        }
                    }["SocketProvider.useEffect"]);
                    socket.on("webrtc:answer", {
                        "SocketProvider.useEffect": (data)=>{
                            if (callCallbacksRef.current.onWebRTCAnswer) {
                                callCallbacksRef.current.onWebRTCAnswer(data);
                            }
                        }
                    }["SocketProvider.useEffect"]);
                    socket.on("webrtc:ice-candidate", {
                        "SocketProvider.useEffect": (data)=>{
                            if (callCallbacksRef.current.onICECandidate) {
                                callCallbacksRef.current.onICECandidate(data);
                            }
                        }
                    }["SocketProvider.useEffect"]);
                }
            })["SocketProvider.useEffect"];
            script.onerror = ({
                "SocketProvider.useEffect": ()=>{
                    setError("Không thể tải thư viện Socket.IO");
                }
            })["SocketProvider.useEffect"];
            document.head.appendChild(script);
            return ({
                "SocketProvider.useEffect": ()=>{
                    if (socketRef.current) {
                        socketRef.current.off("connect");
                        socketRef.current.off("disconnect");
                        socketRef.current.off("connect_error");
                        socketRef.current.off("reconnect_attempt");
                        socketRef.current.off("reconnect");
                        socketRef.current.off("connected");
                        socketRef.current.off("message_sent");
                        socketRef.current.off("message_received");
                        socketRef.current.off("user_typing");
                        socketRef.current.off("message_error");
                        socketRef.current.off("auth_error");
                        socketRef.current.off("call:initiated");
                        socketRef.current.off("call:incoming");
                        socketRef.current.off("call:answered");
                        socketRef.current.off("call:rejected");
                        socketRef.current.off("call:ended");
                        socketRef.current.off("call:failed");
                        socketRef.current.off("webrtc:offer");
                        socketRef.current.off("webrtc:answer");
                        socketRef.current.off("webrtc:ice-candidate");
                        socketRef.current.disconnect();
                        socketRef.current = null;
                    }
                    if (script.parentNode) {
                        script.parentNode.removeChild(script);
                    }
                }
            })["SocketProvider.useEffect"];
        }
    }["SocketProvider.useEffect"], []);
    const sendMessage = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useCallback"])({
        "SocketProvider.useCallback[sendMessage]": (messageData)=>{
            if (!socketRef.current || !isConnected) {
                return false;
            }
            try {
                socketRef.current.emit("send_message", messageData);
                return true;
            } catch  {
                return false;
            }
        }
    }["SocketProvider.useCallback[sendMessage]"], [
        isConnected
    ]);
    const sendTypingIndicator = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useCallback"])({
        "SocketProvider.useCallback[sendTypingIndicator]": (conversationId, isTyping)=>{
            if (!socketRef.current || !isConnected) {
                return;
            }
            try {
                socketRef.current.emit("typing", {
                    conversationId,
                    isTyping
                });
            } catch  {
            // Error sending typing indicator
            }
        }
    }["SocketProvider.useCallback[sendTypingIndicator]"], [
        isConnected
    ]);
    const initiateCall = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useCallback"])({
        "SocketProvider.useCallback[initiateCall]": (callData)=>{
            if (!socketRef.current || !isConnected) {
                console.error("❌ Socket not connected or socketRef is null");
                return false;
            }
            try {
                console.log("📡 Emitting call:initiate event:", callData);
                socketRef.current.emit("call:initiate", callData);
                return true;
            } catch (error) {
                console.error("❌ Error emitting call:initiate:", error);
                return false;
            }
        }
    }["SocketProvider.useCallback[initiateCall]"], [
        isConnected
    ]);
    const answerCall = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useCallback"])({
        "SocketProvider.useCallback[answerCall]": (callId)=>{
            if (!socketRef.current || !isConnected) {
                return false;
            }
            try {
                socketRef.current.emit("call:answer", {
                    callId
                });
                return true;
            } catch  {
                return false;
            }
        }
    }["SocketProvider.useCallback[answerCall]"], [
        isConnected
    ]);
    const rejectCall = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useCallback"])({
        "SocketProvider.useCallback[rejectCall]": (callId)=>{
            if (!socketRef.current || !isConnected) {
                return false;
            }
            try {
                socketRef.current.emit("call:reject", {
                    callId
                });
                return true;
            } catch  {
                return false;
            }
        }
    }["SocketProvider.useCallback[rejectCall]"], [
        isConnected
    ]);
    const endCall = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useCallback"])({
        "SocketProvider.useCallback[endCall]": (callId)=>{
            if (!socketRef.current || !isConnected) {
                return false;
            }
            try {
                socketRef.current.emit("call:end", {
                    callId
                });
                return true;
            } catch  {
                return false;
            }
        }
    }["SocketProvider.useCallback[endCall]"], [
        isConnected
    ]);
    const sendWebRTCOffer = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useCallback"])({
        "SocketProvider.useCallback[sendWebRTCOffer]": (toUserId, offer)=>{
            if (!socketRef.current || !isConnected) {
                return false;
            }
            try {
                socketRef.current.emit("webrtc:offer", {
                    to: toUserId,
                    offer
                });
                return true;
            } catch  {
                return false;
            }
        }
    }["SocketProvider.useCallback[sendWebRTCOffer]"], [
        isConnected
    ]);
    const sendWebRTCAnswer = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useCallback"])({
        "SocketProvider.useCallback[sendWebRTCAnswer]": (toUserId, answer)=>{
            if (!socketRef.current || !isConnected) {
                return false;
            }
            try {
                socketRef.current.emit("webrtc:answer", {
                    to: toUserId,
                    answer
                });
                return true;
            } catch  {
                return false;
            }
        }
    }["SocketProvider.useCallback[sendWebRTCAnswer]"], [
        isConnected
    ]);
    const sendICECandidate = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useCallback"])({
        "SocketProvider.useCallback[sendICECandidate]": (toUserId, candidate)=>{
            if (!socketRef.current || !isConnected) {
                return false;
            }
            try {
                socketRef.current.emit("webrtc:ice-candidate", {
                    to: toUserId,
                    candidate
                });
                return true;
            } catch  {
                return false;
            }
        }
    }["SocketProvider.useCallback[sendICECandidate]"], [
        isConnected
    ]);
    const contextValue = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useMemo"])({
        "SocketProvider.useMemo[contextValue]": ()=>({
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
                registerCallCallbacks
            })
    }["SocketProvider.useMemo[contextValue]"], [
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
        registerCallCallbacks
    ]);
    return /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(SocketContext.Provider, {
        value: contextValue,
        children: children
    }, void 0, false, {
        fileName: "[project]/src/contexts/SocketContext.tsx",
        lineNumber: 435,
        columnNumber: 9
    }, ("TURBOPACK compile-time value", void 0));
};
_s(SocketProvider, "Z3FP4QL+iFlffhPvrO9CwXLs974=");
_c = SocketProvider;
const useSocket = ()=>{
    _s1();
    const context = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useContext"])(SocketContext);
    if (!context) {
        throw new Error("useSocket must be used within SocketProvider");
    }
    return context;
};
_s1(useSocket, "b9L3QQ+jgeyIrH0NfHrJ8nn7VMU=");
var _c;
__turbopack_context__.k.register(_c, "SocketProvider");
if (typeof globalThis.$RefreshHelpers$ === 'object' && globalThis.$RefreshHelpers !== null) {
    __turbopack_context__.k.registerExports(__turbopack_context__.m, globalThis.$RefreshHelpers$);
}
}),
"[project]/src/contexts/NotificationContext.tsx [app-client] (ecmascript)", ((__turbopack_context__) => {
"use strict";

__turbopack_context__.s([
    "NotificationProvider",
    ()=>NotificationProvider,
    "requestNotificationPermission",
    ()=>requestNotificationPermission,
    "useNotification",
    ()=>useNotification
]);
var __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/node_modules/next/dist/compiled/react/jsx-dev-runtime.js [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/node_modules/next/dist/compiled/react/index.js [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$lucide$2d$react$2f$dist$2f$esm$2f$icons$2f$x$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__$3c$export__default__as__X$3e$__ = __turbopack_context__.i("[project]/node_modules/lucide-react/dist/esm/icons/x.js [app-client] (ecmascript) <export default as X>");
var __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$navigation$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/node_modules/next/navigation.js [app-client] (ecmascript)");
;
var _s = __turbopack_context__.k.signature(), _s1 = __turbopack_context__.k.signature(), _s2 = __turbopack_context__.k.signature();
'use client';
;
;
;
// ===== CONTEXT =====
const NotificationContext = /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["createContext"])(null);
const useNotification = ()=>{
    _s();
    const context = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useContext"])(NotificationContext);
    if (!context) {
        throw new Error("useNotification must be used within NotificationProvider");
    }
    return context;
};
_s(useNotification, "b9L3QQ+jgeyIrH0NfHrJ8nn7VMU=");
const requestNotificationPermission = async ()=>{
    if ("TURBOPACK compile-time falsy", 0) //TURBOPACK unreachable
    ;
    if (!("Notification" in window)) {
        console.log("Browser không hỗ trợ notification");
        return false;
    }
    if (Notification.permission === "granted") return true;
    if (Notification.permission !== "denied") {
        const permission = await Notification.requestPermission();
        return permission === "granted";
    }
    return false;
};
// ===== TOAST COMPONENT =====
const MessageToast = ({ notification, onClose, onClick })=>{
    _s1();
    const displayName = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useMemo"])({
        "MessageToast.useMemo[displayName]": ()=>{
            if (notification.senderName && notification.senderName !== "Unknown User") {
                return notification.senderName;
            }
            const firstName = notification.senderFirstName || "";
            const lastName = notification.senderLastName || "";
            const fullName = `${firstName} ${lastName}`.trim();
            return fullName || "Unknown User";
        }
    }["MessageToast.useMemo[displayName]"], [
        notification.senderFirstName,
        notification.senderLastName,
        notification.senderName
    ]);
    const handleClick = ()=>{
        onClick(notification);
        onClose(notification.id);
    };
    return /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
        onClick: handleClick,
        className: "w-[320px] bg-white rounded-xl shadow-lg border border-sky-100 p-3 cursor-pointer hover:shadow-xl hover:-translate-y-0.5 transition-all duration-200",
        children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
            className: "flex items-start gap-3",
            children: [
                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("img", {
                    src: notification.avatar || "https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_640.png",
                    alt: displayName,
                    className: "w-10 h-10 rounded-full object-cover border border-sky-200 flex-shrink-0"
                }, void 0, false, {
                    fileName: "[project]/src/contexts/NotificationContext.tsx",
                    lineNumber: 118,
                    columnNumber: 17
                }, ("TURBOPACK compile-time value", void 0)),
                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                    className: "flex-1 min-w-0",
                    children: [
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("p", {
                            className: "text-sm text-gray-800 leading-snug",
                            children: [
                                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("span", {
                                    className: "font-semibold mr-1",
                                    children: displayName
                                }, void 0, false, {
                                    fileName: "[project]/src/contexts/NotificationContext.tsx",
                                    lineNumber: 131,
                                    columnNumber: 25
                                }, ("TURBOPACK compile-time value", void 0)),
                                notification.message || "đã gửi một thông báo cho bạn."
                            ]
                        }, void 0, true, {
                            fileName: "[project]/src/contexts/NotificationContext.tsx",
                            lineNumber: 130,
                            columnNumber: 21
                        }, ("TURBOPACK compile-time value", void 0)),
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("p", {
                            className: "text-xs text-gray-400 mt-1",
                            children: formatTime(notification.createdDate)
                        }, void 0, false, {
                            fileName: "[project]/src/contexts/NotificationContext.tsx",
                            lineNumber: 136,
                            columnNumber: 21
                        }, ("TURBOPACK compile-time value", void 0)),
                        notification.postId && /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("p", {
                            className: "text-xs text-sky-600 font-medium mt-1",
                            children: "Click để xem →"
                        }, void 0, false, {
                            fileName: "[project]/src/contexts/NotificationContext.tsx",
                            lineNumber: 142,
                            columnNumber: 25
                        }, ("TURBOPACK compile-time value", void 0))
                    ]
                }, void 0, true, {
                    fileName: "[project]/src/contexts/NotificationContext.tsx",
                    lineNumber: 128,
                    columnNumber: 17
                }, ("TURBOPACK compile-time value", void 0)),
                /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("button", {
                    onClick: (e)=>{
                        e.stopPropagation();
                        onClose(notification.id);
                    },
                    className: "ml-1 text-gray-400 hover:text-gray-600",
                    children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$lucide$2d$react$2f$dist$2f$esm$2f$icons$2f$x$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__$3c$export__default__as__X$3e$__["X"], {
                        size: 14
                    }, void 0, false, {
                        fileName: "[project]/src/contexts/NotificationContext.tsx",
                        lineNumber: 156,
                        columnNumber: 21
                    }, ("TURBOPACK compile-time value", void 0))
                }, void 0, false, {
                    fileName: "[project]/src/contexts/NotificationContext.tsx",
                    lineNumber: 149,
                    columnNumber: 17
                }, ("TURBOPACK compile-time value", void 0))
            ]
        }, void 0, true, {
            fileName: "[project]/src/contexts/NotificationContext.tsx",
            lineNumber: 116,
            columnNumber: 13
        }, ("TURBOPACK compile-time value", void 0))
    }, void 0, false, {
        fileName: "[project]/src/contexts/NotificationContext.tsx",
        lineNumber: 112,
        columnNumber: 9
    }, ("TURBOPACK compile-time value", void 0));
};
_s1(MessageToast, "ehgiCZDTO7/vL5DUtjwH2+bK9Qk=");
_c = MessageToast;
const NotificationProvider = ({ children })=>{
    _s2();
    const [notifications, setNotifications] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])([]);
    const [mode, setMode] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])("toast");
    const [notificationCounter, setNotificationCounter] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])(0);
    const router = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$navigation$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useRouter"])();
    const addNotification = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useCallback"])({
        "NotificationProvider.useCallback[addNotification]": (notificationData)=>{
            console.log("🔔 Adding notification:", notificationData);
            setNotifications({
                "NotificationProvider.useCallback[addNotification]": (prev)=>{
                    if (!notificationData?.id) {
                        console.warn("⚠️ Notification missing ID");
                        return prev;
                    }
                    const exists = prev.some({
                        "NotificationProvider.useCallback[addNotification].exists": (n)=>n.id === notificationData.id
                    }["NotificationProvider.useCallback[addNotification].exists"]);
                    if (exists) {
                        console.log("⚠️ Notification already exists:", notificationData.id);
                        return prev;
                    }
                    const fullName = [
                        notificationData.senderFirstName,
                        notificationData.senderLastName
                    ].filter(Boolean).join(" ");
                    const senderName = fullName || notificationData.senderName || "Unknown User";
                    const notification = {
                        id: notificationData.id,
                        senderFirstName: notificationData.senderFirstName,
                        senderLastName: notificationData.senderLastName,
                        senderName,
                        avatar: notificationData.avatar || notificationData.senderImageUrl,
                        message: notificationData.content || notificationData.message,
                        createdDate: notificationData.createdDate || notificationData.timestamp || new Date().toISOString(),
                        type: notificationData.type || "general",
                        seen: notificationData.seen ?? notificationData.read ?? false,
                        postId: notificationData.postId,
                        senderId: notificationData.senderId
                    };
                    playNotificationSound();
                    if (typeof document !== 'undefined' && document.hidden) showBrowserNotification(notification);
                    setNotificationCounter({
                        "NotificationProvider.useCallback[addNotification]": (c)=>c + 1
                    }["NotificationProvider.useCallback[addNotification]"]);
                    console.log("✅ Notification added to state");
                    return [
                        notification,
                        ...prev
                    ];
                }
            }["NotificationProvider.useCallback[addNotification]"]);
        }
    }["NotificationProvider.useCallback[addNotification]"], []);
    const setNotificationsList = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useCallback"])({
        "NotificationProvider.useCallback[setNotificationsList]": (list)=>{
            setNotifications(list);
        }
    }["NotificationProvider.useCallback[setNotificationsList]"], []);
    const removeNotification = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useCallback"])({
        "NotificationProvider.useCallback[removeNotification]": (notificationId)=>{
            setNotifications({
                "NotificationProvider.useCallback[removeNotification]": (prev)=>prev.filter({
                        "NotificationProvider.useCallback[removeNotification]": (n)=>n.id !== notificationId
                    }["NotificationProvider.useCallback[removeNotification]"])
            }["NotificationProvider.useCallback[removeNotification]"]);
        }
    }["NotificationProvider.useCallback[removeNotification]"], []);
    const handleNotificationClick = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useCallback"])({
        "NotificationProvider.useCallback[handleNotificationClick]": (notification)=>{
            if (notification.onClick) {
                notification.onClick(notification);
            }
            if (notification.postId) {
                router.push(`/post/${notification.postId}`);
            }
            removeNotification(notification.id);
        }
    }["NotificationProvider.useCallback[handleNotificationClick]"], [
        router,
        removeNotification
    ]);
    const value = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useMemo"])({
        "NotificationProvider.useMemo[value]": ()=>({
                notifications,
                addNotification,
                removeNotification,
                setNotificationsList,
                setMode,
                mode,
                notificationCounter
            })
    }["NotificationProvider.useMemo[value]"], [
        notifications,
        addNotification,
        removeNotification,
        setNotificationsList,
        mode,
        notificationCounter
    ]);
    return /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(NotificationContext.Provider, {
        value: value,
        children: [
            children,
            /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                className: "fixed top-4 right-4 z-[9999] space-y-3 max-h-screen overflow-y-auto pointer-events-none",
                children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])("div", {
                    className: "pointer-events-auto",
                    children: mode === "toast" && notifications.map((notification)=>/*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(MessageToast, {
                            notification: notification,
                            onClose: removeNotification,
                            onClick: handleNotificationClick
                        }, notification.id, false, {
                            fileName: "[project]/src/contexts/NotificationContext.tsx",
                            lineNumber: 256,
                            columnNumber: 29
                        }, ("TURBOPACK compile-time value", void 0)))
                }, void 0, false, {
                    fileName: "[project]/src/contexts/NotificationContext.tsx",
                    lineNumber: 253,
                    columnNumber: 17
                }, ("TURBOPACK compile-time value", void 0))
            }, void 0, false, {
                fileName: "[project]/src/contexts/NotificationContext.tsx",
                lineNumber: 252,
                columnNumber: 13
            }, ("TURBOPACK compile-time value", void 0))
        ]
    }, void 0, true, {
        fileName: "[project]/src/contexts/NotificationContext.tsx",
        lineNumber: 250,
        columnNumber: 9
    }, ("TURBOPACK compile-time value", void 0));
};
_s2(NotificationProvider, "CmQKhUBb2/f5yTDbWrGfOoo78OE=", false, function() {
    return [
        __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$navigation$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useRouter"]
    ];
});
_c1 = NotificationProvider;
// ===== Helpers =====
const formatTime = (dateString)=>{
    if (!dateString) return "Vừa xong";
    const date = new Date(dateString);
    const now = new Date();
    const diff = Math.floor((now.getTime() - date.getTime()) / 1000);
    if (diff < 60) return "Vừa xong";
    if (diff < 3600) return `${Math.floor(diff / 60)} phút trước`;
    if (diff < 86400) return `${Math.floor(diff / 3600)} giờ trước`;
    return date.toLocaleTimeString("vi-VN", {
        hour: "2-digit",
        minute: "2-digit",
        day: "2-digit",
        month: "2-digit"
    });
};
const playNotificationSound = ()=>{
    if ("TURBOPACK compile-time falsy", 0) //TURBOPACK unreachable
    ;
    try {
        const audio = new Audio("/notification.mp3");
        audio.play().catch(()=>{});
    } catch  {
    // Ignore audio errors
    }
};
const showBrowserNotification = (notification)=>{
    if ("TURBOPACK compile-time falsy", 0) //TURBOPACK unreachable
    ;
    if ("Notification" in window && Notification.permission === "granted") {
        const n = new Notification(notification.senderName, {
            body: notification.message,
            icon: notification.avatar
        });
        setTimeout(()=>n.close(), 5000);
    }
};
var _c, _c1;
__turbopack_context__.k.register(_c, "MessageToast");
__turbopack_context__.k.register(_c1, "NotificationProvider");
if (typeof globalThis.$RefreshHelpers$ === 'object' && globalThis.$RefreshHelpers !== null) {
    __turbopack_context__.k.registerExports(__turbopack_context__.m, globalThis.$RefreshHelpers$);
}
}),
"[project]/src/contexts/NotificationSocketContext.tsx [app-client] (ecmascript)", ((__turbopack_context__) => {
"use strict";

__turbopack_context__.s([
    "NotificationSocketProvider",
    ()=>NotificationSocketProvider,
    "useNotificationSocket",
    ()=>useNotificationSocket
]);
var __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/node_modules/next/dist/compiled/react/jsx-dev-runtime.js [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/node_modules/next/dist/compiled/react/index.js [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$lib$2f$utils$2f$auth$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/lib/utils/auth.ts [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$contexts$2f$NotificationContext$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/contexts/NotificationContext.tsx [app-client] (ecmascript)");
;
var _s = __turbopack_context__.k.signature(), _s1 = __turbopack_context__.k.signature();
'use client';
;
;
;
const NotificationSocketContext = /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["createContext"])(null);
const NotificationSocketProvider = ({ children })=>{
    _s();
    const stompClientRef = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useRef"])(null);
    const [isConnected, setIsConnected] = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useState"])(false);
    const { addNotification } = (0, __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$contexts$2f$NotificationContext$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useNotification"])();
    (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useEffect"])({
        "NotificationSocketProvider.useEffect": ()=>{
            if ("TURBOPACK compile-time falsy", 0) //TURBOPACK unreachable
            ;
            const token = (0, __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$lib$2f$utils$2f$auth$2e$ts__$5b$app$2d$client$5d$__$28$ecmascript$29$__["getToken"])();
            if (!token) {
                console.log("📵 No token, skipping notification socket connection");
                return;
            }
            let client = null;
            // Lazy load sockjs-client and @stomp/stompjs to prevent module-level crash
            const initSocket = {
                "NotificationSocketProvider.useEffect.initSocket": async ()=>{
                    try {
                        console.log("🔌 Loading notification socket libraries...");
                        // Dynamic imports to prevent blank page issue
                        const [{ Client }, { default: SockJS }] = await Promise.all([
                            __turbopack_context__.A("[project]/node_modules/@stomp/stompjs/esm6/index.js [app-client] (ecmascript, async loader)"),
                            __turbopack_context__.A("[project]/node_modules/sockjs-client/lib/entry.js [app-client] (ecmascript, async loader)")
                        ]);
                        // Decode token to get userId
                        const payload = token.split('.')[1];
                        const decoded = JSON.parse(atob(payload));
                        const userId = decoded.sub;
                        console.log("👤 Connecting notification socket for userId:", userId);
                        client = new Client({
                            webSocketFactory: {
                                "NotificationSocketProvider.useEffect.initSocket": ()=>new SockJS(`http://localhost:8082/notification/ws-notification?token=${token}`)
                            }["NotificationSocketProvider.useEffect.initSocket"],
                            connectHeaders: {
                                Authorization: `Bearer ${token}`
                            },
                            reconnectDelay: 5000,
                            debug: {
                                "NotificationSocketProvider.useEffect.initSocket": (str)=>console.log("[STOMP]", str)
                            }["NotificationSocketProvider.useEffect.initSocket"],
                            onConnect: {
                                "NotificationSocketProvider.useEffect.initSocket": ()=>{
                                    console.log("✅ Connected to /ws-notification");
                                    setIsConnected(true);
                                    client?.subscribe(`/user/${userId}/notification`, {
                                        "NotificationSocketProvider.useEffect.initSocket": (message)=>{
                                            try {
                                                const data = JSON.parse(message.body);
                                                console.log("🔔 Realtime notification received:", data);
                                                addNotification({
                                                    id: data.id,
                                                    senderName: data.senderName,
                                                    message: data.content,
                                                    avatar: data.senderImageUrl,
                                                    createdDate: data.timestamp,
                                                    type: data.type || "general",
                                                    postId: data.postId,
                                                    seen: false
                                                });
                                            } catch (e) {
                                                console.error("❌ Failed to parse notification message:", e);
                                            }
                                        }
                                    }["NotificationSocketProvider.useEffect.initSocket"]);
                                }
                            }["NotificationSocketProvider.useEffect.initSocket"],
                            onStompError: {
                                "NotificationSocketProvider.useEffect.initSocket": (frame)=>{
                                    console.error("❌ STOMP Error:", frame.headers["message"]);
                                    setIsConnected(false);
                                }
                            }["NotificationSocketProvider.useEffect.initSocket"],
                            onDisconnect: {
                                "NotificationSocketProvider.useEffect.initSocket": ()=>{
                                    console.log("📴 Disconnected from notification socket");
                                    setIsConnected(false);
                                }
                            }["NotificationSocketProvider.useEffect.initSocket"]
                        });
                        client.activate();
                        stompClientRef.current = client;
                    } catch (error) {
                        console.error("❌ Failed to initialize notification socket:", error);
                    // Don't crash the app - just log the error
                    }
                }
            }["NotificationSocketProvider.useEffect.initSocket"];
            initSocket();
            return ({
                "NotificationSocketProvider.useEffect": ()=>{
                    if (client) {
                        try {
                            client.deactivate();
                        } catch (e) {
                            console.error("Error deactivating notification socket:", e);
                        }
                    }
                }
            })["NotificationSocketProvider.useEffect"];
        }
    }["NotificationSocketProvider.useEffect"], [
        addNotification
    ]);
    return /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(NotificationSocketContext.Provider, {
        value: stompClientRef.current,
        children: children
    }, void 0, false, {
        fileName: "[project]/src/contexts/NotificationSocketContext.tsx",
        lineNumber: 133,
        columnNumber: 9
    }, ("TURBOPACK compile-time value", void 0));
};
_s(NotificationSocketProvider, "UY6expGlUBulVQO8L+vD0ktDue0=", false, function() {
    return [
        __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$contexts$2f$NotificationContext$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useNotification"]
    ];
});
_c = NotificationSocketProvider;
const useNotificationSocket = ()=>{
    _s1();
    return (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$index$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["useContext"])(NotificationSocketContext);
};
_s1(useNotificationSocket, "gDsCjeeItUuvgOWf1v4qoK9RF6k=");
var _c;
__turbopack_context__.k.register(_c, "NotificationSocketProvider");
if (typeof globalThis.$RefreshHelpers$ === 'object' && globalThis.$RefreshHelpers !== null) {
    __turbopack_context__.k.registerExports(__turbopack_context__.m, globalThis.$RefreshHelpers$);
}
}),
"[project]/src/components/providers/ClientProviders.tsx [app-client] (ecmascript)", ((__turbopack_context__) => {
"use strict";

__turbopack_context__.s([
    "ClientProviders",
    ()=>ClientProviders
]);
var __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/node_modules/next/dist/compiled/react/jsx-dev-runtime.js [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f40$chakra$2d$ui$2f$react$2f$dist$2f$esm$2f$styled$2d$system$2f$provider$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/node_modules/@chakra-ui/react/dist/esm/styled-system/provider.js [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f40$chakra$2d$ui$2f$react$2f$dist$2f$esm$2f$styled$2d$system$2f$system$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/node_modules/@chakra-ui/react/dist/esm/styled-system/system.js [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f40$chakra$2d$ui$2f$react$2f$dist$2f$esm$2f$preset$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/node_modules/@chakra-ui/react/dist/esm/preset.js [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$contexts$2f$SocketContext$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/contexts/SocketContext.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$contexts$2f$NotificationContext$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/contexts/NotificationContext.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$src$2f$contexts$2f$NotificationSocketContext$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/src/contexts/NotificationSocketContext.tsx [app-client] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$react$2d$hot$2d$toast$2f$dist$2f$index$2e$mjs__$5b$app$2d$client$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/node_modules/react-hot-toast/dist/index.mjs [app-client] (ecmascript)");
'use client';
;
;
;
;
;
;
const system = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f40$chakra$2d$ui$2f$react$2f$dist$2f$esm$2f$styled$2d$system$2f$system$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["createSystem"])(__TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f40$chakra$2d$ui$2f$react$2f$dist$2f$esm$2f$preset$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["defaultConfig"]);
function ClientProviders({ children }) {
    return /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f40$chakra$2d$ui$2f$react$2f$dist$2f$esm$2f$styled$2d$system$2f$provider$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["ChakraProvider"], {
        value: system,
        children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$contexts$2f$SocketContext$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["SocketProvider"], {
            children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$contexts$2f$NotificationContext$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["NotificationProvider"], {
                children: /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$src$2f$contexts$2f$NotificationSocketContext$2e$tsx__$5b$app$2d$client$5d$__$28$ecmascript$29$__["NotificationSocketProvider"], {
                    children: [
                        children,
                        /*#__PURE__*/ (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$next$2f$dist$2f$compiled$2f$react$2f$jsx$2d$dev$2d$runtime$2e$js__$5b$app$2d$client$5d$__$28$ecmascript$29$__["jsxDEV"])(__TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f$react$2d$hot$2d$toast$2f$dist$2f$index$2e$mjs__$5b$app$2d$client$5d$__$28$ecmascript$29$__["Toaster"], {
                            position: "top-right",
                            toastOptions: {
                                duration: 5000,
                                style: {
                                    borderRadius: '12px',
                                    background: '#fff',
                                    color: '#333',
                                    boxShadow: '0 10px 40px rgba(0, 0, 0, 0.1)'
                                }
                            }
                        }, void 0, false, {
                            fileName: "[project]/src/components/providers/ClientProviders.tsx",
                            lineNumber: 23,
                            columnNumber: 25
                        }, this)
                    ]
                }, void 0, true, {
                    fileName: "[project]/src/components/providers/ClientProviders.tsx",
                    lineNumber: 21,
                    columnNumber: 21
                }, this)
            }, void 0, false, {
                fileName: "[project]/src/components/providers/ClientProviders.tsx",
                lineNumber: 20,
                columnNumber: 17
            }, this)
        }, void 0, false, {
            fileName: "[project]/src/components/providers/ClientProviders.tsx",
            lineNumber: 19,
            columnNumber: 13
        }, this)
    }, void 0, false, {
        fileName: "[project]/src/components/providers/ClientProviders.tsx",
        lineNumber: 18,
        columnNumber: 9
    }, this);
}
_c = ClientProviders;
var _c;
__turbopack_context__.k.register(_c, "ClientProviders");
if (typeof globalThis.$RefreshHelpers$ === 'object' && globalThis.$RefreshHelpers !== null) {
    __turbopack_context__.k.registerExports(__turbopack_context__.m, globalThis.$RefreshHelpers$);
}
}),
]);

//# sourceMappingURL=src_5b993109._.js.map