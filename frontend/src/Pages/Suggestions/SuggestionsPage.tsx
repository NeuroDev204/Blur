import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
    fetchMutualRecommendations,
    fetchNearbyRecommendations,
    fetchSimilarRecommendations,
    fetchPopularRecommendations,
    followUser,
    RecommendationUser,
    RecommendationPageResponse,
} from '../../api/userApi';

type FilterType = 'mutual' | 'nearby' | 'similar' | 'popular';

const FILTERS: { key: FilterType; label: string; desc: string }[] = [
    { key: 'mutual', label: 'Mutual', desc: 'People followed by those you follow' },
    { key: 'nearby', label: 'Nearby', desc: 'People in the same city' },
    { key: 'similar', label: 'Similar', desc: 'People with similar interests' },
    { key: 'popular', label: 'Popular', desc: 'Most followed accounts' },
];

const PAGE_SIZE = 12;

const SuggestionsPage: React.FC = () => {
    const navigate = useNavigate();
    const [filter, setFilter] = useState<FilterType>('mutual');
    const [data, setData] = useState<RecommendationUser[]>([]);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [loading, setLoading] = useState(false);
    const [followed, setFollowed] = useState<Set<string>>(new Set());

    useEffect(() => {
        let cancelled = false;
        const load = async () => {
            setLoading(true);
            try {
                let res: RecommendationPageResponse;
                if (filter === 'mutual') res = await fetchMutualRecommendations(page, PAGE_SIZE);
                else if (filter === 'nearby') res = await fetchNearbyRecommendations(page, PAGE_SIZE);
                else if (filter === 'similar') res = await fetchSimilarRecommendations(page, PAGE_SIZE);
                else res = await fetchPopularRecommendations(0, page, PAGE_SIZE);
                if (!cancelled) {
                    setData(res?.content || []);
                    setTotalPages(res?.totalPages || 0);
                }
            } catch {
                if (!cancelled) setData([]);
            } finally {
                if (!cancelled) setLoading(false);
            }
        };
        load();
        return () => { cancelled = true; };
    }, [filter, page]);

    const handleFilterChange = (f: FilterType) => {
        if (f === filter) return;
        setData([]);
        setPage(0);
        setFollowed(new Set());
        setFilter(f);
    };

    const handleFollow = async (user: RecommendationUser) => {
        setFollowed(prev => new Set(prev).add(user.id));
        try {
            await followUser(user.userId);
        } catch (err) {
            console.error('Follow failed:', err);
            setFollowed(prev => { const next = new Set(prev); next.delete(user.id); return next; });
        }
    };

    const handlePageChange = (p: number) => {
        setData([]);
        setPage(p);
    };

    const formatCount = (n?: number) => {
        if (!n) return '0';
        if (n >= 1000) return `${(n / 1000).toFixed(1)}k`;
        return String(n);
    };

    return (
        <div className="min-h-screen bg-gray-50 dark:bg-gray-900 px-4 py-6 max-w-4xl mx-auto">
            <h1 className="text-2xl font-bold text-gray-800 dark:text-white mb-2">People you may know</h1>
            <p className="text-sm text-gray-500 mb-6">{FILTERS.find(f => f.key === filter)?.desc}</p>

            {/* Filter tabs */}
            <div className="flex gap-2 mb-6 flex-wrap">
                {FILTERS.map(f => (
                    <button
                        key={f.key}
                        onClick={() => handleFilterChange(f.key)}
                        className={`px-4 py-2 rounded-full text-sm font-semibold transition-colors ${
                            filter === f.key
                                ? 'bg-sky-500 text-white shadow-sm'
                                : 'bg-white text-gray-600 border border-gray-200 hover:bg-gray-50 dark:bg-gray-800 dark:text-gray-300 dark:border-gray-700'
                        }`}
                    >
                        {f.label}
                    </button>
                ))}
            </div>

            {/* Grid */}
            {loading ? (
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                    {Array.from({ length: 6 }).map((_, i) => (
                        <div key={i} className="bg-white dark:bg-gray-800 rounded-2xl p-4 flex items-center gap-3 animate-pulse">
                            <div className="w-14 h-14 rounded-full bg-gray-200 dark:bg-gray-700 flex-shrink-0" />
                            <div className="flex-1 space-y-2">
                                <div className="h-3 bg-gray-200 dark:bg-gray-700 rounded w-3/4" />
                                <div className="h-3 bg-gray-200 dark:bg-gray-700 rounded w-1/2" />
                            </div>
                        </div>
                    ))}
                </div>
            ) : data.length === 0 ? (
                <div className="text-center py-16 text-gray-400">
                    <p className="text-lg font-medium">No suggestions found</p>
                    <p className="text-sm mt-1">Try a different filter</p>
                </div>
            ) : (
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                    {data.map(user => {
                        const isFollowed = followed.has(user.id);
                        const fullName = `${user.firstName} ${user.lastName}`.trim();
                        return (
                            <div key={user.id} className="bg-white dark:bg-gray-800 rounded-2xl p-4 flex items-center gap-3 shadow-sm border border-gray-100 dark:border-gray-700">
                                <img
                                    src={user.imageUrl || 'https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_640.png'}
                                    alt={fullName}
                                    className="w-14 h-14 rounded-full object-cover flex-shrink-0 cursor-pointer border border-gray-100"
                                    onClick={() => navigate(`/profile/user?profileId=${user.id}`)}
                                />
                                <div
                                    className="flex-1 min-w-0 cursor-pointer"
                                    onClick={() => navigate(`/profile/user?profileId=${user.id}`)}
                                >
                                    <p className="text-sm font-semibold text-gray-800 dark:text-white truncate">{fullName}</p>
                                    <p className="text-xs text-gray-400 truncate">
                                        {formatCount(user.followerCount)} followers
                                        {user.city ? ` · ${user.city}` : ''}
                                    </p>
                                    {user.mutualConnections && user.mutualConnections > 0 ? (
                                        <p className="text-xs text-sky-500 mt-0.5">{user.mutualConnections} mutual</p>
                                    ) : null}
                                </div>
                                <button
                                    onClick={() => !isFollowed && handleFollow(user)}
                                    className={`text-xs font-semibold px-3 py-1.5 rounded-full transition-colors flex-shrink-0 ${
                                        isFollowed
                                            ? 'bg-gray-100 text-gray-400 cursor-default dark:bg-gray-700'
                                            : 'bg-sky-50 text-sky-500 hover:bg-sky-100'
                                    }`}
                                >
                                    {isFollowed ? 'Following' : 'Follow'}
                                </button>
                            </div>
                        );
                    })}
                </div>
            )}

            {/* Pagination */}
            {totalPages > 1 && (
                <div className="flex justify-center gap-2 mt-8">
                    <button
                        onClick={() => handlePageChange(page - 1)}
                        disabled={page === 0}
                        className="px-4 py-2 rounded-full text-sm font-medium bg-white border border-gray-200 text-gray-600 disabled:opacity-40 hover:bg-gray-50"
                    >
                        Previous
                    </button>
                    <span className="px-4 py-2 text-sm text-gray-500">
                        {page + 1} / {totalPages}
                    </span>
                    <button
                        onClick={() => handlePageChange(page + 1)}
                        disabled={page >= totalPages - 1}
                        className="px-4 py-2 rounded-full text-sm font-medium bg-white border border-gray-200 text-gray-600 disabled:opacity-40 hover:bg-gray-50"
                    >
                        Next
                    </button>
                </div>
            )}
        </div>
    );
};

export default SuggestionsPage;
