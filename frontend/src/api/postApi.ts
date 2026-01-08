import axiosClient from './axiosClient'
import { ApiResponse, Post, PostData, Comment, Like, PaginatedPostsResponse } from '../types/api.types'

export const createPost = async (postData: PostData): Promise<Post> => {
    const response = await axiosClient.post<ApiResponse<Post>>('/post/create', postData)
    if (response.data?.code !== 1000) {
        throw new Error(`Create post error: ${response.data?.message || response.data?.code}`)
    }
    return response.data?.result as Post
}

export const fetchPostById = async (postId: string): Promise<Post | null> => {
    try {
        const response = await axiosClient.get<ApiResponse<Post>>(`/post/${postId}`)
        if (response.data?.code !== 1000) {
            throw new Error(`Fetch post error: ${response.data?.code}`)
        }
        return response.data.result as Post
    } catch {
        return null
    }
}

export const fetchUserPosts = async (): Promise<Post[]> => {
    const response = await axiosClient.get<ApiResponse<Post[]>>('/post/my-posts')
    if (response.data?.code !== 1000) {
        throw new Error(`Fetch error: ${response.data?.code}`)
    }
    return response.data?.result as Post[]
}

export const fetchAllPost = async (page: number = 1, limit: number = 5): Promise<PaginatedPostsResponse> => {
    try {
        const response = await axiosClient.get<ApiResponse<Post[]>>('/post/all', {
            params: { page, limit },
        })

        if (response.data?.code !== 1000) {
            throw new Error(`Fetch error: ${response.data?.code}`)
        }

        const rawResult = response.data?.result
        let posts: Post[] = []

        if (Array.isArray(rawResult)) {
            posts = rawResult
        } else if (rawResult && typeof rawResult === 'object') {
            const paginatedResult = rawResult as { content?: Post[], data?: Post[], posts?: Post[] }
            posts = paginatedResult.content || paginatedResult.data || paginatedResult.posts || []
        }

        const hasNextPage = posts.length >= limit
        return { posts, hasNextPage }
    } catch {
        return { posts: [], hasNextPage: false }
    }
}

export const fetchLikePost = async (postId: string): Promise<Like[]> => {
    const response = await axiosClient.get<ApiResponse<Like[]>>(`/post/${postId}/likes`)
    if (response.data?.code !== 1000) {
        throw new Error(`Fetch likes error: ${response.data?.code}`)
    }
    return response.data?.result as Like[]
}

export const likePost = async (postId: string): Promise<unknown> => {
    const response = await axiosClient.put(`/post/${postId}/like`, {})
    return response.data
}

export const unlikePost = async (postId: string): Promise<unknown> => {
    const response = await axiosClient.put<ApiResponse<unknown>>(`/post/${postId}/unlike`, {})
    if (response.data?.code !== 1000) {
        throw new Error(`Unlike error: ${response.data?.code}`)
    }
    return response.data?.result
}

export const toggleLikePost = async (postId: string, isCurrentlyLiked: boolean): Promise<unknown> => {
    if (isCurrentlyLiked) {
        return await unlikePost(postId)
    } else {
        return await likePost(postId)
    }
}

export const deletePost = async (postId: string): Promise<unknown> => {
    const response = await axiosClient.delete<ApiResponse<unknown>>(`/post/${postId}/delete`)
    if (response.data?.code !== 1000) {
        throw new Error(`Delete error: ${response.data?.code}`)
    }
    return response.data?.result
}

export const getPostsByUserId = async (userId: string): Promise<Post[]> => {
    try {
        const response = await axiosClient.get<ApiResponse<Post[]>>(`/post/users/posts/${userId}`)
        if (response.data?.code !== 1000) {
            throw new Error(`Fetch user posts error: ${response.data?.code}`)
        }
        return response.data?.result || []
    } catch {
        return []
    }
}

export const savePost = async (postId: string): Promise<unknown> => {
    const response = await axiosClient.post<ApiResponse<unknown>>(`/post/save/${postId}`, {})
    if (response.data?.code !== 1000) {
        throw new Error(response.data?.message || 'Save post failed')
    }
    return response.data?.result
}

export const fetchAllComments = async (postId: string): Promise<Comment[]> => {
    const response = await axiosClient.get<ApiResponse<Comment[]>>(`/post/comment/${postId}/comments`)
    if (response.data?.code !== 1000) {
        throw new Error(`Fetch comments error: ${response.data?.code}`)
    }
    return response.data?.result as Comment[]
}

export const getAllComments = async (postId: string): Promise<Comment[]> => {
    const response = await axiosClient.get<ApiResponse<Comment[]>>(`/post/comment/${postId}/all-comments`)
    return response.data?.result || []
}

export const createComment = async (postId: string, content: string): Promise<Comment> => {
    const response = await axiosClient.post<ApiResponse<Comment>>(`/post/comment/${postId}/create`, { content })
    if (response.data?.code !== 1000) {
        throw new Error(`Create comment error: ${response.data?.code}`)
    }
    return response.data?.result as Comment
}

export const replyToComment = async (parentCommentId: string, content: string): Promise<Comment> => {
    const response = await axiosClient.post<ApiResponse<Comment>>(`/post/comment/${parentCommentId}/reply`, { content })
    if (response.data?.code !== 1000) {
        throw new Error(`Reply error: ${response.data?.code}`)
    }
    return response.data?.result as Comment
}

export const deleteComment = async (commentId: string): Promise<unknown> => {
    const response = await axiosClient.delete<ApiResponse<unknown>>(`/post/comment/${commentId}/delete`)
    if (response.data?.code !== 1000) {
        throw new Error(`Delete comment error: ${response.data?.code}`)
    }
    return response.data?.result
}
