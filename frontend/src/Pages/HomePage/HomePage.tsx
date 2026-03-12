import React, { useEffect, useState, useCallback, useRef } from 'react';
import StoryCircle from '../../Components/Story/StoryCircle';
import PostCard from '../../Components/Post/PostCard';
import { fetchAllPost } from '../../api/postApi';
import { fetchUserInfo } from '../../api/userApi';
import { fetchAllStories } from '../../api/storyApi';
import CreatePostModal from '../../Components/Post/CreatePostModal';
import { PostSkeleton, StorySkeleton } from '../../Components/Skeleton/Skeletons';
import PopularSuggestions from '../../Components/Recommendations/PopularSuggestions';

interface Post {
    id?: string;
    _id?: string;
    createdAt?: string;
    [key: string]: unknown;
}

interface User {
    id?: string;
    firstName?: string;
    lastName?: string;
    imageUrl?: string;
    [key: string]: unknown;
}

interface Story {
    id?: string;
    authorId?: string;
    timestamp?: string;
    createdAt?: string;
    [key: string]: unknown;
}

interface UserStory {
    authorId: string;
    stories: Story[];
    representativeStory: Story;
}

// ✅ IMPROVED: Merge và sort theo thời gian
const mergeUniqueById = (prev: Post[], incoming: Post[]): Post[] => {
    const map = new Map<string, Post>();

    // Thêm tất cả posts cũ
    prev.forEach(p => {
        const id = p.id || p._id || '';
        map.set(id, p);
    });

    // Thêm/update posts mới
    incoming.forEach(p => {
        const id = p.id || p._id || '';
        map.set(id, p);
    });

    // ✅ Sort theo createdAt (mới nhất đầu tiên)
    return Array.from(map.values()).sort((a, b) => {
        const timeA = new Date(a.createdAt || 0).getTime();
        const timeB = new Date(b.createdAt || 0).getTime();
        return timeB - timeA; // Descending order
    });
};

const HomePage: React.FC = () => {
    const [posts, setPosts] = useState<Post[]>([]);
    const [user, setUser] = useState<User | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [stories, setStories] = useState<Story[]>([]);
    const [isCreateOpen, setIsCreateOpen] = useState(false);

    // ✅ Phân trang
    const [page, setPage] = useState(1);
    const [hasMore, setHasMore] = useState(true);
    const [isLoadingMore, setIsLoadingMore] = useState(false);

    const sentinelRef = useRef<HTMLDivElement | null>(null);
    const inFlightRef = useRef(false);
    const observerRef = useRef<IntersectionObserver | null>(null); // ✅ THÊM: Ref để quản lý observer
    const isProcessingNewPostRef = useRef(false); // ✅ THÊM: Flag xử lý post mới

    // ✅ Load user + stories (1 lần duy nhất)
    const fetchData = useCallback(async () => {
        try {
            setIsLoading(true);
            const [userInfo, userStories] = await Promise.all([
                fetchUserInfo(),
                fetchAllStories(),
            ]);
            setUser(userInfo);
            setStories(userStories || []);
        } catch (error) {
        } finally {
            setIsLoading(false);
        }
    }, []);

    // ✅ CẢI TIẾN: Load bài đăng với kiểm tra conflict
    const loadMorePosts = useCallback(async () => {
        if (isLoadingMore || !hasMore || isProcessingNewPostRef.current) return;

        setIsLoadingMore(true);
        try {
            const result = await fetchAllPost(page, 5);
            const newPostsRaw = Array.isArray(result.posts) ? result.posts : [];
            const hasNextPage = result.hasNextPage;

            const newPosts = newPostsRaw.map((p: Post) => ({
                ...p,
                id: p.id || p._id,
                createdAt: p.createdAt || new Date().toISOString()
            }));

            setPosts(prev => {
                const merged = mergeUniqueById(prev, newPosts);
                return merged;
            });

            if (newPosts.length > 0) {
                setPage(prev => prev + 1);
            }
            setHasMore(Boolean(hasNextPage && newPosts.length > 0));
        } catch (error) {
        } finally {
            setIsLoadingMore(false);
        }
    }, [page, hasMore, isLoadingMore]);

    // ✅ Gọi lần đầu tiên
    useEffect(() => {
        fetchData();

        (async () => {
            setIsLoadingMore(true);
            try {
                const result = await fetchAllPost(1, 5);
                const newPostsRaw = Array.isArray(result.posts) ? result.posts : [];
                const hasNextPage = result.hasNextPage;
                const first = newPostsRaw.map((p: Post) => ({
                    ...p,
                    id: p.id || p._id,
                    createdAt: p.createdAt || new Date().toISOString()
                }));
                setPosts(first);
                setPage(2);
                setHasMore(Boolean(hasNextPage && first.length > 0));
            } catch (e) {
            } finally {
                setIsLoadingMore(false);
            }
        })();
    }, [fetchData]);

    // ✅ CẢI TIẾN: callback khi sentinel vào viewport
    const onIntersect = useCallback(async (entries: IntersectionObserverEntry[]) => {
        const [entry] = entries;
        if (!entry.isIntersecting) return;
        if (!hasMore) return;
        if (isLoadingMore) return;
        if (inFlightRef.current) return;
        if (isProcessingNewPostRef.current) return; // ✅ Chặn khi đang xử lý post mới

        try {
            inFlightRef.current = true;
            await loadMorePosts();
        } finally {
            inFlightRef.current = false;
        }
    }, [hasMore, isLoadingMore, loadMorePosts]);

    // ✅ CẢI TIẾN: tạo observer với ref để có thể disconnect
    useEffect(() => {
        if (!sentinelRef.current) return;

        const obs = new IntersectionObserver(onIntersect, {
            root: null,
            rootMargin: '300px 0px',
            threshold: 0,
        });

        observerRef.current = obs;
        obs.observe(sentinelRef.current);

        return () => {
            obs.disconnect();
            observerRef.current = null;
        };
    }, [onIntersect]);

    const handleStoryCreated = async (newStory: Story) => {
        await fetchData();
    };

    // ✅ SỬA: Loại bỏ useCallback, dùng function bình thường để tránh stale closure
    const handlePostCreated = (created: Post) => {

        if (!created) {
            return;
        }

        isProcessingNewPostRef.current = true;
        if (observerRef.current && sentinelRef.current) {
            observerRef.current.unobserve(sentinelRef.current);
        }

        // ✅ Thêm bài viết mới lên đầu feed
        setPosts((prev) => {
            const newPost = {
                ...created,
                id: created.id || created._id,
                createdAt: created.createdAt || new Date().toISOString(),
            };

            const updatedPosts = [newPost, ...prev];
            return [...updatedPosts]; // ⚡ Đảm bảo luôn trả mảng mới (force render)
        });

        // ✅ Cưỡng chế cập nhật UI để tránh React bỏ qua
        setTimeout(() => {
            if (observerRef.current && sentinelRef.current) {
                observerRef.current.observe(sentinelRef.current);
            }
            isProcessingNewPostRef.current = false;
        }, 500);
    };



    const handlePostDeleted = (deletedPostId: string) => {
        setPosts(prevPosts => prevPosts.filter(post => post.id !== deletedPostId));
    };

    // Stories grouped by user for rendering
    const renderStories = () => {
        const storiesByUser: { [key: string]: Story[] } = {};

        if (Array.isArray(stories) && stories.length > 0) {
            stories.forEach(story => {
                if (!story.authorId) return;
                if (!storiesByUser[story.authorId]) {
                    storiesByUser[story.authorId] = [];
                }
                storiesByUser[story.authorId].push(story);
            });
        }

        const usersWithStories: UserStory[] = Object.keys(storiesByUser).map(authorId => {
            const userStories = storiesByUser[authorId];
            userStories.sort((a, b) => {
                const timeA = a.timestamp || a.createdAt || '';
                const timeB = b.timestamp || b.createdAt || '';
                return new Date(timeB).getTime() - new Date(timeA).getTime();
            });

            return {
                authorId,
                stories: userStories,
                representativeStory: userStories[0]
            };
        });

        return (
            <div className="storyDiv bg-white rounded-2xl shadow-sm border border-gray-100 p-4 mb-6">
                <div className="flex space-x-3 overflow-x-auto pb-2 scrollbar-hide">
                    <StoryCircle isAddNew={true} onStoryCreated={handleStoryCreated} story={null} user={user} />

                    {isLoading ? (
                        Array.from({ length: 5 }).map((_, index) => (
                            <StorySkeleton key={`skeleton-${index}`} />
                        ))
                    ) : (
                        usersWithStories.map((userStory) => (
                            <StoryCircle
                                key={`story-${userStory.authorId}`}
                                story={userStory.representativeStory}
                                stories={userStory.stories as unknown as never[]}
                                user={user}
                                onStoryCreated={handleStoryCreated}
                            />
                        ))
                    )}
                </div>
            </div>
        );
    };

    const renderPosts = () => (
        <div className="space-y-6 w-full">
            {isLoading ? (
                Array.from({ length: 3 }).map((_, i) => <PostSkeleton key={`post-skeleton-${i}`} />)
            ) : posts.length > 0 ? (
                <>
                    {posts.map((post) => (
                        <PostCard
                            key={post.id || post._id}
                            post={post}
                            user={user}
                            onPostDeleted={handlePostDeleted}
                        />
                    ))}

                    <div ref={sentinelRef} style={{ height: 1 }} />
                </>
            ) : (
                <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-12 text-center">
                    <div className="w-24 h-24 mx-auto mb-6 rounded-full bg-gradient-to-br from-sky-100 to-blue-100 flex items-center justify-center">
                        <svg className="w-12 h-12 text-sky-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                                d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
                        </svg>
                    </div>
                    <h3 className="text-xl font-bold text-gray-800 mb-2">No posts yet</h3>
                    <p className="text-gray-500 text-sm">Start sharing your moments with friends!</p>
                </div>
            )}

            {isLoadingMore && (
                <div className="text-center text-gray-500 py-4">Đang tải thêm...</div>
            )}
            {!hasMore && posts.length > 0 && (
                <div className="text-center text-gray-400 py-4">Bạn đã xem hết bài viết</div>
            )}
        </div>
    );

    return (
        <div className="min-h-screen bg-gradient-to-b from-gray-50 to-white">
            <div className="flex w-full py-6 px-4 gap-6">
                {/* Main feed - centered with auto margins */}
                <div className="flex-1 flex justify-center">
                    <div className="w-full max-w-[620px]">
                        {renderStories()}

                        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-4 mb-6">
                            <div className="flex items-center gap-3">
                                <img
                                    src={user?.imageUrl || "https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_640.png"}
                                    alt="Your avatar"
                                    className="w-12 h-12 rounded-full object-cover border-2 border-sky-200"
                                />
                                <button
                                    onClick={() => setIsCreateOpen(true)}
                                    className="flex-1 text-left px-4 py-3 bg-gray-50 hover:bg-gray-100 rounded-full text-gray-500 transition-colors duration-200"
                                >
                                    What's on your mind, {user?.firstName || 'there'}?
                                </button>
                            </div>
                        </div>

                        {renderPosts()}
                    </div>
                </div>

                {/* Right sidebar */}
                <div className="hidden lg:block w-[280px] flex-shrink-0">
                    <div className="sticky top-6">
                        <PopularSuggestions />
                    </div>
                </div>
            </div>

            <CreatePostModal
                isOpen={isCreateOpen}
                onClose={() => setIsCreateOpen(false)}
                onPostCreate={handlePostCreated as unknown as () => void}
                user={user}
            />

            <style>{`
        .scrollbar-hide::-webkit-scrollbar {
          display: none;
        }
        .scrollbar-hide {
          -ms-overflow-style: none;
          scrollbar-width: none;
        }
      `}</style>
        </div>
    );
};

export default HomePage;
