import React, { useState } from "react";
import { AiFillHeart, AiOutlineHeart } from "react-icons/ai";
import { timeDifference } from "../../Config/Logic";

interface User {
    imageUrl?: string;
    firstName?: string;
    lastName?: string;
    fullName?: string;
    name?: string;
    username?: string;
}

interface Comment {
    id: string;
    content: string;
    createdAt: string;
    likeCount?: number;
    isLiked?: boolean;
    userName?: string;
    firstName?: string;
    lastName?: string;
    userId?: string;
    parentReplyId?: string | null;
}

interface CommentCardProps {
    comment: Comment;
    user: User;
    replies?: Comment[];
    replyUsers?: { [key: string]: User };
    onReply?: () => void;
    onReplyClick?: (reply: Comment) => void;
    onToggleLike?: (willLike: boolean) => void;
}

// Hàm dùng chung để lấy tên hiển thị
const getDisplayName = (obj: Partial<Comment> = {}, user: User = {}): string => {
    return (
        // 1. BE gửi sẵn userName (ưu tiên)
        obj.userName ||
        // 2. lấy firstName + lastName từ chính comment / reply
        [obj.firstName, obj.lastName].filter(Boolean).join(" ") ||
        // 3. lấy thông tin user fetch được
        [user.firstName, user.lastName].filter(Boolean).join(" ") ||
        user.fullName ||
        user.name ||
        // 4. fallback cuối cùng
        user.username ||
        "User"
    );
};

const CommentCard: React.FC<CommentCardProps> = ({
    comment,
    user,
    replies = [],
    replyUsers = {},
    onReply,
    onReplyClick,
    onToggleLike,
}) => {
    const [isCommentLike, setIsCommentLike] = useState(!!comment.isLiked);
    const [showReplies, setShowReplies] = useState(false);

    const handleClickLike = () => {
        const willLike = !isCommentLike;
        setIsCommentLike(willLike);
        onToggleLike && onToggleLike(willLike);
    };

    const displayName = getDisplayName(comment, user);

    return (
        <div className="mb-4">
            {/* Main Comment */}
            <div className="flex items-start gap-3 px-4">
                {/* Avatar */}
                <img
                    className="w-9 h-9 rounded-full object-cover flex-shrink-0"
                    src={
                        user?.imageUrl ||
                        "https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_640.png"
                    }
                    alt={displayName}
                />

                {/* Content */}
                <div className="flex-1 min-w-0">
                    {/* Comment text */}
                    <div className="flex items-start justify-between gap-3">
                        <p className="text-[13px] leading-[18px]">
                            <span className="font-semibold text-gray-900">{displayName}</span>{" "}
                            <span className="text-gray-900">{comment.content}</span>
                        </p>

                        {/* Like button */}
                        <button onClick={handleClickLike} className="flex-shrink-0 -mt-1">
                            {isCommentLike ? (
                                <AiFillHeart className="w-3 h-3 text-red-500" />
                            ) : (
                                <AiOutlineHeart className="w-3 h-3 text-gray-400" />
                            )}
                        </button>
                    </div>

                    {/* Meta info */}
                    <div className="flex items-center gap-3 mt-2">
                        <span className="text-xs text-gray-500 font-normal">
                            {timeDifference(comment.createdAt)}
                        </span>

                        {(comment.likeCount ?? 0) > 0 && (
                            <span className="text-xs text-gray-500 font-semibold">
                                {comment.likeCount} {comment.likeCount === 1 ? "like" : "likes"}
                            </span>
                        )}

                        <button
                            className="text-xs text-gray-500 font-semibold"
                            onClick={onReply}
                        >
                            Reply
                        </button>
                    </div>

                    {/* View/Hide Replies Toggle */}
                    {replies.length > 0 && (
                        <button
                            className="flex items-center gap-2 mt-3 group"
                            onClick={() => setShowReplies((p) => !p)}
                        >
                            <div className="w-6 h-[0.5px] bg-gray-400"></div>
                            <span className="text-xs font-semibold text-gray-500">
                                {showReplies
                                    ? "Hide replies"
                                    : `View replies (${replies.length})`}
                            </span>
                        </button>
                    )}
                </div>
            </div>

            {/* Replies List - THỤT LỀ RÕ RÀNG BẰNG MARGIN LEFT */}
            {showReplies && replies.length > 0 && (
                <div className="mt-3 ml-[64px] space-y-3 pr-4">
                    {replies.map((reply) => {
                        const replyUser = replyUsers[reply.userId || ''] || {};
                        const replyName = getDisplayName(reply, replyUser);

                        // Tách phần mention đầu câu (ví dụ @PhiTruong )
                        const mentionMatch = reply.content.match(/^@(\S+)\s/);
                        const mention = mentionMatch ? mentionMatch[0] : "";
                        const replyContent = mention
                            ? reply.content.slice(mention.length)
                            : reply.content;

                        return (
                            <div key={reply.id} className="flex items-start gap-3">
                                <img
                                    className="w-6 h-6 rounded-full object-cover flex-shrink-0"
                                    src={
                                        replyUser.imageUrl ||
                                        "https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_640.png"
                                    }
                                    alt={replyName}
                                />

                                <div className="flex-1 min-w-0">
                                    <div className="flex items-start justify-between gap-3">
                                        <p className="text-[13px] leading-[18px]">
                                            <span className="font-semibold text-gray-900">
                                                {replyName}
                                            </span>{" "}
                                            {mention && (
                                                <span className="text-blue-500 font-normal">
                                                    {mention}
                                                </span>
                                            )}
                                            <span className="text-gray-900">{replyContent}</span>
                                        </p>

                                        <button
                                            onClick={() => {
                                                console.log("Like reply:", reply.id);
                                            }}
                                            className="flex-shrink-0 -mt-1"
                                        >
                                            {reply.isLiked ? (
                                                <AiFillHeart className="w-3 h-3 text-red-500" />
                                            ) : (
                                                <AiOutlineHeart className="w-3 h-3 text-gray-400" />
                                            )}
                                        </button>
                                    </div>

                                    <div className="flex items-center gap-3 mt-1.5">
                                        <span className="text-xs text-gray-500 font-normal">
                                            {timeDifference(reply.createdAt)}
                                        </span>

                                        {(reply.likeCount ?? 0) > 0 && (
                                            <span className="text-xs text-gray-500 font-semibold">
                                                {reply.likeCount}{" "}
                                                {reply.likeCount === 1 ? "like" : "likes"}
                                            </span>
                                        )}

                                        <button
                                            className="text-xs text-gray-500 font-semibold"
                                            onClick={() => onReplyClick && onReplyClick(reply)}
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
};

export default CommentCard;
