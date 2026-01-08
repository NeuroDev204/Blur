import React, { useState, useRef, useEffect } from "react";
import {
  BsBookmark,
  BsBookmarkFill,
  BsEmojiSmile,
  BsThreeDots,
} from "react-icons/bs";
import { AiFillHeart, AiOutlineHeart } from "react-icons/ai";
import { FaRegComment } from "react-icons/fa";
import { RiSendPlaneLine } from "react-icons/ri";
import { MdDelete } from "react-icons/md";
import { useDisclosure, useToast } from "@chakra-ui/react";
import { Swiper, SwiperSlide } from "swiper/react";
import { Navigation, Pagination } from "swiper/modules";
import "swiper/css";
import "swiper/css/navigation";
import "swiper/css/pagination";

import CommentModal from "../Comment/CommentModal";
import { timeDifference } from "../../Config/Logic";
import {
  fetchLikePost,
  deletePost,
  likePost,
  createComment,
  getAllComments,
  replyToComment,
  savePost,
} from "../../api/postApi";
import { IoSend } from "react-icons/io5";
import { useNavigate } from "react-router-dom";

interface Like {
  userId: string;
  postId?: string;
  id?: string;
  createdAt?: string;
  [key: string]: unknown;
}

const PostCard = ({ post, user, onPostDeleted }: { post: any; user: any; onPostDeleted?: (id: string) => void }) => {
  const [showDropdown, setShowDropdown] = useState(false);
  const [isPostLiked, setIsPostLiked] = useState(false);
  const toast = useToast();
  const [isSaved, setIsSaved] = useState(false);
  const [isPlaying, setIsPlaying] = useState<Record<number, boolean>>({});
  const [progress, setProgress] = useState<Record<number, number>>({});
  const [comments, setComments] = useState<any[]>([]);
  const [likes, setLikes] = useState<Like[]>([]);
  const [isMuted, setIsMuted] = useState(false);
  const [mediaDimensions, setMediaDimensions] = useState<Record<number, { aspectRatio: number; width: number; height: number }>>({});
  const [primaryAspectRatio, setPrimaryAspectRatio] = useState<number | null>(null);
  const videoRefs = useRef<(HTMLVideoElement | null)[]>([]);
  // Token is now handled automatically by axiosClient interceptors
  const { isOpen, onOpen, onClose } = useDisclosure();
  const [comment, setComment] = useState("");
  const navigate = useNavigate();

  // Fetch likes & comments on mount
  useEffect(() => {
    if (!post?.id || !user?.id) return;

    const fetchData = async () => {
      try {
        // Fetch comments
        const allComments = await getAllComments(post.id);
        setComments(allComments);

        // Fetch likes
        const likeRes = await fetchLikePost(post.id);
        const likesArray = (Array.isArray(likeRes) ? likeRes : []) as unknown as Like[];
        setLikes(likesArray);

        // Check if current user has liked the post (try multiple userId formats)
        const possibleUserIds = [
          user.id,
          user.userId,
          user.username,
          user.sub,
        ].filter(Boolean);

        let matched = false;
        for (const uid of possibleUserIds) {
          const found = likesArray.some((likeItem) => likeItem.userId === uid);
          if (found) {
            matched = true;
            break;
          }
        }

        setIsPostLiked(matched);
      } catch (error) {
        console.error("Error fetching data:", error);
        setLikes([]);
        setComments([]);
        setIsPostLiked(false);
      }
    };

    fetchData();
  }, [post?.id, user?.id, user?.userId]);

  // 🖼️ Load image dimensions
  const handleImageLoad = (index, e) => {
    const img = e.target;
    const aspectRatio = img.naturalWidth / img.naturalHeight;

    setMediaDimensions((prev) => ({
      ...prev,
      [index]: {
        aspectRatio,
        width: img.naturalWidth,
        height: img.naturalHeight,
      },
    }));

    if (index === 0 && primaryAspectRatio === null) {
      setPrimaryAspectRatio(aspectRatio);
    }
  };

  // 🎥 Load video dimensions
  const handleVideoLoad = (index, e) => {
    const video = e.target;
    const aspectRatio = video.videoWidth / video.videoHeight;

    setMediaDimensions((prev) => ({
      ...prev,
      [index]: {
        aspectRatio,
        width: video.videoWidth,
        height: video.videoHeight,
      },
    }));

    if (index === 0 && primaryAspectRatio === null) {
      setPrimaryAspectRatio(aspectRatio);
    }
  };

  // 🟥 Delete Post
  const handleDeletePost = async () => {
    toast({
      title: "Delete Post",
      description: "Are you sure you want to delete this post?",
      status: "warning",
      duration: null,
      position: "top",
      isClosable: true,
      render: ({ onClose }) => (
        <div className="bg-white rounded-lg shadow-xl border border-sky-200 p-6 max-w-md">
          <div className="flex items-start gap-4">
            <div className="flex-shrink-0 w-10 h-10 rounded-full bg-amber-100 flex items-center justify-center">
              <svg
                className="w-6 h-6 text-amber-600"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth="2"
                  d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
                />
              </svg>
            </div>
            <div className="flex-1">
              <h3 className="text-lg font-semibold text-gray-900 mb-2">
                Delete Post
              </h3>
              <p className="text-sm text-gray-600 mb-4">
                Are you sure you want to delete this post? This action cannot be
                undone.
              </p>
              <div className="flex gap-3 justify-end">
                <button
                  onClick={onClose}
                  className="px-4 py-2 text-sm font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-lg"
                >
                  Cancel
                </button>
                <button
                  onClick={async () => {
                    onClose();
                    try {
                      await deletePost(post.id);
                      toast({
                        title: "Post deleted successfully",
                        status: "success",
                        duration: 3000,
                        position: "top-right",
                      });
                      setShowDropdown(false);
                      if (onPostDeleted) onPostDeleted(post.id);
                    } catch (error) {
                      toast({
                        title: "Failed to delete post",
                        description: (error as Error).message || "Something went wrong",
                        status: "error",
                        duration: 3000,
                        position: "top-right",
                      });
                    }
                  }}
                  className="px-4 py-2 text-sm font-medium text-white bg-red-500 hover:bg-red-600 rounded-lg"
                >
                  Delete
                </button>
              </div>
            </div>
          </div>
        </div>
      ),
    });
  };

  // Create comment or reply
  const handleCreateComment = async (content, parentCommentId = null) => {
    if (!content.trim()) return;

    try {
      if (parentCommentId) {
        // Reply to existing comment
        const newReply = await replyToComment(parentCommentId, content);
        setComments((prev) => [...prev, newReply]);

        toast({
          title: "Reply created successfully.",
          status: "success",
          duration: 2000,
          position: "top-right",
        });
      } else {
        // Create root comment
        const createdComment = await createComment(post.id, content);
        setComments((prev) => [...prev, createdComment]);

        toast({
          title: "Comment created successfully.",
          status: "success",
          duration: 2000,
          position: "top-right",
        });
      }

      setComment("");
    } catch (error) {
      console.error("Error creating comment/reply:", error);

      toast({
        title: parentCommentId
          ? "Failed to create reply"
          : "Failed to create comment",
        description: (error as any).response?.data?.message || (error as Error).message,
        status: "error",
        duration: 3000,
        position: "top-right",
      });
    }
  };

  // Toggle like/unlike with optimistic update
  const handlePostLike = async () => {
    const previousLiked = isPostLiked;
    const previousLikes = [...likes];

    // Get current user ID (try multiple formats for compatibility)
    const currentUserId = user.id || user.userId || user.username || user.sub;

    try {
      // Optimistic update
      setIsPostLiked(!isPostLiked);

      if (isPostLiked) {
        setLikes((prev) =>
          prev.filter((like) => like.userId !== currentUserId)
        );
      } else {
        setLikes((prev) => [
          ...prev,
          {
            userId: currentUserId,
            postId: post.id,
            createdAt: new Date().toISOString(),
            id: `temp-${Date.now()}`,
          },
        ]);
      }

      // Call API (backend handles toggle)
      await likePost(post.id);

      // Refetch to sync with database
      const likeRes = await fetchLikePost(post.id);
      const likesArray = (Array.isArray(likeRes) ? likeRes : []) as unknown as Like[];
      setLikes(likesArray);

      // Check if user has liked with all possible IDs
      const possibleUserIds = [
        user.id,
        user.userId,
        user.username,
        user.sub,
      ].filter(Boolean);

      let matched = false;
      for (const uid of possibleUserIds) {
        if (likesArray.some((l) => l.userId === uid)) {
          matched = true;
          break;
        }
      }

      setIsPostLiked(matched);
    } catch (error) {
      console.error("Error toggling like:", error);

      // Rollback to previous state
      setIsPostLiked(previousLiked);
      setLikes(previousLikes);

      toast({
        title: "Failed to update like",
        status: "error",
        duration: 2000,
        position: "top-right",
      });

      // Refetch to ensure correct state
      try {
        const likeRes = await fetchLikePost(post.id);
        const likesArray = (Array.isArray(likeRes) ? likeRes : []) as unknown as Like[];
        setLikes(likesArray);

        const possibleUserIds = [
          user.id,
          user.userId,
          user.username,
          user.sub,
        ].filter(Boolean);

        let matched = false;
        for (const uid of possibleUserIds) {
          if (likesArray.some((l) => l.userId === uid)) {
            matched = true;
            break;
          }
        }

        setIsPostLiked(matched);
      } catch (refetchError) {
        console.error("Error refetching likes:", refetchError);
      }
    }
  };

  // 💾 Save post
  const handleSavePost = async () => {
    try {
      await savePost(post.id);

      setIsSaved(true);

      toast({
        title: "Post saved successfully",
        status: "success",
        duration: 3000,
        position: "top-right",
      });
    } catch (error) {
      console.error("Error saving post:", error);
      toast({
        title: "Failed to save post",
        status: "error",
        duration: 3000,
        position: "top-right",
      });
    }
  };

  const mediaUrls = Array.isArray(post?.mediaUrls) ? post.mediaUrls : [];
  const handleClickUserName = () =>
    navigate(`/profile/user/?profileId=${post?.profileId}`);
  const isCurrentUserPostOwner = post?.userId === (user?.userId || user?.id);

  // 🎥 Video controls
  const handleSeek = (index, value) => {
    const video = videoRefs.current[index];
    if (!video) return;
    video.currentTime = (video.duration * value) / 100;
    setProgress((prev) => ({ ...prev, [index]: value }));
  };

  const handleTimeUpdate = (index) => {
    const video = videoRefs.current[index];
    if (!video) return;
    const progressPercent = (video.currentTime / video.duration) * 100;
    setProgress((prev) => ({
      ...prev,
      [index]: progressPercent || 0,
    }));
  };

  const togglePlayPause = (index) => {
    const video = videoRefs.current[index];
    if (!video) return;
    if (isPlaying[index]) video.pause();
    else video.play();
    setIsPlaying((prev) => ({
      ...prev,
      [index]: !prev[index],
    }));
  };

  // 🎨 Get container style
  const getMediaContainerStyle = () => {
    if (primaryAspectRatio === null) {
      return {
        height: "400px",
        width: "100%",
      };
    }

    const aspectRatio = primaryAspectRatio;

    if (aspectRatio < 0.8) {
      return {
        aspectRatio: aspectRatio.toString(),
        maxHeight: "600px",
        width: "100%",
      };
    } else if (aspectRatio > 1.3) {
      return {
        aspectRatio: aspectRatio.toString(),
        maxHeight: "500px",
        width: "100%",
      };
    } else {
      return {
        aspectRatio: aspectRatio.toString(),
        maxHeight: "600px",
        width: "100%",
      };
    }
  };

  // 🎨 UI
  return (
    <div className="bg-white shadow-lg hover:shadow-2xl rounded-3xl overflow-hidden mb-8 border-2 border-sky-100 hover:border-sky-300 transition-all duration-300 transform hover:-translate-y-1">
      {/* Header */}
      <div className="flex justify-between items-center py-5 px-6 bg-gradient-to-r from-sky-50 via-white to-sky-50">
        <div className="flex items-center gap-4">
          <div
            className="relative group cursor-pointer"
            onClick={handleClickUserName}
          >
            <img
              className="relative h-14 w-14 rounded-full object-cover border-3 border-white shadow-lg ring-2 ring-sky-200"
              src={
                post?.userImageUrl ||
                "https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_640.png"
              }
              alt="User"
            />
          </div>
          <div>
            <p
              className="font-bold text-base cursor-pointer hover:text-sky-600"
              onClick={handleClickUserName}
            >
              {post?.userName ||
                `${post?.firstName || ""} ${post?.lastName || ""}`.trim() ||
                "Unknown"}
            </p>

            <p className="text-xs text-gray-500 font-medium">
              {post?.createdAt ? timeDifference(post.createdAt) : "Just now"}
            </p>
          </div>
        </div>
        <div className="relative">
          <button
            onClick={() => setShowDropdown(!showDropdown)}
            className="p-2 rounded-full hover:bg-sky-50 transition-colors duration-200"
          >
            <BsThreeDots className="text-xl text-gray-600" />
          </button>
          {showDropdown && (
            <div className="absolute top-12 right-0 bg-white shadow-xl rounded-xl py-2 z-20 min-w-[140px] border border-sky-100">
              {isCurrentUserPostOwner ? (
                <button
                  onClick={handleDeletePost}
                  className="w-full px-4 py-2.5 text-left text-sm text-red-600 hover:bg-red-50"
                >
                  <MdDelete className="inline-block mr-1" />
                  Delete
                </button>
              ) : (
                <button className="w-full px-4 py-2.5 text-left text-sm text-gray-700 hover:bg-sky-50">
                  Report
                </button>
              )}
            </div>
          )}
        </div>
      </div>

      {/* Caption */}
      {post?.content && (
        <div className="px-6 pb-4 text-sm text-gray-800 leading-relaxed">
          {post.content}
        </div>
      )}

      {/* Media */}
      {mediaUrls.length > 0 && (
        <div className="relative w-full bg-gradient-to-br from-sky-50 to-gray-50">
          <Swiper
            spaceBetween={0}
            slidesPerView={1}
            navigation
            pagination={{ clickable: true }}
            modules={[Navigation, Pagination]}
            className="post-swiper w-full"
          >
            {mediaUrls.map((url, index) => {
              const isVideo = url.match(/\.(mp4|webm|ogg)$/i);
              const containerStyle = getMediaContainerStyle();

              return (
                <SwiperSlide key={index}>
                  <div
                    className="flex justify-center items-center w-full bg-black/5 overflow-hidden"
                    style={containerStyle}
                  >
                    {isVideo ? (
                      <video
                        ref={(el) => { videoRefs.current[index] = el; }}
                        src={url}
                        className="max-w-full max-h-full w-auto h-auto object-contain"
                        loop
                        muted={isMuted}
                        onLoadedMetadata={(e) => handleVideoLoad(index, e)}
                        onTimeUpdate={() => handleTimeUpdate(index)}
                        onClick={() => togglePlayPause(index)}
                      />
                    ) : (
                      <img
                        src={url}
                        alt={`post-media-${index}`}
                        className="max-w-full max-h-full w-auto h-auto object-contain"
                        onLoad={(e) => handleImageLoad(index, e)}
                      />
                    )}
                  </div>
                </SwiperSlide>
              );
            })}
          </Swiper>
        </div>
      )}

      {/* Actions */}
      <div className="flex justify-between items-center px-6 py-4 bg-gradient-to-r from-white to-sky-50/30">
        <div className="flex items-center gap-5">
          <button
            onClick={handlePostLike}
            className="group transition-transform hover:scale-110 active:scale-95 duration-200"
          >
            {isPostLiked ? (
              <AiFillHeart className="text-2xl text-red-500 animate-pulse" />
            ) : (
              <AiOutlineHeart className="text-2xl text-gray-700 group-hover:text-red-500" />
            )}
          </button>
          <button
            onClick={onOpen}
            className="group transition-transform hover:scale-110 active:scale-95 duration-200"
          >
            <FaRegComment className="text-xl text-gray-700 group-hover:text-sky-500" />
          </button>
          <RiSendPlaneLine className="text-xl text-gray-700 group-hover:text-sky-500" />
        </div>
        <button
          onClick={handleSavePost}
          className="group transition-transform hover:scale-110 active:scale-95 duration-200"
        >
          {isSaved ? (
            <BsBookmarkFill className="text-xl text-sky-500" />
          ) : (
            <BsBookmark className="text-xl text-gray-700 group-hover:text-sky-500" />
          )}
        </button>
      </div>

      {/* Likes & Comments */}
      <div className="px-6 pb-4">
        <p className="text-sm font-semibold text-gray-800">
          {likes.length} {likes.length === 1 ? "like" : "likes"}
        </p>
        <button
          onClick={onOpen}
          className="text-sm text-gray-500 hover:text-sky-600 mt-1"
        >
          View all {comments.length}{" "}
          {comments.length === 1 ? "comment" : "comments"}
        </button>
      </div>

      {/* Add Comment */}
      <div className="border-t border-sky-100 px-6 py-4 flex items-center gap-3 bg-gradient-to-r from-sky-50/50 to-white">
        <BsEmojiSmile className="text-xl text-gray-400" />
        <input
          className="flex-1 outline-none text-sm placeholder-gray-400 bg-transparent py-2 px-1"
          type="text"
          placeholder="Add a comment..."
          value={comment}
          onChange={(e) => setComment(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter" && comment.trim()) {
              handleCreateComment(comment);
            }
          }}
        />
        {comment.trim() && (
          <IoSend
            onClick={() => handleCreateComment(comment)}
            className="text-xl text-sky-500 cursor-pointer hover:scale-110"
          />
        )}
      </div>

      <CommentModal
        user={user}
        post={post}
        comments={comments}
        postMedia={post.mediaUrls}
        likeCount={likes.length}
        isOpen={isOpen}
        onClose={onClose}
        isSaved={isSaved}
        isPostLike={isPostLiked}
        handlePostLike={handlePostLike}
        handleSavePost={handleSavePost}
        handleCreateComment={handleCreateComment}
      />
    </div>
  );
};

export default PostCard;

