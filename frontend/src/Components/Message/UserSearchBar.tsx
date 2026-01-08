import React, { useState, useEffect, useRef } from 'react';
import { Search, Loader, X } from 'lucide-react';
import toast from 'react-hot-toast';
import { profileApiCall } from '../../service/api';

const UserSearchBar = ({ onSelectUser, onConversationCreated }) => {
  const [searchQuery, setSearchQuery] = useState("");
  const [searchResults, setSearchResults] = useState([]);
  const [isSearching, setIsSearching] = useState(false);
  const [showResults, setShowResults] = useState(false);
  const [isCreatingConversation, setIsCreatingConversation] = useState(false);
  const searchTimeoutRef = useRef(null);
  const searchContainerRef = useRef(null);

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (searchContainerRef.current && !searchContainerRef.current.contains(event.target)) {
        setShowResults(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  useEffect(() => {
    if (searchTimeoutRef.current) {
      clearTimeout(searchTimeoutRef.current);
    }

    if (!searchQuery.trim()) {
      setSearchResults([]);
      setShowResults(false);
      return;
    }

    setIsSearching(true);
    searchTimeoutRef.current = setTimeout(async () => {
      try {
        const data = await profileApiCall(`/users/search/${encodeURIComponent(searchQuery)}`);
        setSearchResults((data as any)?.result || []);
        setShowResults(true);
      } catch (error) {
        console.error("❌ Error searching users:", error);
        setSearchResults([]);
      } finally {
        setIsSearching(false);
      }
    }, 300);

    return () => {
      if (searchTimeoutRef.current) {
        clearTimeout(searchTimeoutRef.current);
      }
    };
  }, [searchQuery]);

  const createConversation = async (userId) => {
    try {
      const response = await fetch('http://localhost:8888/api/chat/conversations/create', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        },
        body: JSON.stringify({
          type: 'DIRECT',
          participantIds: [userId]
        })
      });

      if (!response.ok) {
        throw new Error('Failed to create conversation');
      }

      const data = await response.json();
      return data?.result || data;
    } catch (error) {
      console.error("❌ Error creating conversation:", error);
      throw error;
    }
  };

  const handleSelectUser = async (user) => {
    if (isCreatingConversation) return;

    setIsCreatingConversation(true);
    const loadingToast = toast.loading('Đang tạo cuộc trò chuyện...', {
      style: {
        borderRadius: '12px',
        fontSize: '14px',
      }
    });

    try {
      const conversation = await createConversation(user.userId);

      toast.success('Đã tạo cuộc trò chuyện!', {
        id: loadingToast,
        duration: 2000,
        style: {
          borderRadius: '12px',
          fontSize: '14px',
        }
      });

      if (onConversationCreated) {
        onConversationCreated(conversation);
      }

      if (onSelectUser) {
        onSelectUser(user);
      }

      setSearchQuery("");
      setSearchResults([]);
      setShowResults(false);
    } catch (error) {
      toast.error('Không thể tạo cuộc trò chuyện', {
        id: loadingToast,
        duration: 2000,
        style: {
          borderRadius: '12px',
          fontSize: '14px',
        }
      });
    } finally {
      setIsCreatingConversation(false);
    }
  };

  const clearSearch = () => {
    setSearchQuery("");
    setSearchResults([]);
    setShowResults(false);
  };

  return (
    <div ref={searchContainerRef} className="relative p-3 border-b border-gray-200 bg-white">
      {/* Search Input */}
      <div className="relative">
        <div className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400">
          <Search size={18} />
        </div>

        <input
          type="text"
          placeholder="Tìm kiếm..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          onFocus={() => searchQuery.trim() && setShowResults(true)}
          disabled={isCreatingConversation}
          className="w-full pl-10 pr-10 py-2 bg-gray-100 rounded-lg text-sm focus:outline-none focus:bg-gray-200 transition-colors disabled:opacity-50"
        />

        {/* Clear/Loading Button */}
        <div className="absolute right-3 top-1/2 -translate-y-1/2">
          {isSearching ? (
            <Loader size={16} className="text-blue-500 animate-spin" />
          ) : searchQuery && (
            <button
              onClick={clearSearch}
              className="text-gray-400 hover:text-gray-600 transition-colors"
            >
              <X size={16} />
            </button>
          )}
        </div>
      </div>

      {/* Search Results Dropdown */}
      {showResults && searchResults.length > 0 && (
        <div className="absolute top-full left-3 right-3 mt-2 bg-white rounded-2xl shadow-2xl border border-gray-100 max-h-96 overflow-hidden z-50">
          <div className="overflow-y-auto max-h-96">
            {searchResults.map((user, index) => (
              <div
                key={user.userId}
                onClick={() => !isCreatingConversation && handleSelectUser(user)}
                className={`flex items-center gap-3 p-3 hover:bg-gray-50 cursor-pointer transition-colors ${index !== searchResults.length - 1 ? 'border-b border-gray-100' : ''
                  } ${isCreatingConversation ? 'opacity-50 cursor-not-allowed' : ''}`}
              >
                {/* Avatar with Blue Gradient Ring */}
                <div className="flex-shrink-0">
                  <div className="w-12 h-12 rounded-full bg-gradient-to-br from-blue-400 to-cyan-500 p-[2px]">
                    <img
                      src={user.imageUrl || user.avatar || 'https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_640.png'}
                      alt={user.username}
                      className="w-full h-full rounded-full object-cover bg-white"
                    />
                  </div>
                </div>

                {/* User Info */}
                <div className="flex-1 min-w-0">
                  <div className="font-semibold text-sm text-gray-900 truncate">
                    {user.firstName} {user.lastName}
                  </div>
                  <div className="text-xs text-gray-500 truncate">
                    @{user.username}
                  </div>
                </div>

                {/* Arrow Icon */}
                {!isCreatingConversation && (
                  <div className="flex-shrink-0 text-gray-300">
                    <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                    </svg>
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* No Results */}
      {showResults && searchQuery.trim() && !isSearching && searchResults.length === 0 && (
        <div className="absolute top-full left-3 right-3 mt-2 bg-white rounded-2xl shadow-2xl border border-gray-100 p-6 text-center z-50">
          <div className="w-16 h-16 mx-auto mb-3 bg-gray-100 rounded-full flex items-center justify-center">
            <Search size={28} className="text-gray-300" />
          </div>
          <p className="text-sm text-gray-600 font-medium">Không tìm thấy người dùng</p>
          <p className="text-xs text-gray-400 mt-1">Thử tìm kiếm với từ khóa khác</p>
        </div>
      )}
    </div>
  );
};

export default UserSearchBar;

