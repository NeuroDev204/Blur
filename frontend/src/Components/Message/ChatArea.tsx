// src/components/Chat/ChatArea.jsx
import React, { useState, useEffect, useRef, useCallback, useMemo } from 'react';
import { Image, Send, Loader, Smile, Plus, Phone, Video, Info, Sparkles } from 'lucide-react';
import toast, { Toaster } from 'react-hot-toast';
import MessageBubble from './MessageBubble';
import MediaPreview from './MediaPreview';
import { useCall } from '../../hooks/useCall';
import IncomingCallModal from '../Call/IncommingCallModal';
import CallWindow from '../Call/CallWindow';
import CallEndedModal from '../Call/CallendedModal';


export const uploadToCloudnary = async (file) => {
  if (!file) {
    return null;
  }
  try {
    const data = new FormData();
    data.append("file", file);
    data.append("upload_preset", "instagram");
    data.append("cloud_name", "dqg5pghlu");

    const isVideo = file.type.startsWith("video");
    const endpoint = isVideo
      ? "https://api.cloudinary.com/v1_1/dqg5pghlu/video/upload"
      : "https://api.cloudinary.com/v1_1/dqg5pghlu/image/upload";
    const res = await fetch(endpoint, { method: "POST", body: data });

    if (!res.ok) {
      throw new Error(`Upload failed: ${res.status}`);
    }

    const fileData = await res.json();

    const url = fileData.secure_url || fileData.url;

    if (!url) {
      throw new Error("No URL returned from Cloudinary");
    }

    const attachment = {
      id: fileData.public_id || `attachment-${Date.now()}`,
      url: url,
      fileName: file.name,
      fileType: file.type,
      fileSize: file.size,
      width: fileData.width || null,
      height: fileData.height || null,
      duration: fileData.duration || null,
      thumbnailUrl: fileData.thumbnail_url || null,
      format: fileData.format || null,
      resourceType: fileData.resource_type || (isVideo ? 'video' : 'image'),
    };

    return attachment;

  } catch (error) {
    alert(`Lỗi upload file ${file.name}: ${(error as Error).message}`);
    return null;
  }
};

const ChatArea = ({
  conversation,
  messages,
  onSendMessage,
  isConnected,
  currentUserId,
  currentUser,
  loadingMessages = false,
  messagesError = null
}) => {
  const [input, setInput] = useState("");
  const [selectedFiles, setSelectedFiles] = useState([]);
  const [isUploading, setIsUploading] = useState(false);
  const [showActions, setShowActions] = useState(false);
  const messagesEndRef = useRef(null);
  const fileInputRef = useRef(null);
  const inputRef = useRef(null);
  const {
    callState,
    mediaState,
    connectionState,
    callDuration,
    localVideoRef,
    remoteVideoRef,
    initiateCall,
    answerCall,
    rejectCall,
    endCall,
    toggleAudio,
    toggleVideo,
    callEndedInfo,
    closeCallEndedModal
  } = useCall(currentUserId);

  const handleVoiceCall = useCallback(() => {
    if (!conversation || callState.isInCall) {
      toast.error('Không thể gọi lúc này', {
        position: 'top-center',
        style: { background: '#ef4444', color: '#fff' },
      });
      return;
    }
    const receiver = conversation.participants.find(
      p => p.userId !== currentUserId
    );

    if (!receiver || !receiver.userId) {

      toast.error('Không tìm thấy thông tin người nhận', {
        position: 'top-center',
        style: { background: '#ef4444', color: '#fff' },
      });
      return;
    }

    // ============ FIX: Xác định tên người gọi (current user) ============
    const currentUserName = (() => {
      // Ưu tiên 1: firstName + lastName
      if (currentUser?.firstName && currentUser?.lastName) {
        return `${currentUser.firstName} ${currentUser.lastName}`.trim();
      }
      // Ưu tiên 2: firstName hoặc lastName (nếu chỉ có 1)
      if (currentUser?.firstName) return currentUser.firstName;
      if (currentUser?.lastName) return currentUser.lastName;
      // Ưu tiên 3: username
      if (currentUser?.username) return currentUser.username;
      // Ưu tiên 4: name field
      if (currentUser?.name) return currentUser.name;
      // Fallback
      return 'Người dùng Blur';
    })();

    const receiverData = {
      userId: receiver.userId,
      name: receiver.firstName && receiver.lastName
        ? `${receiver.firstName} ${receiver.lastName}`.trim()
        : receiver.username || 'Unknown',
      avatar: receiver.imageUrl || receiver.avatar || conversation.conversationAvatar || null,
      conversationId: conversation._id || conversation.id,
      currentUserName: currentUserName,
      currentUserAvatar: currentUser?.imageUrl || currentUser?.avatar || null
    };
    initiateCall(receiverData, 'VOICE');
  }, [conversation, callState.isInCall, currentUser, currentUserId, initiateCall]);

  const handleVideoCall = useCallback(() => {
    if (!conversation || callState.isInCall) {
      toast.error('Không thể gọi lúc này', {
        position: 'top-center',
        style: { background: '#ef4444', color: '#fff' },
      });
      return;
    }

    const receiver = conversation.participants.find(
      p => p.userId !== currentUserId
    );

    if (!receiver || !receiver.userId) {
      toast.error('Không tìm thấy thông tin người nhận', {
        position: 'top-center',
        style: { background: '#ef4444', color: '#fff' },
      });
      return;
    }

    // ============ FIX: Tương tự với video call ============
    const currentUserName = (() => {
      if (currentUser?.firstName && currentUser?.lastName) {
        return `${currentUser.firstName} ${currentUser.lastName}`.trim();
      }
      if (currentUser?.firstName) return currentUser.firstName;
      if (currentUser?.lastName) return currentUser.lastName;
      if (currentUser?.username) return currentUser.username;
      if (currentUser?.name) return currentUser.name;
      return 'Người dùng Blur';
    })();

    const receiverData = {
      userId: receiver.userId,
      name: receiver.firstName && receiver.lastName
        ? `${receiver.firstName} ${receiver.lastName}`.trim()
        : receiver.username || 'Unknown',
      avatar: receiver.imageUrl || receiver.avatar || conversation.conversationAvatar || null,
      conversationId: conversation._id || conversation.id,
      currentUserName: currentUserName,
      currentUserAvatar: currentUser?.imageUrl || currentUser?.avatar || null
    };
    initiateCall(receiverData, 'VIDEO');
  }, [conversation, callState.isInCall, currentUser, currentUserId, initiateCall]);

  // Optimize scroll với useCallback và Intersection Observer
  const scrollToBottom = useCallback(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth", block: "end" });
  }, []);

  // Debounce scroll để tránh scroll quá nhiều
  useEffect(() => {
    const timeoutId = setTimeout(scrollToBottom, 100);
    return () => clearTimeout(timeoutId);
  }, [messages.length, scrollToBottom]);

  // Đóng actions khi click outside
  useEffect(() => {
    const handleClickOutside = (e) => {
      if (showActions && !e.target.closest('.actions-menu')) {
        setShowActions(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [showActions]);

  // Memoize file validation
  const validateFiles = useCallback((files) => {
    const MAX_FILE_SIZE = 100 * 1024 * 1024; // 100MB
    const validFiles = files.filter(file => {
      const isImage = file.type.startsWith('image/');
      const isVideo = file.type.startsWith('video/');
      const isValidSize = file.size <= MAX_FILE_SIZE;

      if (!isValidSize) {
        toast.error(`File ${file.name} quá lớn (tối đa 100MB)`, {
          duration: 3000,
          style: {
            borderRadius: '12px',
            background: '#262626',
            color: '#fff',
            fontSize: '14px',
            padding: '12px 16px',
          }
        });
        return false;
      }

      return isImage || isVideo;
    });

    if (validFiles.length !== files.length) {
      toast.error('Chỉ chấp nhận ảnh và video', {
        duration: 2000,
        style: {
          borderRadius: '12px',
          background: '#262626',
          color: '#fff',
          fontSize: '14px',
          padding: '12px 16px',
        }
      });
    }

    return validFiles;
  }, []);

  const handleFileSelect = useCallback((e) => {
    const files = Array.from(e.target.files);
    const validFiles = validateFiles(files);
    setSelectedFiles(prev => [...prev, ...validFiles]);
    setShowActions(false);

    // Reset input để có thể chọn lại cùng file
    e.target.value = '';
  }, [validateFiles]);

  const removeFile = useCallback((index) => {
    setSelectedFiles(prev => prev.filter((_, i) => i !== index));
  }, []);

  const handleSend = useCallback(async () => {
    if ((!input.trim() && selectedFiles.length === 0) || !conversation || !isConnected) {
      return;
    }

    setIsUploading(true);

    const uploadingToast = selectedFiles.length > 0
      ? toast.loading('Đang tải lên...', {
        position: 'bottom-center',
        style: {
          borderRadius: '12px',
          fontSize: '14px',
          background: '#262626',
          color: '#fff',
          padding: '12px 16px',
        }
      })
      : null;

    try {
      let attachments = [];

      if (selectedFiles.length > 0) {
        const uploadPromises = selectedFiles.map(file => uploadToCloudnary(file));
        attachments = await Promise.all(uploadPromises);
        attachments = attachments.filter(att => att?.url?.trim());

        if (attachments.length !== selectedFiles.length) {
          throw new Error("Upload failed");
        }
      }

      await onSendMessage(input, attachments);

      if (uploadingToast) {
        toast.success('Đã gửi', {
          id: uploadingToast,
          duration: 1500,
          style: {
            borderRadius: '12px',
            background: '#262626',
            color: '#fff',
            fontSize: '14px',
            padding: '12px 16px',
          }
        });
      }

      setInput("");
      setSelectedFiles([]);

      requestAnimationFrame(() => {
        inputRef.current?.focus();
      });
    } catch (error) {
      if (uploadingToast) {
        toast.error('Không thể gửi', {
          id: uploadingToast,
          duration: 2000,
          style: {
            borderRadius: '12px',
            background: '#262626',
            color: '#fff',
            fontSize: '14px',
            padding: '12px 16px',
          }
        });
      }
    } finally {
      setIsUploading(false);
    }
  }, [input, selectedFiles, conversation, isConnected, onSendMessage]);

  const handleKeyPress = useCallback((e) => {
    if (e.key === 'Enter' && !e.shiftKey && !isUploading) {
      e.preventDefault();
      handleSend();
    }
  }, [handleSend, isUploading]);

  const handleAiAssist = useCallback(async () => {
    if (!input.trim()) {
      toast('Vui lòng nhập nội dung để AI hỗ trợ.', {
        icon: '🤔',
        position: 'bottom-center',
        duration: 2000,
        style: {
          borderRadius: '12px',
          background: '#262626',
          color: '#fff',
          fontSize: '14px',
          padding: '12px 16px',
        },
      });
      return;
    }

    const aiToast = toast.loading('AI đang suy nghĩ...', {
      position: 'bottom-center',
      style: {
        borderRadius: '12px',
        background: '#262626',
        color: '#fff',
        fontSize: '14px',
        padding: '12px 16px',
      },
    });

    try {
      const response = await fetch('http://localhost:8888/api/chat/ai/chat', {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          conversationId: null,
          userId: localStorage.getItem('userId') || 'anonymous',
          message: input
        })
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || `Lỗi AI: ${response.statusText}`);
      }

      const data = await response.json();

      // Backend trả về: { conversationId, response, success, error }
      if (data && data.success && data.response) {
        setInput(data.response); // Thay data.message thành data.response
        toast.success('AI đã hỗ trợ!', { id: aiToast, duration: 2000 });
      } else {
        throw new Error(data.error || 'Phản hồi từ AI không hợp lệ.');
      }
    } catch (error) {
      toast.error((error as Error).message || 'Không thể kết nối tới AI.', {
        id: aiToast,
        duration: 3000
      });
    }
  }, [input]);

  // Memoize để tránh re-render không cần thiết
  const canSend = useMemo(() =>
    (input.trim() || selectedFiles.length > 0) && !isUploading,
    [input, selectedFiles.length, isUploading]
  );

  if (!conversation) {
    return (
      <div className="flex-1 flex items-center justify-center bg-white">
        <div className="text-center px-6">
          <div className="w-24 h-24 mx-auto mb-6 border-2 border-black rounded-full flex items-center justify-center">
            <Send size={40} className="text-black" strokeWidth={1.5} />
          </div>
          <h3 className="text-[22px] font-light text-black mb-2">
            Tin nhắn của bạn
          </h3>
          <p className="text-sm text-gray-500 font-normal">
            Gửi ảnh và tin nhắn riêng tư cho bạn bè hoặc nhóm.
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="flex-1 flex flex-col bg-white h-full relative">
      <Toaster position="bottom-center" />

      {/* Header - Instagram Style */}
      <div className="bg-white border-b border-gray-200 px-5 py-3 sticky top-0 z-10">
        <div className="flex items-center justify-between max-w-full">
          <div className="flex items-center gap-3 flex-1 min-w-0">
            <div className="relative flex-shrink-0">
              <div className="w-10 h-10 rounded-full overflow-hidden">
                <img
                  src={conversation.conversationAvatar || 'https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_640.png'}
                  alt={conversation.conversationName}
                  className="w-full h-full object-cover"
                />
              </div>
              {isConnected && (
                <div className="absolute -bottom-0.5 -right-0.5 w-3.5 h-3.5 bg-green-500 border-2 border-white rounded-full"></div>
              )}
            </div>
            <div className="flex-1 min-w-0">
              <h3 className="font-semibold text-black text-sm truncate leading-tight">
                {conversation.conversationName}
              </h3>
              {isConnected && (
                <span className="text-xs text-gray-500 font-normal">Đang hoạt động</span>
              )}
            </div>
          </div>

          {/* Call Buttons */}
          <div className="flex items-center gap-1 flex-shrink-0">
            <button
              onClick={handleVoiceCall}
              disabled={callState.isInCall}
              className="w-9 h-9 flex items-center justify-center text-black hover:bg-gray-100 rounded-full transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              aria-label="Gọi thoại"
            >
              <Phone size={22} strokeWidth={1.5} />
            </button>

            <button
              onClick={handleVideoCall}
              disabled={callState.isInCall}
              className="w-9 h-9 flex items-center justify-center text-black hover:bg-gray-100 rounded-full transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              aria-label="Gọi video"
            >
              <Video size={22} strokeWidth={1.5} />
            </button>

            <button
              className="w-9 h-9 flex items-center justify-center text-black hover:bg-gray-100 rounded-full transition-colors"
              aria-label="Thông tin"
            >
              <Info size={22} strokeWidth={1.5} />
            </button>
          </div>
        </div>
      </div>

      {/* Messages Area */}
      <div className="flex-1 overflow-y-auto bg-white" style={{ scrollbarWidth: 'thin' }}>
        {loadingMessages ? (
          <div className="flex flex-col items-center justify-center h-full text-gray-400 px-6">
            <div className="w-16 h-16 mb-5 rounded-full flex items-center justify-center border-4 border-blue-200 border-t-blue-500 animate-spin">
            </div>
            <p className="text-center text-sm font-normal text-gray-500">
              Đang tải tin nhắn...
            </p>
          </div>
        ) : messagesError ? (
          <div className="flex flex-col items-center justify-center h-full text-gray-400 px-6">
            <div className="w-24 h-24 mb-5 border-2 border-red-300 rounded-full flex items-center justify-center">
              <Send size={40} className="text-red-400" strokeWidth={1.5} />
            </div>
            <p className="text-center text-sm font-normal text-red-500 mb-1">
              {messagesError}
            </p>
            <p className="text-center text-xs text-gray-400">
              Vui lòng thử lại hoặc làm mới trang
            </p>
          </div>
        ) : messages.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-full text-gray-400 px-6">
            <div className="w-24 h-24 mb-5 border-2 border-gray-300 rounded-full flex items-center justify-center">
              <Send size={40} className="text-gray-400" strokeWidth={1.5} />
            </div>
            <p className="text-center text-sm font-normal text-gray-500 mb-1">
              Chưa có tin nhắn nào
            </p>
            <p className="text-center text-xs text-gray-400">
              Gửi tin nhắn để bắt đầu cuộc trò chuyện
            </p>
          </div>
        ) : (
          <div className="py-4 px-5">
            {messages.map((msg) => (
              <MessageBubble
                key={msg.id}
                msg={msg}
                currentUserId={currentUserId}
              />
            ))}
            <div ref={messagesEndRef} />
          </div>
        )}
      </div>

      {/* Input Area - Instagram Style */}
      <div className="bg-white border-t border-gray-200 p-5">
        {/* File Preview */}
        {selectedFiles.length > 0 && (
          <div className="mb-4">
            <div className="flex items-center justify-between mb-3">
              <span className="text-sm font-semibold text-black">
                {selectedFiles.length} tệp
              </span>
              <button
                onClick={() => setSelectedFiles([])}
                className="text-sm text-blue-500 font-semibold hover:text-blue-700 transition-colors"
              >
                Xóa tất cả
              </button>
            </div>
            <div className="flex flex-wrap gap-2">
              {selectedFiles.map((file, idx) => (
                <MediaPreview key={idx} file={file} onRemove={() => removeFile(idx)} />
              ))}
            </div>
          </div>
        )}

        {/* Input Box */}
        <div className="flex items-center gap-3">
          {/* Actions Button */}
          <div className="relative actions-menu">
            <button
              onClick={() => setShowActions(!showActions)}
              className="w-9 h-9 flex items-center justify-center text-black hover:bg-gray-100 rounded-full transition-colors flex-shrink-0"
              aria-label="Thêm tệp"
            >
              <Plus size={24} strokeWidth={2} />
            </button>

            {showActions && (
              <>
                <div
                  className="fixed inset-0 z-10"
                  onClick={() => setShowActions(false)}
                />
                <div className="absolute bottom-full left-0 mb-2 bg-white shadow-lg rounded-xl py-2 min-w-[180px] border border-gray-200 z-20 animate-in fade-in slide-in-from-bottom-2 duration-200">
                  <button
                    onClick={() => {
                      fileInputRef.current?.click();
                      setShowActions(false);
                    }}
                    className="w-full flex items-center gap-3 px-4 py-2.5 hover:bg-gray-50 transition-colors text-left"
                  >
                    <div className="w-8 h-8 bg-blue-500 rounded-full flex items-center justify-center flex-shrink-0">
                      <Image size={16} className="text-white" strokeWidth={2} />
                    </div>
                    <span className="text-sm font-normal text-black">Ảnh/Video</span>
                  </button>
                </div>
              </>
            )}
          </div>

          <input
            type="file"
            ref={fileInputRef}
            onChange={handleFileSelect}
            multiple
            accept="image/*,video/*"
            className="hidden"
          />

          {/* Text Input */}
          <div className="flex-1 flex items-center gap-3 bg-gray-100 rounded-full px-4 py-2 border border-gray-200 focus-within:border-gray-300 transition-colors">
            <input
              ref={inputRef}
              type="text"
              className="flex-1 bg-transparent focus:outline-none text-sm text-black placeholder-gray-500 font-normal"
              placeholder="Nhắn tin..."
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKeyPress}
              disabled={isUploading}
            />
            <button
              onClick={handleAiAssist}
              className="text-black hover:opacity-60 transition-opacity flex-shrink-0"
              aria-label="AI"
            >
              <Sparkles size={22} strokeWidth={1.5} />
            </button>
            <button
              className="text-black hover:opacity-60 transition-opacity flex-shrink-0"
              aria-label="Emoji"
            >
              <Smile size={22} strokeWidth={1.5} />
            </button>
          </div>

          {/* Send Button */}
          {canSend && (
            <button
              onClick={handleSend}
              disabled={isUploading}
              className="text-blue-500 font-semibold text-sm hover:text-blue-700 disabled:text-blue-300 transition-colors flex-shrink-0 px-1"
            >
              {isUploading ? (
                <Loader size={20} className="animate-spin" strokeWidth={2} />
              ) : (
                'Gửi'
              )}
            </button>
          )}
        </div>
      </div>

      {/* ============ CALL MODALS ============ */}

      {/* Incoming Call Modal */}
      {callState.isIncoming && (
        <IncomingCallModal
          callerName={callState.callerName}
          callerAvatar={callState.callerAvatar}
          callType={callState.callType}
          onAnswer={answerCall}
          onReject={rejectCall}
        />
      )}

      {/* Active Call Window */}
      {callState.isInCall && !callState.isIncoming && (
        <CallWindow
          callState={callState}
          mediaState={mediaState}
          connectionState={connectionState}
          callDuration={callDuration}
          localVideoRef={localVideoRef}
          remoteVideoRef={remoteVideoRef}
          onEndCall={endCall}
          onToggleAudio={toggleAudio}
          onToggleVideo={toggleVideo}
        />
      )}

      {/* Call Ended Modal */}
      {callEndedInfo && (
        <CallEndedModal
          callerName={callEndedInfo.callerName}
          callerAvatar={callEndedInfo.callerAvatar}
          callType={callEndedInfo.callType}
          duration={callEndedInfo.duration}
          endReason={callEndedInfo.endReason as any}
          onClose={closeCallEndedModal}
        />
      )}
    </div>
  );
};

export default ChatArea;


