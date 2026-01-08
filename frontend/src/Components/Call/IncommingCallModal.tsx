// src/components/Call/IncomingCallModal.tsx
import React, { useEffect, useState } from 'react';
import { Phone, PhoneOff, Video, PhoneIncoming } from 'lucide-react';

interface IncomingCallModalProps {
    callerName?: string;
    callerAvatar?: string;
    callType: 'VIDEO' | 'VOICE';
    onAnswer: () => void;
    onReject: () => void;
}

const IncomingCallModal: React.FC<IncomingCallModalProps> = ({
    callerName,
    callerAvatar,
    callType,
    onAnswer,
    onReject
}) => {
    const [timer, setTimer] = useState(0);

    useEffect(() => {
        const interval = setInterval(() => {
            setTimer(prev => prev + 1);
        }, 1000);

        // Auto reject after 60 seconds
        const timeout = setTimeout(() => {
            onReject();
        }, 60000);

        return () => {
            clearInterval(interval);
            clearTimeout(timeout);
        };
    }, [onReject]);

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center 
                    bg-black/70 backdrop-blur-lg animate-fadeIn">
            <div className="bg-white rounded-3xl shadow-2xl p-10 w-[420px] 
                      animate-scaleIn mx-4">

                {/* Caller Avatar */}
                <div className="text-center mb-8">
                    <div className="relative inline-block">
                        {/* Pulsing Ring Effect */}
                        <div className="absolute inset-0 rounded-full 
                            bg-blue-400 opacity-30 
                            animate-ping"></div>

                        {callerAvatar ? (
                            <img
                                src={callerAvatar}
                                alt={callerName}
                                className="relative w-32 h-32 rounded-full 
                          border-4 border-blue-100 shadow-xl
                          object-cover"
                            />
                        ) : (
                            <div className="relative w-32 h-32 rounded-full 
                              bg-blue-500
                              flex items-center justify-center
                              border-4 border-blue-100 shadow-xl">
                                <span className="text-5xl font-semibold text-white">
                                    {callerName?.charAt(0).toUpperCase() || 'N'}
                                </span>
                            </div>
                        )}
                    </div>
                </div>

                {/* Caller Info */}
                <div className="text-center mb-10">
                    <h2 className="text-2xl font-semibold text-gray-900 mb-3">
                        {callerName || 'Người dùng Blur'}
                    </h2>

                    <div className="flex items-center justify-center gap-2.5 
                          text-gray-600 mb-2">
                        {callType === 'VIDEO' ? (
                            <>
                                <Video size={20} className="text-blue-500" strokeWidth={2.5} />
                                <span className="font-medium text-base">Cuộc gọi video</span>
                            </>
                        ) : (
                            <>
                                <PhoneIncoming size={20} className="text-blue-500" strokeWidth={2.5} />
                                <span className="font-medium text-base">Cuộc gọi thoại</span>
                            </>
                        )}
                    </div>

                    <p className="text-sm text-gray-400 mt-3 font-medium">
                        Đang đổ chuông... {timer}s
                    </p>
                </div>

                {/* Action Buttons */}
                <div className="flex gap-8 justify-center">
                    {/* Reject Button */}
                    <button
                        onClick={onReject}
                        className="w-[72px] h-[72px] rounded-full 
                       bg-red-500 hover:bg-red-600
                       text-white flex items-center justify-center 
                       transition-all duration-200 
                       hover:scale-110 active:scale-95
                       shadow-xl hover:shadow-2xl"
                        title="Từ chối"
                    >
                        <PhoneOff size={32} strokeWidth={2.5} />
                    </button>

                    {/* Answer Button */}
                    <button
                        onClick={onAnswer}
                        className="w-[72px] h-[72px] rounded-full 
                       bg-blue-500 hover:bg-blue-600
                       text-white flex items-center justify-center 
                       transition-all duration-200 
                       hover:scale-110 active:scale-95
                       shadow-xl hover:shadow-2xl
                       animate-pulse"
                        title="Trả lời"
                    >
                        <Phone size={32} strokeWidth={2.5} />
                    </button>
                </div>
            </div>
        </div>
    );
};

export default IncomingCallModal;

