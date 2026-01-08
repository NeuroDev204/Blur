// src/components/Call/CallEndedModal.tsx
import React, { useEffect } from 'react';
import { Phone, Clock, CheckCircle, XCircle, PhoneOff, LucideIcon } from 'lucide-react';

type EndReason = 'ENDED' | 'REJECTED' | 'MISSED' | 'FAILED' | 'BUSY' | 'NOT_FOUND' | 'OFFLINE';

interface CallEndedModalProps {
    callerName?: string;
    callerAvatar?: string;
    callType: 'VIDEO' | 'VOICE';
    duration: number;
    endReason: EndReason;
    errorMessage?: string;
    onClose: () => void;
}

interface StatusInfo {
    icon: LucideIcon;
    iconColor: string;
    bgColor: string;
    title: string;
    message: string;
}

const CallEndedModal: React.FC<CallEndedModalProps> = ({
    callerName,
    callerAvatar,
    callType,
    duration,
    endReason,
    errorMessage,
    onClose
}) => {
    // Auto close after 3 seconds
    useEffect(() => {
        const timer = setTimeout(() => {
            onClose();
        }, 3000);

        return () => clearTimeout(timer);
    }, [onClose]);

    // Format duration
    const formatDuration = (seconds: number): string => {
        if (!seconds || seconds === 0) return '0:00';

        const hrs = Math.floor(seconds / 3600);
        const mins = Math.floor((seconds % 3600) / 60);
        const secs = seconds % 60;

        if (hrs > 0) {
            return `${hrs}:${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
        }
        return `${mins}:${secs.toString().padStart(2, '0')}`;
    };

    // Get status info based on end reason
    const getStatusInfo = (): StatusInfo => {
        switch (endReason) {
            case 'ENDED':
                return {
                    icon: CheckCircle,
                    iconColor: 'text-green-500',
                    bgColor: 'bg-green-50',
                    title: 'Cuộc gọi đã kết thúc',
                    message: duration > 0 ? `Thời lượng: ${formatDuration(duration)}` : 'Cuộc gọi đã hoàn thành'
                };
            case 'REJECTED':
                return {
                    icon: PhoneOff,
                    iconColor: 'text-red-500',
                    bgColor: 'bg-red-50',
                    title: 'Cuộc gọi bị từ chối',
                    message: 'Người nhận đã từ chối cuộc gọi'
                };
            case 'MISSED':
                return {
                    icon: XCircle,
                    iconColor: 'text-orange-500',
                    bgColor: 'bg-orange-50',
                    title: 'Cuộc gọi nhớ',
                    message: 'Không có ai trả lời'
                };
            case 'BUSY':
                return {
                    icon: PhoneOff,
                    iconColor: 'text-orange-500',
                    bgColor: 'bg-orange-50',
                    title: 'Máy bận',
                    message: errorMessage || 'Người nhận đang trong cuộc gọi khác'
                };
            case 'NOT_FOUND':
                return {
                    icon: XCircle,
                    iconColor: 'text-red-500',
                    bgColor: 'bg-red-50',
                    title: 'Không tìm thấy',
                    message: errorMessage || 'Không tìm thấy người dùng'
                };
            case 'OFFLINE':
                return {
                    icon: XCircle,
                    iconColor: 'text-gray-500',
                    bgColor: 'bg-gray-50',
                    title: 'Người dùng offline',
                    message: errorMessage || 'Người nhận không trực tuyến'
                };
            case 'FAILED':
                return {
                    icon: XCircle,
                    iconColor: 'text-red-500',
                    bgColor: 'bg-red-50',
                    title: 'Cuộc gọi thất bại',
                    message: errorMessage || 'Không thể kết nối'
                };
            default:
                return {
                    icon: Phone,
                    iconColor: 'text-gray-500',
                    bgColor: 'bg-gray-50',
                    title: 'Cuộc gọi đã kết thúc',
                    message: errorMessage || ''
                };
        }
    };

    const statusInfo = getStatusInfo();
    const StatusIcon = statusInfo.icon;

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center 
                    bg-black/50 backdrop-blur-sm animate-fadeIn">
            <div className="bg-white rounded-3xl shadow-2xl p-8 w-[380px] 
                      animate-scaleIn mx-4">

                {/* Status Icon */}
                <div className="text-center mb-6">
                    <div className={`w-20 h-20 rounded-full ${statusInfo.bgColor} 
                          flex items-center justify-center mx-auto mb-4`}>
                        <StatusIcon size={40} className={statusInfo.iconColor} strokeWidth={2} />
                    </div>

                    <h2 className="text-2xl font-semibold text-gray-900 mb-2">
                        {statusInfo.title}
                    </h2>

                    {statusInfo.message && (
                        <p className="text-base text-gray-600 font-medium">
                            {statusInfo.message}
                        </p>
                    )}
                </div>

                {/* Caller Info */}
                <div className="flex items-center gap-4 p-4 bg-gray-50 rounded-2xl mb-6">
                    {callerAvatar ? (
                        <img
                            src={callerAvatar}
                            alt={callerName}
                            className="w-14 h-14 rounded-full object-cover border-2 border-white shadow"
                        />
                    ) : (
                        <div className="w-14 h-14 rounded-full bg-blue-500 
                            flex items-center justify-center 
                            border-2 border-white shadow">
                            <span className="text-xl font-semibold text-white">
                                {callerName?.charAt(0).toUpperCase() || 'N'}
                            </span>
                        </div>
                    )}

                    <div className="flex-1 min-w-0">
                        <p className="font-semibold text-gray-900 truncate">
                            {callerName || 'Người dùng Blur'}
                        </p>
                        <p className="text-sm text-gray-500">
                            {callType === 'VIDEO' ? 'Cuộc gọi video' : 'Cuộc gọi thoại'}
                        </p>
                    </div>

                    {duration > 0 && (
                        <div className="flex items-center gap-1.5 text-gray-600">
                            <Clock size={16} strokeWidth={2.5} />
                            <span className="text-sm font-medium font-mono">
                                {formatDuration(duration)}
                            </span>
                        </div>
                    )}
                </div>

                {/* Close Button */}
                <button
                    onClick={onClose}
                    className="w-full py-3.5 rounded-xl 
                     bg-blue-500 hover:bg-blue-600
                     text-white font-semibold text-base
                     transition-colors duration-200
                     shadow-lg hover:shadow-xl"
                >
                    Đóng
                </button>

                {/* Auto close indicator */}
                <p className="text-center text-xs text-gray-400 mt-3">
                    Tự động đóng sau 3 giây
                </p>
            </div>
        </div>
    );
};

export default CallEndedModal;

