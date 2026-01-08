/**
 * Skeleton loading components for the feed page.
 * Used to display loading states while fetching data.
 */
import React from 'react';

/**
 * Skeleton for a single post card during loading.
 * Mimics the layout of PostCard with animated placeholder elements.
 */
export const PostSkeleton: React.FC = () => (
    <div className="bg-white shadow-lg rounded-3xl overflow-hidden mb-8 border-2 border-gray-100 animate-pulse">
        <div className="flex items-center p-6 gap-4 bg-gradient-to-r from-gray-50 to-white">
            <div className="relative">
                <div className="w-14 h-14 bg-gradient-to-br from-sky-200 to-blue-200 rounded-full"></div>
            </div>
            <div className="flex-1">
                <div className="w-32 h-4 bg-gray-200 rounded-lg mb-2"></div>
                <div className="w-20 h-3 bg-gray-100 rounded-lg"></div>
            </div>
        </div>
        <div className="w-full h-96 bg-gradient-to-br from-sky-50 to-blue-50"></div>
        <div className="p-6">
            <div className="flex gap-5 mb-4">
                <div className="w-8 h-8 bg-gray-200 rounded-full"></div>
                <div className="w-8 h-8 bg-gray-200 rounded-full"></div>
                <div className="w-8 h-8 bg-gray-200 rounded-full"></div>
            </div>
            <div className="w-full h-3 bg-gray-100 rounded-lg mb-2"></div>
            <div className="w-3/4 h-3 bg-gray-100 rounded-lg"></div>
        </div>
    </div>
);

/**
 * Skeleton for a single story circle during loading.
 * Mimics the layout of StoryCircle with animated placeholder.
 */
export const StorySkeleton: React.FC = () => (
    <div className="flex flex-col items-center flex-shrink-0">
        <div className="w-20 h-28 rounded-2xl bg-gradient-to-br from-sky-100 to-blue-100 animate-pulse"></div>
    </div>
);
