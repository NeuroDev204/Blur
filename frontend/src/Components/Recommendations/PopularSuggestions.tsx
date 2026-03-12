import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { fetchPopularRecommendations, followUser, RecommendationUser } from '../../api/userApi';

const PopularSuggestions: React.FC = () => {
    const [users, setUsers] = useState<RecommendationUser[]>([]);
    const [loading, setLoading] = useState(true);
    const [following, setFollowing] = useState<Set<string>>(new Set());
    const navigate = useNavigate();

    useEffect(() => {
        fetchPopularRecommendations(0, 0, 8)
            .then(res => setUsers(res?.content || []))
            .catch(() => setUsers([]))
            .finally(() => setLoading(false));
    }, []);

    const handleFollow = async (user: RecommendationUser) => {
        try {
            await followUser(user.userId);
            setFollowing(prev => new Set(prev).add(user.id));
        } catch {
            // silent
        }
    };

    const formatCount = (n?: number) => {
        if (!n) return '0';
        if (n >= 1000) return `${(n / 1000).toFixed(1)}k`;
        return String(n);
    };

    if (loading) {
        return (
            <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-4">
                <div className="h-4 bg-gray-100 rounded w-32 mb-4 animate-pulse" />
                {Array.from({ length: 5 }).map((_, i) => (
                    <div key={i} className="flex items-center gap-3 mb-4">
                        <div className="w-10 h-10 rounded-full bg-gray-100 animate-pulse flex-shrink-0" />
                        <div className="flex-1">
                            <div className="h-3 bg-gray-100 rounded w-24 mb-1 animate-pulse" />
                            <div className="h-3 bg-gray-100 rounded w-16 animate-pulse" />
                        </div>
                        <div className="w-14 h-7 bg-gray-100 rounded-full animate-pulse" />
                    </div>
                ))}
            </div>
        );
    }

    if (!users.length) return null;

    return (
        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-4">
            <h3 className="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-4">
                Suggested for you
            </h3>

            <div className="space-y-3">
                {users.map(user => {
                    const isFollowed = following.has(user.id);
                    const fullName = `${user.firstName} ${user.lastName}`.trim();

                    return (
                        <div key={user.id} className="flex items-center gap-3">
                            <img
                                src={user.imageUrl || 'https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_640.png'}
                                alt={fullName}
                                className="w-10 h-10 rounded-full object-cover flex-shrink-0 cursor-pointer border border-gray-100"
                                onClick={() => navigate(`/profile/user?profileId=${user.id}`)}
                            />

                            <div
                                className="flex-1 min-w-0 cursor-pointer"
                                onClick={() => navigate(`/profile/user?profileId=${user.id}`)}
                            >
                                <p className="text-sm font-semibold text-gray-800 truncate">{fullName}</p>
                                <p className="text-xs text-gray-400 truncate">
                                    {formatCount(user.followerCount)} followers
                                    {user.city ? ` · ${user.city}` : ''}
                                </p>
                            </div>

                            <button
                                onClick={() => handleFollow(user)}
                                disabled={isFollowed}
                                className={`text-xs font-semibold px-3 py-1.5 rounded-full transition-colors flex-shrink-0 ${
                                    isFollowed
                                        ? 'bg-gray-100 text-gray-400 cursor-default'
                                        : 'bg-sky-50 text-sky-500 hover:bg-sky-100'
                                }`}
                            >
                                {isFollowed ? 'Đã follow' : 'Follow'}
                            </button>
                        </div>
                    );
                })}
            </div>
        </div>
    );
};

export default PopularSuggestions;
