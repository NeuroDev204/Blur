import React, { useState, useEffect, useRef } from "react";
import { useParams, useLocation, useNavigate } from "react-router-dom";
import {
  fetchPostById,
  likePost,
  unlikePost,
  createComment,
  fetchLikePost,
  getAllComments,
  replyToComment,
} from "../../api/postApi";
import { fetchUserByUserId } from "../../api/userApi";
import { useToast } from "@chakra-ui/react";
import { ArrowLeft, Share2 } from "lucide-react";
import { AiFillHeart, AiOutlineHeart } from "react-icons/ai";
import { FaRegComment } from "react-icons/fa";
import { IoSend } from "react-icons/io5";
import { jwtDecode } from "jwt-decode";
import { Swiper, SwiperSlide } from "swiper/react";
import { Navigation, Pagination } from "swiper/modules";
import "swiper/css";
import "swiper/css/navigation";
import "swiper/css/pagination";

const PostDetailPage = () => {
  const { postId } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const toast = useToast();
  const token = localStorage.getItem("token");

  const [post, setPost] = useState(location.state?.post || null);
  const [postOwner, setPostOwner] = useState(null);
  const [currentUser, setCurrentUser] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isPostLiked, setIsPostLiked] = useState(false);
  const [likes, setLikes] = useState([]);
  const [comments, setComments] = useState([]);
  const [commentText, setCommentText] = useState("");
  const [isSubmittingComment, setIsSubmittingComment] = useState(false);
  const [userId, setUserId] = useState(null);

  // user info cho từng comment
  const [commentUsers, setCommentUsers] = useState({});
  // comment đang được reply
  const [replyingTo, setReplyingTo] = useState(null);
  // id comment gốc dùng để gọi API reply
  const [replyParentId, setReplyParentId] = useState(null);

  // Media
  const [mediaDimensions, setMediaDimensions] = useState({});
  const [primaryAspectRatio, setPrimaryAspectRatio] = useState(null);
  const videoRefs = useRef<(HTMLVideoElement | null)[]>([]);
  const inputRef = useRef<HTMLInputElement>(null);

  // ================== DISPLAY NAME & MENTION ==================

  interface UserLike {
    userName?: string;
    firstName?: string;
    lastName?: string;
    fullName?: string;
    name?: string;
    username?: string;
    userId?: string;
    [key: string]: unknown;
  }

  const getDisplayName = (obj: UserLike = {}, user: UserLike = {}) => {
    return (
      obj.userName ||
      [obj.firstName, obj.lastName].filter(Boolean).join(" ") ||
      [user.firstName, user.lastName].filter(Boolean).join(" ") ||
      user.fullName ||
      user.name ||
      user.username ||
      "User"
    );
  };

  const buildMention = (cmt: UserLike = {}, u: UserLike = {}) => {
    const fullName =
      cmt.userName ||
      [cmt.firstName, cmt.lastName].filter(Boolean).join(" ") ||
      u.fullName ||
      [u.firstName, u.lastName].filter(Boolean).join(" ") ||
      u.name ||
      u.username ||
      "User";

    return `@${fullName.replace(/\s+/g, "")}`;
  };

  // ================== CURRENT USER ==================
  useEffect(() => {
    if (!token) return;

    try {
      const decoded = jwtDecode(token);
      setUserId(decoded.sub);

      const fetchCurrentUser = async () => {
        try {
          const userData = await fetchUserByUserId(decoded.sub);
          setCurrentUser(userData);
        } catch (error) {
          console.error("Error fetching current user:", error);
        }
      };

      fetchCurrentUser();
    } catch (error) {
      console.error("Error decoding token:", error);
    }
  }, [token]);

  // ================== MEDIA ==================
  const handleImageLoad = (index, e) => {
    const img = e.target;
    const aspectRatio = img.naturalWidth / img.naturalHeight;
    setMediaDimensions((prev) => ({
      ...prev,
      [index]: { aspectRatio },
    }));
    if (index === 0 && primaryAspectRatio === null) {
      setPrimaryAspectRatio(aspectRatio);
    }
  };

  const handleVideoLoad = (index, e) => {
    const video = e.target;
    const aspectRatio = video.videoWidth / video.videoHeight;
    setMediaDimensions((prev) => ({
      ...prev,
      [index]: { aspectRatio },
    }));
    if (index === 0 && primaryAspectRatio === null) {
      setPrimaryAspectRatio(aspectRatio);
    }
  };

  const getMediaContainerStyle = () => {
    if (primaryAspectRatio === null) {
      return { height: "400px", width: "100%" };
    }
    return {
      aspectRatio: primaryAspectRatio.toString(),
      maxHeight: "600px",
      width: "100%",
    };
  };

  // ================== FETCH POST + COMMENTS + LIKES ==================
  useEffect(() => {
    const fetchData = async () => {
      if (!postId) return;

      try {
        setIsLoading(true);

        let postData = post;
        if (!postData) {
          postData = await fetchPostById(postId);
          setPost(postData);
        }

        const allComments = await getAllComments(postData.id);
        setComments(allComments);

        const likeRes = await fetchLikePost(postData.id);
        const likesArray = Array.isArray(likeRes) ? likeRes : [];
        setLikes(likesArray);

        if (userId) {
          const liked = likesArray.some(
            (likeItem: any) => likeItem.userId === userId
          );
          setIsPostLiked(liked);
        }
      } catch (error) {
        console.error("❌ Error fetching data:", error);
        if (!post) {
          toast({
            title: "Không thể tải bài viết",
            status: "error",
            duration: 2000,
            position: "top-right",
          });
          setTimeout(() => navigate(-1), 1500);
        }
      } finally {
        setIsLoading(false);
      }
    };

    fetchData();
  }, [postId, userId]);

  // ================== FETCH USER CHO TỪNG COMMENT ==================
  useEffect(() => {
    const fetchUsersForComments = async () => {
      if (!comments.length) return;

      console.log('📥 Fetching users for', comments.length, 'comments');

      const newUsers = {};
      await Promise.all(
        comments.map(async (cmt) => {
          if (!cmt.userId || commentUsers[cmt.userId]) return;
          try {
            console.log('🔄 Fetching user:', cmt.userId);
            const userData = await fetchUserByUserId(cmt.userId);
            console.log('✅ Fetched user:', cmt.userId, userData);
            newUsers[cmt.userId] = userData;
          } catch (err) {
            console.error("❌ Error fetching comment user:", err);
          }
        })
      );

      if (Object.keys(newUsers).length > 0) {
        console.log('💾 Updating commentUsers with:', newUsers);
        setCommentUsers((prev) => ({ ...prev, ...newUsers }));
      }
    };

    fetchUsersForComments();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [comments]);

  // ================== MAP post -> postOwner ==================
  useEffect(() => {
    if (!post) return;

    setPostOwner({
      firstName: post.firstName,
      lastName: post.lastName,
      avatar: post.userImageUrl,
      userName: post.userName,
      username: post.userName,
    });
  }, [post]);

  // ================== LIKE HANDLER ==================
  const handlePostLike = async () => {
    if (!userId) return;

    try {
      if (isPostLiked) {
        setIsPostLiked(false);
        setLikes((prev) => prev.filter((like) => like.userId !== userId));
        await unlikePost(postId);
      } else {
        setIsPostLiked(true);
        setLikes((prev) => [
          ...prev,
          {
            userId,
            postId,
            createdAt: new Date().toISOString(),
            id: `temp-${Date.now()}`,
          },
        ]);
        await likePost(postId);
      }
    } catch (error) {
      console.error("❌ Error toggling like:", error);
      try {
        const likeRes = await fetchLikePost(postId);
        setLikes(Array.isArray(likeRes) ? likeRes : []);
        setIsPostLiked(likeRes.some((l: any) => l.userId === userId));
      } catch (refetchError) {
        console.error("Error refetching likes:", refetchError);
      }
    }
  };

  // ================== COMMENT / REPLY HANDLER ==================
  const handleCreateComment = async (commentContent) => {
    if (!commentContent.trim() || isSubmittingComment) return;

    const parentCommentId = replyParentId || null;

    try {
      setIsSubmittingComment(true);

      if (parentCommentId) {
        // REPLY
        const newReply = await replyToComment(parentCommentId, commentContent);
        setComments((prev) => [...prev, newReply]);

        toast({
          title: "Đã trả lời",
          status: "success",
          duration: 2000,
          position: "top-right",
        });
      } else {
        // COMMENT GỐC
        const createdComment = await createComment(postId, commentContent);
        setComments((prev) => [...prev, createdComment]);

        toast({
          title: "Đã bình luận",
          status: "success",
          duration: 2000,
          position: "top-right",
        });
      }

      setCommentText("");
      setReplyingTo(null);
      setReplyParentId(null);
    } catch (error) {
      console.error("❌ Error creating comment:", error);
      toast({
        title: "Không thể gửi bình luận",
        status: "error",
        duration: 2000,
        position: "top-right",
      });
    } finally {
      setIsSubmittingComment(false);
    }
  };

  // ================== LOADING / NOT FOUND ==================
  if (isLoading) {
    return (
      <div className="min-h-screen bg-gradient-to-b from-gray-50 to-white flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-sky-500 mx-auto mb-4"></div>
          <p className="text-gray-500">Đang tải bài viết...</p>
        </div>
      </div>
    );
  }

  if (!post) {
    return (
      <div className="min-h-screen bg-gradient-to-b from-gray-50 to-white flex items-center justify-center">
        <div className="text-center">
          <h3 className="text-xl font-bold text-gray-800 mb-2">
            Không tìm thấy bài viết
          </h3>
          <button
            onClick={() => navigate(-1)}
            className="px-6 py-2 bg-gradient-to-r from-sky-400 to-blue-500 text-white rounded-xl font-semibold"
          >
            Quay lại
          </button>
        </div>
      </div>
    );
  }

  const mediaUrls = Array.isArray(post?.mediaUrls) ? post.mediaUrls : [];

  return (
    <div className="min-h-screen bg-gradient-to-b from-gray-50 to-white">
      {/* Header */}
      <div className="bg-white border-b sticky top-0 z-10 shadow-sm">
        <div className="max-w-3xl mx-auto px-4 py-3 flex items-center gap-4">
          <button
            onClick={() => navigate(-1)}
            className="p-2 hover:bg-gray-100 rounded-full transition-colors"
          >
            <ArrowLeft size={20} />
          </button>
          <h1 className="text-lg font-semibold">Bài viết</h1>
        </div>
      </div>

      <div className="max-w-3xl mx-auto p-4">
        <div className="bg-white rounded-2xl shadow-sm border overflow-hidden">
          {/* User Info */}
          <div className="p-4 flex items-center gap-3">
            <img
              src={
                postOwner?.avatar ||
                "https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_640.png"
              }
              alt="User"
              className="w-12 h-12 rounded-full object-cover border-2 border-gray-200"
            />
            <div className="flex-1">
              <h3 className="font-semibold text-gray-900">
                {postOwner ? getDisplayName(postOwner, postOwner) : "User"}
              </h3>

              <p className="text-xs text-gray-500">
                {post.createdAt
                  ? new Date(post.createdAt).toLocaleDateString("vi-VN", {
                    year: "numeric",
                    month: "long",
                    day: "numeric",
                    hour: "2-digit",
                    minute: "2-digit",
                  })
                  : "Recently"}
              </p>
            </div>
          </div>

          {/* Content */}
          {post.content && (
            <div className="px-4 pb-4">
              <p className="text-gray-800 text-base leading-relaxed whitespace-pre-wrap">
                {post.content}
              </p>
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
                            controls
                            onLoadedMetadata={(e) => handleVideoLoad(index, e)}
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
          <div className="px-4 pb-4 pt-2 flex items-center gap-6 border-t">
            <button
              onClick={handlePostLike}
              className="flex items-center gap-2 transition-all group"
            >
              {isPostLiked ? (
                <AiFillHeart className="text-2xl text-red-500 animate-pulse" />
              ) : (
                <AiOutlineHeart className="text-2xl text-gray-700 group-hover:text-red-500" />
              )}
              <span className="text-sm font-semibold">{likes.length}</span>
            </button>

            <button className="flex items-center gap-2 text-gray-600 hover:text-sky-500">
              <FaRegComment className="text-xl" />
              <span className="text-sm font-semibold">{comments.length}</span>
            </button>

            <button className="flex items-center gap-2 text-gray-600 hover:text-sky-500 ml-auto">
              <Share2 size={22} />
            </button>
          </div>
        </div>

        {/* Comments */}
        <div className="mt-4 bg-white rounded-2xl shadow-sm border p-6">
          <h2 className="font-semibold text-lg mb-4">
            Bình luận ({comments.length})
          </h2>

          {/* Comment Input */}
          <div className="flex gap-3 mb-6 items-center">
            <img
              src={
                currentUser?.avatar ||
                "https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_640.png"
              }
              alt="You"
              className="w-10 h-10 rounded-full object-cover"
            />
            <input
              ref={inputRef}
              type="text"
              value={commentText}
              onChange={(e) => setCommentText(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter" && commentText.trim()) {
                  handleCreateComment(commentText);
                }
              }}
              placeholder="Viết bình luận..."
              className="flex-1 px-4 py-2 bg-gray-50 border rounded-full text-sm focus:outline-none focus:ring-2 focus:ring-sky-400"
            />
            {commentText.trim() && (
              <IoSend
                onClick={() => handleCreateComment(commentText)}
                className="text-xl text-sky-500 cursor-pointer hover:scale-110"
              />
            )}
          </div>

          {/* Comments List */}
          <div className="space-y-4">
            {comments.length === 0 ? (
              <p className="text-gray-500 text-sm text-center py-8">
                Chưa có bình luận nào.
              </p>
            ) : (
              (() => {
                // Phân tách root comments và replies
                const rootComments = comments.filter((c) => !c.parentReplyId);
                const repliesMap = {};

                comments.forEach((c) => {
                  if (c.parentReplyId) {
                    if (!repliesMap[c.parentReplyId]) {
                      repliesMap[c.parentReplyId] = [];
                    }
                    repliesMap[c.parentReplyId].push(c);
                  }
                });

                return rootComments.map((comment) => {
                  const cUser = commentUsers[comment.userId] || {};
                  const displayName = getDisplayName(comment, cUser);
                  const replies = repliesMap[comment.id] || [];

                  return (
                    <div key={comment.id}>
                      {/* Root Comment */}
                      <div className="flex gap-3">
                        <img
                          src={
                            cUser.imageUrl ||
                            comment.avatar ||
                            "https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_640.png"
                          }
                          alt="User"
                          className="w-8 h-8 rounded-full object-cover"
                        />
                        <div className="flex-1">
                          <div className="bg-gray-50 rounded-2xl px-4 py-2">
                            <p className="font-semibold text-sm">{displayName}</p>
                            <p className="text-sm text-gray-700 mt-1">
                              {comment.content}
                            </p>
                          </div>

                          <div className="flex items-center gap-3 mt-1 ml-4">
                            <p className="text-xs text-gray-500">
                              {comment.createdAt
                                ? new Date(comment.createdAt).toLocaleString("vi-VN")
                                : "Vừa xong"}
                            </p>

                            <button
                              onClick={() => {
                                const mention = buildMention(comment, cUser);
                                setReplyingTo(comment);
                                setReplyParentId(comment.id);

                                setCommentText((prev) =>
                                  prev.startsWith(mention + " ") ? prev : `${mention} `
                                );
                                if (inputRef.current) {
                                  inputRef.current.focus();
                                }
                              }}
                              className="text-xs text-gray-500 hover:text-sky-600 font-semibold"
                            >
                              Reply
                            </button>
                          </div>
                        </div>
                      </div>

                      {/* Replies - THỤT LỀ */}
                      {replies.length > 0 && (
                        <div className="ml-11 mt-3 space-y-3">
                          {replies.map((reply) => {
                            const replyUser = commentUsers[reply.userId] || {};
                            const replyDisplayName = getDisplayName(reply, replyUser);

                            // Tách mention
                            const mentionMatch = reply.content.match(/^@(\S+)\s/);
                            const mention = mentionMatch ? mentionMatch[0] : "";
                            const replyContent = mention
                              ? reply.content.slice(mention.length)
                              : reply.content;

                            return (
                              <div key={reply.id} className="flex gap-3">
                                <img
                                  src={
                                    replyUser.imageUrl ||
                                    reply.avatar ||
                                    "https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_640.png"
                                  }
                                  alt="User"
                                  className="w-7 h-7 rounded-full object-cover"
                                />
                                <div className="flex-1">
                                  <div className="bg-gray-50 rounded-2xl px-4 py-2">
                                    <p className="font-semibold text-sm">
                                      {replyDisplayName}
                                    </p>
                                    <p className="text-sm text-gray-700 mt-1">
                                      {mention && (
                                        <span className="text-blue-500 font-semibold">
                                          {mention}
                                        </span>
                                      )}
                                      {replyContent}
                                    </p>
                                  </div>

                                  <div className="flex items-center gap-3 mt-1 ml-4">
                                    <p className="text-xs text-gray-500">
                                      {reply.createdAt
                                        ? new Date(reply.createdAt).toLocaleString("vi-VN")
                                        : "Vừa xong"}
                                    </p>

                                    <button
                                      onClick={() => {
                                        const mention = buildMention(reply, replyUser);
                                        setReplyingTo(reply);
                                        setReplyParentId(comment.id); // Reply to root comment

                                        setCommentText((prev) =>
                                          prev.startsWith(mention + " ")
                                            ? prev
                                            : `${mention} `
                                        );
                                        if (inputRef.current) {
                                          inputRef.current.focus();
                                        }
                                      }}
                                      className="text-xs text-gray-500 hover:text-sky-600 font-semibold"
                                    >
                                      Reply
                                    </button>
                                  </div>
                                </div>
                              </div>
                            );
                          })}
                        </div>
                      )}
                    </div>
                  );
                });
              })()
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default PostDetailPage;