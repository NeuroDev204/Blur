import { Dot, MoreHorizontal } from 'lucide-react';
import React, { MouseEvent } from 'react';
import NotificationIcon from './NotificationIcon';
import { timeDifference } from "../../Config/Logic";

interface Notification {
    id: string;
    senderName?: string;
    senderImageUrl?: string;
    content?: string;
    timestamp?: string;
    type?: string;
    seen?: boolean;
}

interface NotificationItemProps {
    notification: Notification;
    onMarkRead: (id: string) => void;
    onClick: (notification: Notification) => void;
}

const NotificationItem: React.FC<NotificationItemProps> = ({ notification, onMarkRead, onClick }) => {
    const { id, senderName, senderImageUrl, content, timestamp, type, seen } = notification;

    return (
        <div
            onClick={() => onClick(notification)}
            className={`group flex items-center p-4 hover:bg-sky-50/50 transition-all duration-200 cursor-pointer border-l-4 ${seen
                    ? 'border-l-transparent bg-white'
                    : 'border-l-sky-500 bg-sky-50/30'
                }`}
        >
            {/* Avatar */}
            <div className="relative mr-4 flex-shrink-0">
                <div className="relative">
                    <div className={`absolute inset-0 rounded-full blur-sm transition-opacity ${!seen
                            ? 'bg-gradient-to-br from-sky-400 to-blue-500 opacity-20'
                            : 'opacity-0'
                        }`}></div>
                    <img
                        src={
                            senderImageUrl ||
                            "https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_640.png"
                        }
                        alt={senderName || 'User'}
                        className="relative w-12 h-12 rounded-full object-cover border-2 border-white shadow-md"
                    />
                </div>
                <div className="absolute -bottom-1 -right-1">
                    <NotificationIcon type={type || 'default'} />
                </div>
            </div>

            {/* Content */}
            <div className="flex-1 min-w-0">
                <div className="flex items-start justify-between">
                    <div className="flex-1">
                        <p className="text-sm text-gray-900 leading-relaxed">
                            <span className="font-semibold hover:text-sky-600 transition-colors">
                                {senderName}
                            </span>
                            <span className="font-normal text-gray-600 ml-1">
                                {content}
                            </span>
                        </p>
                        <p className="text-xs text-sky-600 font-medium mt-1.5">
                            {timeDifference(timestamp || '')}
                        </p>
                    </div>

                    {/* Action buttons */}
                    <div className="ml-4 flex items-center gap-2 flex-shrink-0">
                        {type === 'follow' && (
                            <button className="bg-gradient-to-r from-sky-400 to-blue-500 hover:from-sky-500 hover:to-blue-600 text-white text-xs px-4 py-2 rounded-xl font-semibold transition-all shadow-sm hover:shadow-md hover:-translate-y-0.5">
                                Follow
                            </button>
                        )}
                        {type === 'message' && (
                            <button className="bg-gradient-to-r from-sky-400 to-blue-500 hover:from-sky-500 hover:to-blue-600 text-white text-xs px-4 py-2 rounded-xl font-semibold transition-all shadow-sm hover:shadow-md hover:-translate-y-0.5">
                                Reply
                            </button>
                        )}
                        <button className="p-1.5 hover:bg-sky-100 rounded-full transition-colors opacity-0 group-hover:opacity-100">
                            <MoreHorizontal size={18} className="text-gray-600" />
                        </button>
                    </div>
                </div>

                {/* Unread indicator */}
                {!seen && (
                    <button
                        onClick={(e: MouseEvent<HTMLButtonElement>) => {
                            e.stopPropagation();
                            onMarkRead(id);
                        }}
                        className="mt-2 flex items-center text-xs text-sky-600 hover:text-sky-700 font-semibold transition-colors"
                    >
                        <Dot size={18} className="text-sky-500 animate-pulse" />
                        Mark as read
                    </button>
                )}
            </div>
        </div>
    );
};

export default NotificationItem;
