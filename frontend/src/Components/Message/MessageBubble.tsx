import React, { useMemo } from 'react';
import { Check, CheckCheck, Phone, Video, Sparkles, Copy } from 'lucide-react';
import MessageAttachment from './MessageAttachment';
import toast from 'react-hot-toast';

interface Sender {
  firstName?: string;
  lastName?: string;
  username?: string;
  avatar?: string;
}

interface Attachment {
  id?: string;
  [key: string]: unknown;
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
interface Message {
  id?: string;
  senderId?: string;
  message?: string;
  content?: string;
  createdDate?: string;
  sender?: Sender;
  isPending?: boolean;
  isRead?: boolean;
  isAiMessage?: boolean;
  messageType?: string;
  attachments?: Attachment[];
  [key: string]: unknown;
}

interface MessageBubbleProps {
  msg: Message;
  currentUserId: string;
}

const MessageBubble = React.memo<MessageBubbleProps>(({ msg, currentUserId }) => {
  const isMe = msg.senderId === currentUserId;

  // ✅ Check if message is from AI
  const isAiMessage = useMemo(() =>
    msg.senderId === 'AI_BOT' || msg.isAiMessage === true,
    [msg.senderId, msg.isAiMessage]
  );

  // Memoize time formatting
  const time = useMemo(() => {
    const date = new Date(msg.createdDate || Date.now());
    return date.toLocaleTimeString('vi-VN', {
      hour: '2-digit',
      minute: '2-digit'
    });
  }, [msg.createdDate]);

  // Memoize sender name
  const senderName = useMemo(() => {
    if (!msg.sender) return '';
    const firstName = msg.sender.firstName || '';
    const lastName = msg.sender.lastName || '';
    return `${firstName} ${lastName}`.trim() || msg.sender.username || 'User';
  }, [msg.sender]);

  // Status Icon
  const StatusIcon = useMemo(() => {
    if (msg.isPending) return null;
    if (msg.isRead) return <CheckCheck size={12} strokeWidth={2.5} />;
    return <Check size={12} strokeWidth={2.5} />;
  }, [msg.isPending, msg.isRead]);

  // Has attachments
  const hasAttachments = useMemo(() =>
    msg.attachments && msg.attachments.length > 0,
    [msg.attachments]
  );

  // Has text
  const hasText = useMemo(() =>
    msg.message && msg.message.trim().length > 0,
    [msg.message]
  );

  // Check if call message
  const isCallMessage = useMemo(() =>
    msg.messageType === 'VOICE_CALL' || msg.messageType === 'VIDEO_CALL',
    [msg.messageType]
  );

  // ✅ Copy AI message to clipboard
  const handleCopyAiMessage = () => {
    if (msg.message) {
      navigator.clipboard.writeText(msg.message);
      toast.success('Đã sao chép', {
        duration: 1500,
        style: {
          borderRadius: '12px',
          fontSize: '14px',
        }
      });
    }
  };

  // ✅ RENDER CALL MESSAGE
  if (isCallMessage) {
    const isVideoCall = msg.messageType === 'VIDEO_CALL';

    return (
      <div className={`flex ${isMe ? 'justify-end' : 'justify-start'} mb-0.5 group`}>
        <div className={`flex items-end gap-2 max-w-[70%] ${isMe ? 'flex-row-reverse' : 'flex-row'}`}>
          {!isMe && (
            <div className="w-7 h-7 rounded-full flex-shrink-0 mb-0.5 overflow-hidden">
              <img
                src={msg.sender?.avatar || 'https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_640.png'}
                alt={senderName}
                className="w-full h-full object-cover"
                loading="lazy"
              />
            </div>
          )}

          <div className="flex flex-col max-w-full">
            {!isMe && senderName && (
              <span className="text-xs font-normal text-gray-500 mb-1 px-3 opacity-0 group-hover:opacity-100 transition-opacity">
                {senderName}
              </span>
            )}

            <div className={`relative ${msg.isPending ? 'opacity-60' : ''}`}>
              <div className={`inline-flex items-center gap-2 px-4 py-2.5 rounded-[20px] ${isMe
                  ? 'bg-blue-500 text-white rounded-br-md'
                  : 'bg-gray-100 text-gray-700 rounded-bl-md border border-gray-200'
                }`}>
                {isVideoCall ? (
                  <Video size={18} className={isMe ? 'text-white' : 'text-gray-600'} />
                ) : (
                  <Phone size={18} className={isMe ? 'text-white' : 'text-gray-600'} />
                )}

                <span className="text-[15px] font-normal whitespace-nowrap">
                  {msg.message}
                </span>
              </div>

              <div className={`flex items-center gap-1 mt-0.5 ${isMe ? 'justify-end' : 'justify-start'
                } opacity-0 group-hover:opacity-100 transition-opacity`}>
                <span className="text-[11px] text-gray-400 font-normal">
                  {time}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }

  // ✅ RENDER AI MESSAGE (Special styling)
  if (isAiMessage) {
    return (
      <div className="flex justify-start mb-2 group">
        <div className="flex items-end gap-2 max-w-[75%]">
          {/* AI Avatar with gradient */}
          <div className="w-8 h-8 rounded-full flex-shrink-0 mb-0.5 overflow-hidden bg-gradient-to-br from-green-400 to-emerald-500 flex items-center justify-center">
            <Sparkles size={18} className="text-white" strokeWidth={2} />
          </div>

          <div className="flex flex-col max-w-full">
            {/* AI Badge */}
            <div className="flex items-center gap-2 mb-1 px-3">
              <span className="text-xs font-semibold text-green-600 uppercase tracking-wide">
                🤖 AI Assistant
              </span>
            </div>

            {/* AI Message Bubble */}
            <div className="relative">
              <div className="inline-block max-w-full bg-gradient-to-br from-green-50 to-emerald-50 rounded-[20px] rounded-bl-md border-2 border-green-200 shadow-sm">
                {/* Attachments */}
                {hasAttachments && (
                  <div className={`${hasText ? 'mb-1' : ''}`}>
                    {msg.attachments.map((att, idx) => (
                      <MessageAttachment
                        key={att.id || idx}
                        attachment={att}
                        isMe={false}
                      />
                    ))}
                  </div>
                )}

                {/* Text message */}
                {hasText && (
                  <div className={`px-4 py-3 break-words whitespace-pre-wrap text-[15px] leading-relaxed font-normal text-gray-800 ${hasAttachments ? 'pt-0' : ''
                    }`}>
                    {msg.message}
                  </div>
                )}
              </div>

              {/* Time & Actions */}
              <div className="flex items-center gap-2 mt-1 opacity-0 group-hover:opacity-100 transition-opacity">
                <span className="text-[11px] text-gray-400 font-normal">
                  {time}
                </span>

                {/* Copy Button for AI messages */}
                <button
                  onClick={handleCopyAiMessage}
                  className="text-gray-400 hover:text-green-600 transition-colors p-1 rounded hover:bg-green-50"
                  title="Sao chép"
                >
                  <Copy size={12} strokeWidth={2} />
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }

  // ✅ RENDER NORMAL MESSAGE (TEXT/IMAGE/VIDEO/FILE)
  return (
    <div className={`flex ${isMe ? 'justify-end' : 'justify-start'} mb-0.5 group`}>
      <div className={`flex items-end gap-2 max-w-[70%] ${isMe ? 'flex-row-reverse' : 'flex-row'}`}>
        {!isMe && (
          <div className="w-7 h-7 rounded-full flex-shrink-0 mb-0.5 overflow-hidden">
            <img
              src={msg.sender?.avatar || 'https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_640.png'}
              alt={senderName}
              className="w-full h-full object-cover"
              loading="lazy"
            />
          </div>
        )}

        <div className="flex flex-col max-w-full">
          {!isMe && senderName && (
            <span className="text-xs font-normal text-gray-500 mb-1 px-3 opacity-0 group-hover:opacity-100 transition-opacity">
              {senderName}
            </span>
          )}

          <div className={`relative ${msg.isPending ? 'opacity-60' : ''}`}>
            <div className={`inline-block max-w-full ${isMe
                ? 'bg-blue-500 text-white rounded-[20px] rounded-br-md'
                : 'bg-gray-100 text-black rounded-[20px] rounded-bl-md border border-gray-200'
              }`}>
              {hasAttachments && (
                <div className={`${hasText ? 'mb-1' : ''}`}>
                  {msg.attachments.map((att, idx) => (
                    <MessageAttachment
                      key={att.id || idx}
                      attachment={att}
                      isMe={isMe}
                    />
                  ))}
                </div>
              )}

              {hasText && (
                <div className={`px-3 py-2 break-words whitespace-pre-wrap text-[15px] leading-[18px] font-normal ${hasAttachments ? 'pt-0' : ''
                  }`}>
                  {msg.message}
                </div>
              )}
            </div>

            <div className={`flex items-center gap-1 mt-0.5 ${isMe ? 'justify-end' : 'justify-start'
              } opacity-0 group-hover:opacity-100 transition-opacity`}>
              <span className="text-[11px] text-gray-400 font-normal">
                {time}
              </span>
              {isMe && StatusIcon && (
                <span className="text-gray-400">
                  {StatusIcon}
                </span>
              )}
              {msg.isPending && (
                <span className="text-[11px] text-gray-400 font-normal">
                  • Đang gửi
                </span>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}, (prevProps, nextProps) => {
  return (
    prevProps.msg.id === nextProps.msg.id &&
    prevProps.msg.message === nextProps.msg.message &&
    prevProps.msg.messageType === nextProps.msg.messageType &&
    prevProps.msg.isAiMessage === nextProps.msg.isAiMessage && // ✅ ADD THIS
    prevProps.msg.isPending === nextProps.msg.isPending &&
    prevProps.msg.isRead === nextProps.msg.isRead &&
    prevProps.msg.attachments === nextProps.msg.attachments &&
    prevProps.currentUserId === nextProps.currentUserId
  );
});

MessageBubble.displayName = 'MessageBubble';

export default MessageBubble;