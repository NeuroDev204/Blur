# Real-time Moderation Update Architecture

## Overview
Simplified architecture that leverages the existing **communication-service WebSocket infrastructure** instead of creating a separate WebSocket layer in content-service.

## Architecture Flow

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. User submits comment                                          │
│    ↓                                                              │
│ 2. CommentService saves with status=PENDING_MODERATION           │
│    ↓                                                              │
│ 3. ModerationProducer sends to Kafka: "comment-moderation-request"
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ AI Model Service (Python)                                        │
│ - Receives request from Kafka                                    │
│ - Processes comment with ML model                                │
│ - Publishes result to "comment-moderation-results" topic         │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ Content Service - ModerationResultConsumer                       │
│ - Kafka listener receives moderation result                      │
│ - Updates comment status in MongoDB                              │
│ - Calls CommunicationServiceClient (HTTP/Feign)                 │
│   POST /notification/moderation-update                           │
│   {userId, commentId, postId, status}                            │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ Communication Service - NotificationController                   │
│ - Receives moderation-update HTTP request                        │
│ - Injects WebSocketNotificationService                           │
│ - Calls: webSocketNotificationService.sendModerationUpdate()     │
│   (uses existing SimpMessagingTemplate)                          │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ STOMP Message Broker (in-memory, Spring)                        │
│ Destination: /user/{userId}/queue/moderation                    │
│ Payload: {commentId, postId, status, timestamp}                 │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ Frontend - React Component                                       │
│ - NotificationSocketContext (existing)                           │
│   Connects to: ws://localhost:8888/ws (API Gateway)              │
│   (Gateway routes to /chat/ws → communication-service)           │
│ - useModerationListener hook                                     │
│   Subscribes to: /user/queue/moderation                          │
│ - PostDetailPage & PostCard                                      │
│   Callbacks filter rejected comments from state                  │
│   Shows toast: "Bình luận bị ẩn"                                │
└─────────────────────────────────────────────────────────────────┘
```

## Code Changes Made

### 1. Communication Service
**File:** `communication-service/src/main/java/.../notification/controller/NotificationController.java`

Added new endpoint:
```java
@PostMapping("/moderation-update")
public ApiResponse<String> sendModerationUpdate(@RequestBody Map<String, String> request) {
    String userId = request.get("userId");
    String commentId = request.get("commentId");
    String postId = request.get("postId");
    String status = request.get("status");

    var moderationData = Map.of(
            "commentId", commentId,
            "postId", postId,
            "status", status,
            "timestamp", System.currentTimeMillis());

    webSocketNotificationService.sendModerationUpdate(userId, moderationData);
    return ApiResponse.<String>builder()
            .result("Moderation update sent")
            .build();
}
```

**File:** `communication-service/src/main/java/.../websocket/service/WebSocketNotificationService.java`

Added new method:
```java
/**
 * Push moderation update realtime den user qua STOMP
 * Client subscribe: /user/queue/moderation
 */
public void sendModerationUpdate(String userId, Object moderationData) {
    messagingTemplate.convertAndSendToUser(userId, "/queue/moderation", moderationData);
}
```

### 2. Content Service

**File:** `content-service/src/main/java/.../client/CommunicationServiceClient.java` (NEW)

Created HTTP client using Feign:
```java
@FeignClient(name = "communication-service", url = "${app.services.communication:http://localhost:8083}")
public interface CommunicationServiceClient {
    @PostMapping("/notification/moderation-update")
    Map<String, Object> sendModerationUpdate(@RequestBody Map<String, String> request);
}
```

**File:** `content-service/src/main/java/.../kafka/ModerationResultConsumer.java`

Updated to use HTTP client instead of local WebSocket:
```java
// Before: moderationNotificationService.sendModerationUpdate(...)
// After:
Map<String, String> notificationRequest = new HashMap<>();
notificationRequest.put("userId", comment.getUserId());
notificationRequest.put("commentId", commentId);
notificationRequest.put("postId", comment.getPostId());
notificationRequest.put("status", status);
try {
    communicationServiceClient.sendModerationUpdate(notificationRequest);
} catch (Exception e) {
    System.err.println("Failed to send moderation update via WebSocket: " + e.getMessage());
}
```

### 3. Frontend

**File:** `frontend/src/hooks/useModerationListener.ts`

Updated to use existing NotificationSocketContext:
```typescript
import { useNotificationSocket } from "../contexts/NotificationSocketContext"

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

    stompClient.subscribe(`/user/queue/moderation`, handleModerationMessage)
  }, [stompClient, onModerationUpdate])
}
```

**Files:** `PostDetailPage.tsx`, `PostCard.tsx`

Already integrated with useModerationListener hook that:
- Subscribes to real-time moderation updates
- Filters rejected comments from state
- Shows notification toast

## Benefits of This Approach

✅ **No Duplicate WebSocket Infrastructure** - Reuses existing communication-service setup  
✅ **Simpler Deployment** - No need for separate WebSocket config in content-service  
✅ **Better Separation of Concerns** - WebSocket messaging centralized in communication-service  
✅ **Existing Security** - WebSocket auth/JWT handling already in place  
✅ **Scalable** - Can handle multiple notification types through same WebSocket  
✅ **Fault Tolerant** - HTTP client call with error handling (doesn't fail comment moderation)

## Testing the End-to-End Flow

1. Create a comment on a post in the UI
2. Observe comment appears with status="PENDING_MODERATION" 
3. Wait for AI model to process (check model-service logs)
4. When moderation completes:
   - Backend publishes to Kafka
   - ModerationResultConsumer picks it up
   - Calls communication-service HTTP endpoint
   - STOMP message sent to `/user/{userId}/queue/moderation`
   - Frontend hook receives message
   - Comment auto-hidden if status="REJECTED"
   - Toast notification shows: "Bình luận bị ẩn"
5. No page reload needed - completely real-time

## Environment Configuration

No additional configuration needed! The existing setup already handles:
- Communication service URL: `${app.services.communication:http://localhost:8083}` (from Feign)
- WebSocket endpoint: `ws://localhost:8888/ws` (routed by API Gateway)
- STOMP subscriptions: `/user/queue/moderation` (standard messaging prefix)

## Deployment Checklist

- ✅ Communication service rebuilt with moderation endpoint
- ✅ Content service rebuilt with HTTP client
- ✅ Frontend useModerationListener updated
- ⏳ Services need restart to load new JARs
- ⏳ End-to-end test verifying real-time hiding

## Files Deleted/Disabled

The following files in content-service are now **unused** (but left in place for reference):
- `ModerationNotificationService.java` - Replaced by communication-service
- `WebSocketConfig.java` - Replaced by communication-service

These can be removed in a cleanup pass.
