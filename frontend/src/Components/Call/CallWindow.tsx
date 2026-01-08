// src/components/Call/CallWindow.tsx
import React, { useState, useEffect, RefObject } from 'react';
import {
    PhoneOff,
    Mic,
    MicOff,
    Video,
    VideoOff,
    Maximize2,
    Minimize2
} from 'lucide-react';

interface CallState {
    callType: 'VIDEO' | 'VOICE';
    status: string;
    callerName?: string;
    callerAvatar?: string;
    receiverName?: string;
    receiverAvatar?: string;
}

interface MediaState {
    isVideoEnabled: boolean;
    isAudioEnabled: boolean;
}

interface CallWindowProps {
    callState: CallState;
    mediaState: MediaState;
    connectionState: string;
    callDuration: number;
    localVideoRef: RefObject<HTMLVideoElement>;
    remoteVideoRef: RefObject<HTMLVideoElement | HTMLAudioElement>;
    onEndCall: () => void;
    onToggleAudio: () => void;
    onToggleVideo: () => void;
}

const CallWindow: React.FC<CallWindowProps> = ({
    callState,
    mediaState,
    connectionState,
    callDuration,
    localVideoRef,
    remoteVideoRef,
    onEndCall,
    onToggleAudio,
    onToggleVideo
}) => {
    const [isFullscreen, setIsFullscreen] = useState(false);
    const [showControls, setShowControls] = useState(true);

    const isVideo = callState.callType === 'VIDEO';
    const isConnected = connectionState === 'connected';

    // Xác định tên và avatar hiển thị
    const getDisplayInfo = () => {
        if (callState.callerName) {
            return {
                name: callState.callerName,
                avatar: callState.callerAvatar,
                initial: callState.callerName?.charAt(0).toUpperCase() || 'U'
            };
        }

        if (callState.receiverName) {
            return {
                name: callState.receiverName,
                avatar: callState.receiverAvatar,
                initial: callState.receiverName?.charAt(0).toUpperCase() || 'U'
            };
        }

        return {
            name: 'Không rõ',
            avatar: null,
            initial: 'U'
        };
    };

    const displayInfo = getDisplayInfo();

    // Format call duration
    const formatDuration = (seconds: number): string => {
        const hrs = Math.floor(seconds / 3600);
        const mins = Math.floor((seconds % 3600) / 60);
        const secs = seconds % 60;

        if (hrs > 0) {
            return `${hrs}:${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
        }
        return `${mins}:${secs.toString().padStart(2, '0')}`;
    };

    // Auto-hide controls after 3s in fullscreen
    useEffect(() => {
        if (!isFullscreen || !showControls) return;

        const timeout = setTimeout(() => {
            setShowControls(false);
        }, 3000);

        return () => clearTimeout(timeout);
    }, [isFullscreen, showControls]);

    // Force play remote audio
    useEffect(() => {
        const remoteElement = remoteVideoRef.current;
        if (remoteElement && remoteElement.srcObject) {
            remoteElement.play().catch(() => {
                const playOnClick = () => {
                    remoteElement?.play();
                    document.removeEventListener('click', playOnClick);
                };
                document.addEventListener('click', playOnClick);
            });
        }
    }, [connectionState, remoteVideoRef]);

    // Toggle fullscreen
    const toggleFullscreen = () => {
        setIsFullscreen(!isFullscreen);
    };

    // Connection quality indicator
    const getConnectionQuality = () => {
        switch (connectionState) {
            case 'connected':
                return { color: 'bg-green-500', text: 'Đã kết nối' };
            case 'connecting':
                return { color: 'bg-blue-400', text: 'Đang kết nối...' };
            case 'failed':
            case 'disconnected':
                return { color: 'bg-red-500', text: 'Kết nối kém' };
            default:
                if (callState.status === 'RINGING') {
                    return { color: 'bg-blue-500', text: 'Đang gọi...' };
                }
                return { color: 'bg-gray-400', text: 'Đang khởi tạo...' };
        }
    };

    const quality = getConnectionQuality();

    return (
        <div
            className={`fixed z-50 bg-[#1e293b] shadow-2xl transition-all
                  ${isFullscreen
                    ? 'inset-0'
                    : 'bottom-6 right-6 w-[420px] h-[620px] rounded-3xl overflow-hidden'
                }`}
            onMouseMove={() => setShowControls(true)}
        >

            {/* Remote Video/Audio Container */}
            <div className="relative w-full h-full bg-[#0f172a]">

                {isVideo ? (
                    <>
                        {/* Remote Video */}
                        <video
                            ref={remoteVideoRef as RefObject<HTMLVideoElement>}
                            autoPlay
                            playsInline
                            className="w-full h-full object-cover"
                        />

                        {/* Local Video (Picture-in-Picture) */}
                        <div className="absolute top-5 right-5 w-36 h-44 rounded-2xl 
                            overflow-hidden shadow-2xl border-2 border-white/10
                            transition-all hover:scale-105 bg-[#1e293b]">
                            <video
                                ref={localVideoRef}
                                autoPlay
                                playsInline
                                muted
                                className="w-full h-full object-cover scale-x-[-1]"
                            />

                            {!mediaState.isVideoEnabled && (
                                <div className="absolute inset-0 bg-[#1e293b] 
                                flex items-center justify-center">
                                    <VideoOff size={32} className="text-gray-400" />
                                </div>
                            )}
                        </div>
                    </>
                ) : (
                    /* Voice Call UI */
                    <div className="w-full h-full flex flex-col items-center 
                          justify-center text-white px-6">

                        {/* Hidden Audio Element */}
                        <audio
                            ref={remoteVideoRef as RefObject<HTMLAudioElement>}
                            autoPlay
                            playsInline
                            style={{ display: 'none' }}
                        />

                        {/* Avatar */}
                        <div className="relative mb-10">
                            {/* Animated Ring */}
                            {isConnected && (
                                <div className="absolute inset-0 rounded-full 
                                bg-blue-400 opacity-20 animate-ping"></div>
                            )}

                            {displayInfo.avatar ? (
                                <img
                                    src={displayInfo.avatar}
                                    alt={displayInfo.name}
                                    className="relative w-36 h-36 rounded-full 
                            border-4 border-blue-500/30 shadow-2xl
                            object-cover"
                                />
                            ) : (
                                <div className="relative w-36 h-36 rounded-full 
                                bg-blue-500
                                flex items-center justify-center
                                border-4 border-blue-400/30 shadow-2xl">
                                    <span className="text-6xl font-semibold text-white">
                                        {displayInfo.initial}
                                    </span>
                                </div>
                            )}
                        </div>

                        {/* Name */}
                        <h2 className="text-3xl font-semibold mb-3 text-white">
                            {displayInfo.name}
                        </h2>

                        {/* Status */}
                        <p className="text-blue-200 text-base font-medium">
                            {callState.status === 'RINGING' ? 'Đang gọi...' :
                                callState.status === 'ANSWERED' ? 'Đang kết nối...' :
                                    isConnected ? 'Đã kết nối' : 'Đang kết nối...'}
                        </p>
                    </div>
                )}

                {/* Status Overlay */}
                <div
                    className={`absolute top-5 left-5 transition-opacity duration-300
                      ${showControls || !isFullscreen ? 'opacity-100' : 'opacity-0'}`}
                >
                    <div className="bg-black/60 backdrop-blur-md px-5 py-3 
                          rounded-full flex items-center gap-4 shadow-lg">
                        {/* Connection Quality */}
                        <div className="flex items-center gap-2.5">
                            <div className={`w-2.5 h-2.5 rounded-full ${quality.color} 
                              ${isConnected ? 'animate-pulse' : ''}`}></div>
                            <span className="text-white text-sm font-medium">{quality.text}</span>
                        </div>

                        {/* Duration */}
                        {isConnected && (
                            <>
                                <div className="w-px h-5 bg-white/20"></div>
                                <span className="text-white text-sm font-mono font-medium">
                                    {formatDuration(callDuration)}
                                </span>
                            </>
                        )}
                    </div>
                </div>

                {/* Fullscreen Toggle */}
                <button
                    onClick={toggleFullscreen}
                    className={`absolute top-5 right-5 p-2.5 rounded-full 
                     bg-black/60 backdrop-blur-md hover:bg-black/80 
                     text-white transition-all shadow-lg
                     ${showControls || !isFullscreen ? 'opacity-100' : 'opacity-0'}`}
                >
                    {isFullscreen ? <Minimize2 size={22} /> : <Maximize2 size={22} />}
                </button>

                {/* Call Controls */}
                <div
                    className={`absolute bottom-0 left-0 right-0 
                      bg-black/80 backdrop-blur-md
                      p-10 transition-opacity duration-300
                      ${showControls || !isFullscreen ? 'opacity-100' : 'opacity-0'}`}
                >
                    <div className="flex justify-center items-center gap-8">

                        {/* Mute Button */}
                        <button
                            onClick={onToggleAudio}
                            className={`w-16 h-16 rounded-full flex items-center 
                         justify-center transition-all 
                         hover:scale-110 active:scale-95
                         ${mediaState.isAudioEnabled
                                    ? 'bg-white/10 hover:bg-white/20 border-2 border-white/20'
                                    : 'bg-red-500 hover:bg-red-600'
                                } text-white shadow-xl`}
                            title={mediaState.isAudioEnabled ? 'Tắt tiếng' : 'Bật tiếng'}
                        >
                            {mediaState.isAudioEnabled ? (
                                <Mic size={26} strokeWidth={2.5} />
                            ) : (
                                <MicOff size={26} strokeWidth={2.5} />
                            )}
                        </button>

                        {/* End Call Button */}
                        <button
                            onClick={onEndCall}
                            className="w-20 h-20 rounded-full 
                         bg-red-500 hover:bg-red-600
                         text-white 
                         flex items-center justify-center 
                         transition-all 
                         hover:scale-110 active:scale-95
                         shadow-2xl"
                            title="Kết thúc cuộc gọi"
                        >
                            <PhoneOff size={32} strokeWidth={2.5} />
                        </button>

                        {/* Video Toggle (if video call) */}
                        {isVideo && (
                            <button
                                onClick={onToggleVideo}
                                className={`w-16 h-16 rounded-full flex items-center 
                           justify-center transition-all 
                           hover:scale-110 active:scale-95
                           ${mediaState.isVideoEnabled
                                        ? 'bg-white/10 hover:bg-white/20 border-2 border-white/20'
                                        : 'bg-red-500 hover:bg-red-600'
                                    } text-white shadow-xl`}
                                title={mediaState.isVideoEnabled ? 'Tắt camera' : 'Bật camera'}
                            >
                                {mediaState.isVideoEnabled ? (
                                    <Video size={26} strokeWidth={2.5} />
                                ) : (
                                    <VideoOff size={26} strokeWidth={2.5} />
                                )}
                            </button>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default CallWindow;

