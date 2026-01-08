// ============================================
// Common API Response Types
// ============================================

/**
 * Standard API response wrapper
 * All backend responses follow this structure
 */
export interface ApiResponse<T> {
    code: number
    message?: string
    result?: T
}

/**
 * Paginated response for list endpoints
 */
export interface PaginatedResponse<T> {
    content: T[]
    totalPages: number
    totalElements: number
    size: number
    number: number
    first: boolean
    last: boolean
}

// ============================================
// User & Profile Types
// ============================================

export interface UserProfile {
    id: string
    userId?: string
    firstName?: string
    lastName?: string
    imageUrl?: string
    bio?: string
    followers?: string[]
    following?: string[]
    city?: string
    phone?: string
    email?: string
    gender?: string
    website?: string
    address?: string
    dob?: string
    [key: string]: unknown
}

export interface UpdateProfileData {
    firstName?: string
    lastName?: string
    bio?: string
    imageUrl?: string
    [key: string]: unknown
}

// ============================================
// Post Types
// ============================================

export interface Post {
    id: string
    content?: string
    caption?: string
    mediaUrls?: string[]
    userId?: string
    profileId?: string
    userName?: string
    userImageUrl?: string
    firstName?: string
    lastName?: string
    likes?: string[]
    likeCount?: number
    commentCount?: number
    createdAt?: string
    [key: string]: unknown
}

export interface PostData {
    caption?: string
    content?: string
    mediaUrls?: string[]
    [key: string]: unknown
}

export interface PaginatedPostsResponse {
    posts: Post[]
    hasNextPage: boolean
}

// ============================================
// Comment Types
// ============================================

export interface Comment {
    id: string
    content: string
    userId?: string
    postId?: string
    parentCommentId?: string
    createdAt?: string
    firstName?: string
    lastName?: string
    userImageUrl?: string
    [key: string]: unknown
}

// ============================================
// Like Types
// ============================================

export interface Like {
    id?: string
    userId: string
    postId?: string
    createdAt?: string
    [key: string]: unknown
}

export interface LikeResponse {
    code?: number
    result?: unknown
    [key: string]: unknown
}

// ============================================
// Story Types
// ============================================

export interface Story {
    id: string
    mediaUrl?: string
    media?: string
    username?: string
    firstName?: string
    lastName?: string
    userId?: string
    likes?: string[]
    caption?: string
    createdAt?: string
    [key: string]: unknown
}

export interface StoryData {
    mediaUrl: string
    caption?: string
    [key: string]: unknown
}

// ============================================
// Notification Types
// ============================================

export interface Notification {
    id: string
    type?: string
    content?: string
    seen?: boolean
    senderId?: string
    receiverId?: string
    createdAt?: string
    [key: string]: unknown
}

// ============================================
// Chat & Message Types
// ============================================

export interface Conversation {
    id: string
    type: 'DIRECT' | 'GROUP'
    participants: UserProfile[]
    name?: string
    lastMessage?: Message
    unreadCount?: number
    createdAt?: string
    [key: string]: unknown
}

export interface ConversationData {
    type: 'DIRECT' | 'GROUP'
    participantIds: string[]
    name?: string
}

export interface Message {
    id: string
    content: string
    senderId: string
    conversationId?: string
    type?: string
    timestamp?: string
    createdAt?: string
    read?: boolean
    [key: string]: unknown
}

export interface MessageData {
    content: string
    type?: string
}

export interface PaginatedMessages {
    content: Message[]
    totalPages: number
    totalElements: number
    [key: string]: unknown
}

// ============================================
// Auth Types
// ============================================

export interface RegistrationData {
    username: string
    password: string
    email?: string
    firstName?: string
    lastName?: string
    [key: string]: unknown
}

export interface LoginData {
    username: string
    password: string
}

export interface AuthResponse {
    token: string
    authenticated?: boolean
    [key: string]: unknown
}
