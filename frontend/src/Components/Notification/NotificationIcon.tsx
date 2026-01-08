// ============= NotificationIcon.tsx =============
import { Clock, Heart, MessageCircle, UserPlus } from 'lucide-react';
import React from 'react';

type NotificationType = 'LikePost' | 'CommentPost' | 'follow' | 'message' | string;

interface NotificationIconProps {
    type: NotificationType;
    className?: string;
}

const NotificationIcon: React.FC<NotificationIconProps> = ({ type, className = '' }) => {
    const iconProps = { size: 14 };
    const baseClass = `${className} p-1.5 rounded-full border-2 border-white shadow-md`;

    switch (type) {
        case 'LikePost':
            return (
                <div className={`${baseClass} bg-gradient-to-br from-red-400 to-red-500`}>
                    <Heart {...iconProps} className="text-white fill-white" />
                </div>
            );
        case 'CommentPost':
            return (
                <div className={`${baseClass} bg-gradient-to-br from-sky-400 to-blue-500`}>
                    <MessageCircle {...iconProps} className="text-white" />
                </div>
            );
        case 'follow':
            return (
                <div className={`${baseClass} bg-gradient-to-br from-emerald-400 to-emerald-500`}>
                    <UserPlus {...iconProps} className="text-white" />
                </div>
            );
        case 'message':
            return (
                <div className={`${baseClass} bg-gradient-to-br from-violet-400 to-violet-500`}>
                    <MessageCircle {...iconProps} className="text-white" />
                </div>
            );
        default:
            return (
                <div className={`${baseClass} bg-gradient-to-br from-gray-400 to-gray-500`}>
                    <Clock {...iconProps} className="text-white" />
                </div>
            );
    }
};

export default NotificationIcon;
