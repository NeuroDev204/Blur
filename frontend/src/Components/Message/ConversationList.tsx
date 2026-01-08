import React, { useMemo, useCallback, useState } from 'react';
import { MessageCircle, MoreVertical, Trash2, X } from 'lucide-react';
import UserSearchBar from './UserSearchBar';
import { useUnreadMessages } from '../../hooks/useUnreadMessages';

interface Conversation {
  id: string;
  conversationName?: string;
  conversationAvatar?: string;
  lastMessage?: string;
  lastMessageTime?: string;
  [key: string]: unknown;
}

interface DeleteConfirmDialogProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void;
  conversationName: string;
}

// Delete Confirmation Dialog Component
const DeleteConfirmDialog: React.FC<DeleteConfirmDialogProps> = ({ isOpen, onClose, onConfirm, conversationName }) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-[9999] flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm animate-fade-in">
      <div className="bg-white rounded-2xl shadow-2xl max-w-sm w-full p-6 animate-scale-in">
        {/* Icon */}
        <div className="w-14 h-14 mx-auto mb-4 bg-red-100 rounded-full flex items-center justify-center">
          <Trash2 size={28} className="text-red-600" />
        </div>

        {/* Title */}
        <h3 className="text-lg font-bold text-gray-900 text-center mb-2">
          Xóa cuộc trò chuyện?
        </h3>

        {/* Message */}
        <p className="text-sm text-gray-600 text-center mb-6">
          Bạn có chắc muốn xóa cuộc trò chuyện với{' '}
          <span className="font-semibold text-gray-900">{conversationName}</span>?
          <br />
          <span className="text-red-500 font-medium">Hành động này không thể hoàn tác.</span>
        </p>

        {/* Buttons */}
        <div className="flex gap-3">
          <button
            onClick={onClose}
            className="flex-1 px-4 py-2.5 bg-gray-100 hover:bg-gray-200 text-gray-700 font-semibold rounded-xl transition-colors"
          >
            Hủy
          </button>
          <button
            onClick={onConfirm}
            className="flex-1 px-4 py-2.5 bg-gradient-to-r from-red-500 to-red-600 hover:from-red-600 hover:to-red-700 text-white font-semibold rounded-xl transition-all shadow-md hover:shadow-lg"
          >
            Xóa
          </button>
        </div>
      </div>

      <style>{`
        @keyframes fadeIn {
          from { opacity: 0; }
          to { opacity: 1; }
        }
        @keyframes scaleIn {
          from {
            opacity: 0;
            transform: scale(0.9);
          }
          to {
            opacity: 1;
            transform: scale(1);
          }
        }
        .animate-fade-in {
          animation: fadeIn 0.2s ease-out;
        }
        .animate-scale-in {
          animation: scaleIn 0.2s ease-out;
        }
      `}</style>
    </div>
  );
};

interface ConversationItemProps {
  conv: Conversation;
  isSelected: boolean;
  unreadCount: number;
  onClick: () => void;
  onDelete: (conv: Conversation) => void;
}

// Conversation Item với Delete Menu
const ConversationItem = React.memo<ConversationItemProps>(({
  conv,
  isSelected,
  unreadCount,
  onClick,
  onDelete
}) => {
  const [showMenu, setShowMenu] = useState(false);
  const hasUnread = unreadCount > 0;

  // Memoize time formatting
  const messageTime = useMemo(() => {
    if (!conv.lastMessageTime) return '';

    const date = new Date(conv.lastMessageTime);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);

    if (diffMins < 1) return 'Vừa xong';
    if (diffMins < 60) return `${diffMins} phút`;
    if (diffHours < 24) return `${diffHours} giờ`;
    if (diffDays < 7) return `${diffDays} ngày`;

    return date.toLocaleDateString('vi-VN', {
      day: '2-digit',
      month: '2-digit'
    });
  }, [conv.lastMessageTime]);

  const displayUnreadCount = useMemo(() =>
    unreadCount > 99 ? '99+' : unreadCount.toString(),
    [unreadCount]
  );

  const handleMenuClick = (e) => {
    e.stopPropagation();
    setShowMenu(!showMenu);
  };

  const handleDeleteClick = (e) => {
    e.stopPropagation();
    setShowMenu(false);
    onDelete(conv);
  };

  return (
    <div
      onClick={onClick}
      className={`relative flex items-center p-4 cursor-pointer transition-all duration-200 group ${isSelected
        ? 'bg-gradient-to-r from-sky-50 to-blue-50 border-l-4 border-l-blue-500'
        : 'hover:bg-gradient-to-r hover:from-gray-50 hover:to-blue-50/30'
        }`}
    >
      {/* Avatar */}
      <div className="relative flex-shrink-0">
        <div className={`rounded-full p-[2px] ${hasUnread
          ? 'bg-gradient-to-br from-sky-400 via-blue-500 to-cyan-500'
          : 'bg-gradient-to-br from-gray-300 to-gray-400'
          }`}>
          <img
            src={conv.conversationAvatar || 'https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_640.png'}
            alt={conv.conversationName}
            className="w-14 h-14 rounded-full object-cover bg-white"
            loading="lazy"
          />
        </div>
        <div className="absolute bottom-0 right-0 w-4 h-4 bg-green-400 rounded-full border-2 border-white shadow-md" />
      </div>

      {/* Content */}
      <div className="flex-1 min-w-0 ml-4 mr-2">
        <div className="flex items-center justify-between mb-1">
          <h3 className={`truncate text-base ${hasUnread
            ? 'font-bold text-gray-900'
            : 'font-semibold text-gray-700'
            }`}>
            {conv.conversationName}
          </h3>

          {messageTime && (
            <span className={`text-xs ml-2 flex-shrink-0 font-medium ${hasUnread ? 'text-blue-600' : 'text-gray-400'
              }`}>
              {messageTime}
            </span>
          )}
        </div>

        <div className="flex items-center justify-between gap-2">
          <p className={`text-sm truncate ${hasUnread
            ? 'text-gray-900 font-semibold'
            : 'text-gray-500 font-normal'
            }`}>
            {conv.lastMessage || 'Chưa có tin nhắn'}
          </p>

          {hasUnread && (
            <div className="flex-shrink-0 min-w-[22px] h-6 px-2.5 flex items-center justify-center bg-gradient-to-br from-sky-500 to-blue-600 text-white text-xs font-bold rounded-full shadow-lg">
              {displayUnreadCount}
            </div>
          )}
        </div>
      </div>

      {/* Menu Button - Hiện khi hover */}
      <div className="relative flex-shrink-0">
        <button
          onClick={handleMenuClick}
          className="w-8 h-8 flex items-center justify-center text-gray-400 hover:text-gray-600 hover:bg-gray-200 rounded-full transition-all opacity-0 group-hover:opacity-100"
        >
          <MoreVertical size={18} />
        </button>

        {/* Dropdown Menu */}
        {showMenu && (
          <>
            {/* Overlay để đóng menu khi click outside */}
            <div
              className="fixed inset-0 z-10"
              onClick={(e) => {
                e.stopPropagation();
                setShowMenu(false);
              }}
            />

            {/* Menu */}
            <div className="absolute right-0 top-full mt-1 z-20 bg-white rounded-xl shadow-2xl border border-gray-100 py-1 min-w-[160px]">
              <button
                onClick={handleDeleteClick}
                className="w-full flex items-center gap-3 px-4 py-2.5 hover:bg-red-50 transition-colors text-left group"
              >
                <Trash2 size={16} className="text-red-500 group-hover:scale-110 transition-transform" />
                <span className="text-sm font-medium text-red-600">Xóa</span>
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
});

ConversationItem.displayName = 'ConversationItem';

const ConversationList = ({ conversations, selected, onSelect, onSelectUser, onConversationDeleted, onConversationCreated }) => {
  const { unreadByConversation } = useUnreadMessages();
  const [deleteDialog, setDeleteDialog] = useState({ isOpen: false, conversation: null });
  const [isDeleting, setIsDeleting] = useState(false);

  // Memoize conversations with unread data
  const conversationsWithUnread = useMemo(() =>
    conversations.map(conv => ({
      ...conv,
      unreadCount: unreadByConversation[conv.id] || 0
    })),
    [conversations, unreadByConversation]
  );

  // Sort conversations
  const sortedConversations = useMemo(() =>
    [...conversationsWithUnread].sort((a, b) => {
      if (a.unreadCount > 0 && b.unreadCount === 0) return -1;
      if (a.unreadCount === 0 && b.unreadCount > 0) return 1;

      const timeA = a.lastMessageTime ? new Date(a.lastMessageTime).getTime() : 0;
      const timeB = b.lastMessageTime ? new Date(b.lastMessageTime).getTime() : 0;
      return timeB - timeA;
    }),
    [conversationsWithUnread]
  );

  // Handle select conversation
  const handleSelect = useCallback(async (conv) => {
    onSelect(conv);

    const unreadCount = unreadByConversation[conv.id] || 0;
    if (unreadCount > 0) {
      try {
        const response = await fetch(`http://localhost:8888/api/chat/conversations/mark-as-read?conversationId=${conv.id}`, {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${localStorage.getItem('token')}`
          }
        });

        if (!response.ok) {
          throw new Error('Failed to mark as read');
        }

        console.log('✅ Marked conversation as read:', conv.id);
      } catch (error) {
        console.error('❌ Error marking as read:', error);
      }
    }
  }, [onSelect, unreadByConversation]);

  // Handle delete conversation
  const handleDelete = useCallback((conv) => {
    setDeleteDialog({ isOpen: true, conversation: conv });
  }, []);

  const confirmDelete = useCallback(async () => {
    if (!deleteDialog.conversation || isDeleting) return;

    setIsDeleting(true);
    const conversationId = deleteDialog.conversation.id;

    try {
      console.log(`🗑️ Deleting conversation: ${conversationId}`);

      const response = await fetch(`http://localhost:8888/api/chat/conversations?conversationId=${conversationId}`, {
        method: 'DELETE',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        }
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      const data = await response.json();
      console.log('✅ Conversation deleted successfully:', data);

      // Close dialog
      setDeleteDialog({ isOpen: false, conversation: null });

      // Deselect if currently selected
      if (selected?.id === conversationId) {
        onSelect(null);
      }

      // Notify parent to refresh conversations
      if (onConversationDeleted) {
        onConversationDeleted(conversationId);
      }

      // Show success notification
      if (window.toast) {
        window.toast.success('Đã xóa cuộc trò chuyện', {
          duration: 2000,
          style: {
            borderRadius: '12px',
            fontSize: '14px',
          }
        });
      }
    } catch (error) {
      console.error('❌ Failed to delete conversation:', error);

      // Show error notification
      if (window.toast) {
        window.toast.error('Không thể xóa cuộc trò chuyện', {
          duration: 2000,
          style: {
            borderRadius: '12px',
            fontSize: '14px',
          }
        });
      }
    } finally {
      setIsDeleting(false);
    }
  }, [deleteDialog.conversation, isDeleting, selected, onSelect, onConversationDeleted]);

  const closeDialog = useCallback(() => {
    if (!isDeleting) {
      setDeleteDialog({ isOpen: false, conversation: null });
    }
  }, [isDeleting]);

  // Calculate total unread
  const totalUnread = useMemo(() =>
    Object.values(unreadByConversation).reduce((sum, count) => sum + count, 0),
    [unreadByConversation]
  );

  return (
    <>
      <div className="w-full md:w-80 border-r border-gray-200 bg-white overflow-hidden flex flex-col shadow-sm h-full">
        {/* Header */}
        <div className="bg-gradient-to-r from-sky-500 via-blue-600 to-cyan-500 px-4 md:px-5 py-3 md:py-4 shadow-md">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 md:gap-3">
              <MessageCircle size={22} className="text-white md:w-6 md:h-6" />
              <h2 className="text-lg md:text-xl font-bold text-white">Tin nhắn</h2>
            </div>
            {totalUnread > 0 && (
              <div className="px-2 md:px-3 py-0.5 md:py-1 bg-white/90 backdrop-blur-sm rounded-full">
                <span className="text-xs md:text-sm font-bold text-blue-600">
                  {totalUnread > 99 ? '99+' : totalUnread}
                </span>
              </div>
            )}
          </div>
        </div>

        {/* User Search Bar */}
        <div className="border-b border-gray-100 bg-gradient-to-b from-sky-50/50 to-white">
          <UserSearchBar
            onSelectUser={onSelectUser}
            onConversationCreated={onConversationCreated}
          />
        </div>

        {/* Conversations List */}
        <div className="flex-1 overflow-y-auto scrollbar-thin scrollbar-thumb-blue-200 scrollbar-track-transparent hover:scrollbar-thumb-blue-300">
          {sortedConversations.length === 0 ? (
            <div className="flex flex-col items-center justify-center h-full p-8 text-center">
              <div className="w-20 h-20 mb-4 bg-gradient-to-br from-sky-100 to-blue-100 rounded-full flex items-center justify-center shadow-lg">
                <MessageCircle size={36} className="text-blue-400" />
              </div>
              <p className="text-base font-semibold text-gray-600 mb-1">
                Chưa có cuộc trò chuyện
              </p>
              <p className="text-sm text-gray-400">
                Tìm bạn bè và bắt đầu nhắn tin
              </p>
            </div>
          ) : (
            <div className="divide-y divide-gray-100">
              {sortedConversations.map((conv) => (
                <ConversationItem
                  key={conv.id}
                  conv={conv}
                  isSelected={selected?.id === conv.id}
                  unreadCount={conv.unreadCount}
                  onClick={() => handleSelect(conv)}
                  onDelete={handleDelete}
                />
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Delete Confirmation Dialog */}
      <DeleteConfirmDialog
        isOpen={deleteDialog.isOpen}
        onClose={closeDialog}
        onConfirm={confirmDelete}
        conversationName={deleteDialog.conversation?.conversationName || ''}
      />
    </>
  );
};

export default ConversationList;
