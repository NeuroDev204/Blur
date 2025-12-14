/**
 * Custom hook to check if current user has liked a post.
 * Handles the complexity of userId matching across different formats.
 */
import { useMemo, useCallback } from 'react';

interface Like {
    userId: string;
    postId?: string;
    id?: string;
    createdAt?: string;
    [key: string]: unknown;
}

interface User {
    id?: string;
    userId?: string;
    username?: string;
    sub?: string;
    [key: string]: unknown;
}

/**
 * Gets all possible user IDs from a user object.
 * Different parts of the app may use different ID fields.
 */
export const getPossibleUserIds = (user: User | null): string[] => {
    if (!user) return [];
    return [
        user.id,
        user.userId,
        user.username,
        user.sub,
    ].filter(Boolean) as string[];
};

/**
 * Checks if any of the possible user IDs match the likes array.
 * @param likes - Array of Like objects
 * @param user - User object with various ID fields
 * @returns true if user has liked, false otherwise
 */
export const checkUserHasLiked = (likes: Like[], user: User | null): boolean => {
    const possibleUserIds = getPossibleUserIds(user);

    for (const uid of possibleUserIds) {
        if (likes.some((like) => like.userId === uid)) {
            return true;
        }
    }
    return false;
};

/**
 * Gets the current user ID from user object.
 * Tries multiple fields for compatibility.
 */
export const getCurrentUserId = (user: User | null): string | null => {
    if (!user) return null;
    return user.id || user.userId || user.username || user.sub || null;
};

/**
 * Hook to manage like status checking.
 * Provides memoized functions to check if user has liked a post.
 */
export const useLikeStatus = (user: User | null) => {
    const possibleUserIds = useMemo(() => getPossibleUserIds(user), [user]);
    const currentUserId = useMemo(() => getCurrentUserId(user), [user]);

    const checkHasLiked = useCallback((likes: Like[]): boolean => {
        for (const uid of possibleUserIds) {
            if (likes.some((like) => like.userId === uid)) {
                return true;
            }
        }
        return false;
    }, [possibleUserIds]);

    return {
        possibleUserIds,
        currentUserId,
        checkHasLiked,
    };
};
