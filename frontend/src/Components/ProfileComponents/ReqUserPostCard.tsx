import React, { useEffect, useState } from "react";
import { AiFillHeart, AiOutlineHeart } from "react-icons/ai";
import { FaComment } from "react-icons/fa";
import { MdPlayCircleOutline } from "react-icons/md";
import { fetchAllComments, fetchLikePost } from "../../api/postApi";

const ReqUserPostCard = ({ post }) => {
  const [likesCount, setLikesCount] = useState(post.likesCount || 0);
  const [commentsCount, setCommentsCount] = useState(post.commentsCount || 0);
  const [isLoading, setIsLoading] = useState(true);

  const hasMedia = post.mediaUrls && post.mediaUrls.length > 0;
  const isVideo = (url) => url.endsWith(".mp4") || url.includes("video");

  useEffect(() => {
    const fetchLikeAndComment = async () => {
      try {
        setIsLoading(true);
        const likeData = await fetchLikePost(post.id);
        setLikesCount(likeData?.length || 0);

        const commentData = await fetchAllComments(post.id);
        setCommentsCount(commentData?.length || 0);
      } catch (err) {
        console.error("Lỗi khi load likes/comments", err);
      } finally {
        setIsLoading(false);
      }
    };
    fetchLikeAndComment();
  }, [post.id]);

  if (isLoading) {
    return (
      <div className="w-full aspect-square rounded-2xl overflow-hidden bg-gradient-to-br from-sky-50 to-blue-50 animate-pulse">
        <div className="w-full h-full flex items-center justify-center">
          <div className="w-16 h-16 rounded-full bg-white/50"></div>
        </div>
      </div>
    );
  }

  return (
    <div className="group w-full aspect-square rounded-2xl overflow-hidden shadow-md bg-white transition-all duration-300 hover:shadow-xl relative">
      {hasMedia ? (
        <div className="relative w-full h-full bg-gradient-to-br from-gray-900 to-gray-800 overflow-hidden">
          {isVideo(post.mediaUrls[0]) ? (
            <>
              <video
                className="w-full h-full object-cover"
                src={post.mediaUrls[0]}
                preload="metadata"
              />
              {/* Video indicator overlay */}
              <div className="absolute inset-0 bg-black/20 group-hover:bg-black/40 transition-colors flex items-center justify-center pointer-events-none">
                <MdPlayCircleOutline className="w-16 h-16 text-white opacity-80 group-hover:opacity-100 transition-opacity" />
              </div>
            </>
          ) : (
            <img
              className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
              src={post.mediaUrls[0]}
              alt="Post media"
            />
          )}

          {/* Hover overlay with stats */}
          <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-black/40 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300 flex items-center justify-center">
            <div className="flex gap-8 text-white">
              <div className="flex items-center gap-2 transform translate-y-4 group-hover:translate-y-0 transition-transform duration-300">
                <AiFillHeart className="w-6 h-6" />
                <span className="text-lg font-bold">{likesCount}</span>
              </div>
              <div className="flex items-center gap-2 transform translate-y-4 group-hover:translate-y-0 transition-transform duration-300 delay-75">
                <FaComment className="w-6 h-6" />
                <span className="text-lg font-bold">{commentsCount}</span>
              </div>
            </div>
          </div>
        </div>
      ) : (
        // Text-only post
        <div className="relative w-full h-full bg-gradient-to-br from-sky-50 via-white to-blue-50 p-6 flex flex-col justify-between">
          {/* Content */}
          <div className="flex-1 flex items-center justify-center">
            <p className="text-gray-800 text-base leading-relaxed text-center line-clamp-6">
              {post?.content}
            </p>
          </div>

          {/* Stats footer */}
          <div className="pt-4 border-t border-gray-200 flex gap-6 justify-center">
            <div className="flex items-center gap-2 text-gray-600 group/like hover:text-red-500 transition-colors">
              <AiFillHeart className="w-5 h-5" />
              <span className="text-sm font-semibold">{likesCount}</span>
            </div>
            <div className="flex items-center gap-2 text-gray-600 group/comment hover:text-sky-500 transition-colors">
              <FaComment className="w-5 h-5" />
              <span className="text-sm font-semibold">{commentsCount}</span>
            </div>
          </div>

          {/* Decorative corner gradient */}
          <div className="absolute top-0 right-0 w-32 h-32 bg-gradient-to-br from-sky-400/10 to-blue-500/10 rounded-bl-full -z-10"></div>
          <div className="absolute bottom-0 left-0 w-24 h-24 bg-gradient-to-tr from-blue-400/10 to-sky-500/10 rounded-tr-full -z-10"></div>
        </div>
      )}
    </div>
  );
};

export default ReqUserPostCard;