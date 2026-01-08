// ============= NotificationStats.tsx =============
import { Heart, MessageCircle, UserPlus, LucideIcon } from 'lucide-react';
import React from 'react';

interface Notification {
    type?: string;
    [key: string]: unknown;
}

interface NotificationStatsProps {
    notifications: Notification[];
}

interface StatItem {
    icon: LucideIcon;
    count: number;
    label: string;
    gradient: string;
    iconColor: string;
}

const NotificationStats: React.FC<NotificationStatsProps> = ({ notifications }) => {
    const stats = {
        likes: notifications.filter(n => n.type === 'like').length,
        comments: notifications.filter(n => n.type === 'comment').length,
        follows: notifications.filter(n => n.type === 'follow').length,
        messages: notifications.filter(n => n.type === 'message').length,
    };

    const statItems: StatItem[] = [
        {
            icon: Heart,
            count: stats.likes,
            label: 'Likes',
            gradient: 'from-red-100 to-red-50',
            iconColor: 'text-red-500'
        },
        {
            icon: MessageCircle,
            count: stats.comments,
            label: 'Comments',
            gradient: 'from-sky-100 to-sky-50',
            iconColor: 'text-sky-500'
        },
        {
            icon: UserPlus,
            count: stats.follows,
            label: 'Follows',
            gradient: 'from-emerald-100 to-emerald-50',
            iconColor: 'text-emerald-500'
        },
        {
            icon: MessageCircle,
            count: stats.messages,
            label: 'Messages',
            gradient: 'from-violet-100 to-violet-50',
            iconColor: 'text-violet-500'
        },
    ];

    return (
        <div className="px-6 py-5 bg-gradient-to-r from-gray-50 to-white border-b border-gray-100">
            <div className="grid grid-cols-4 gap-4">
                {statItems.map((item, index) => (
                    <div key={index} className="text-center group cursor-pointer">
                        <div className={`w-12 h-12 bg-gradient-to-br ${item.gradient} rounded-2xl flex items-center justify-center mx-auto mb-2 shadow-sm group-hover:shadow-md group-hover:scale-105 transition-all`}>
                            <item.icon size={20} className={item.iconColor} />
                        </div>
                        <p className="text-lg font-bold text-gray-900 mb-0.5">{item.count}</p>
                        <p className="text-xs text-gray-500 font-medium">{item.label}</p>
                    </div>
                ))}
            </div>
        </div>
    );
};

export default NotificationStats;
