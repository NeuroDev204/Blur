import React, { useEffect, useState, useRef } from "react";
import { likeStory, unlikeStory, deleteStory } from "../../api/storyApi";
import { timeDifference } from "../../Config/Logic";
import { useToast } from '@chakra-ui/react';
import { MdClose, MdVolumeOff, MdVolumeUp, MdMoreVert, MdDelete } from 'react-icons/md';
import { AiFillHeart, AiOutlineHeart } from 'react-icons/ai';

interface Story {
  id?: string;
  firstName?: string;
  lastName?: string;
  username?: string;
  mediaUrl?: string;
  mediaType?: string;
  thumbnailUrl?: string | null;
  content?: string;
  userProfileImage?: string;
  createdAt?: string;
  [key: string]: unknown;
}

interface StoryModalProps {
  isOpen: boolean;
  onClose: () => void;
  stories: Story[];
  story?: Story | null;
  onDeleteSuccess?: (storyId: string) => void;
}

const StoryModal: React.FC<StoryModalProps> = ({ isOpen, onClose, stories, story, onDeleteSuccess }) => {
  const [currentIndex, setCurrentIndex] = useState(0);
  const [isLiked, setIsLiked] = useState(false);
  const [isMuted, setIsMuted] = useState(true);
  const [showMenu, setShowMenu] = useState(false);
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const videoRef = useRef<HTMLVideoElement>(null);
  const modalContentRef = useRef<HTMLDivElement>(null);
  const toast = useToast();

  const currentStory = stories[currentIndex] || {};

  const isVideo = currentStory.mediaUrl &&
    (currentStory.mediaUrl.toLowerCase().endsWith('.mp4') ||
      currentStory.mediaUrl.toLowerCase().endsWith('.mov') ||
      currentStory.mediaUrl.toLowerCase().endsWith('.webm'));

  const handleClose = () => {
    if (timerRef.current) {
      clearTimeout(timerRef.current);
      timerRef.current = null;
    }

    if (videoRef.current) {
      videoRef.current.pause();
      videoRef.current.currentTime = 0;

      try {
        videoRef.current.removeEventListener('ended', handleVideoEnd);
      } catch (error) {
        // Ignore cleanup error
      }
    }

    onClose();
  };

  const handleVideoEnd = () => {
    if (currentIndex < stories.length - 1) {
      setCurrentIndex((prev) => prev + 1);
    } else {
      handleClose();
    }
  };

  const handleOutsideClick = (e) => {
    if (modalContentRef.current && !modalContentRef.current.contains(e.target)) {
      handleClose();
    }
  };

  useEffect(() => {
    if (isOpen) {
      if (isVideo) {
        if (videoRef.current) {
          videoRef.current.addEventListener('ended', handleVideoEnd);
          return () => {
            if (videoRef.current) {
              videoRef.current.removeEventListener('ended', handleVideoEnd);
            }
          };
        }
      } else {
        timerRef.current = setTimeout(() => {
          if (currentIndex < stories.length - 1) {
            setCurrentIndex((prev) => prev + 1);
          } else {
            handleClose();
          }
        }, 10000);
      }

      return () => {
        if (timerRef.current) {
          clearTimeout(timerRef.current);
        }
      };
    }
  }, [currentIndex, isOpen, stories.length, isVideo]);

  useEffect(() => {
    if (isOpen) {
      setCurrentIndex(0);
      setIsMuted(true);
    }
  }, [isOpen]);

  useEffect(() => {
    if (videoRef.current) {
      videoRef.current.muted = isMuted;
    }
  }, [currentIndex, isMuted]);

  useEffect(() => {
    return () => {
      if (timerRef.current) {
        clearTimeout(timerRef.current);
      }
      if (videoRef.current) {
        videoRef.current.pause();
      }
    };
  }, []);

  const handleNext = (e) => {
    e.stopPropagation();
    if (timerRef.current) {
      clearTimeout(timerRef.current);
    }
    if (videoRef.current) {
      videoRef.current.pause();
    }

    if (currentIndex < stories.length - 1) {
      setCurrentIndex(currentIndex + 1);
    } else {
      handleClose();
    }
  };

  const handlePrev = (e) => {
    e.stopPropagation();
    if (timerRef.current) {
      clearTimeout(timerRef.current);
    }
    if (videoRef.current) {
      videoRef.current.pause();
    }

    if (currentIndex > 0) {
      setCurrentIndex(currentIndex - 1);
    }
  };

  const toggleMute = () => {
    setIsMuted(!isMuted);
    if (videoRef.current) {
      videoRef.current.muted = !isMuted;
    }
  };

  const handleLikeStory = async (storyId) => {
    try {
      if (isLiked) {
        await unlikeStory(storyId);
      } else {
        await likeStory(storyId);
      }
      setIsLiked(!isLiked);
    } catch (error) {
      console.error("Error liking/unliking story:", error);
    }
  };

  const handleDeleteStory = async (storyId) => {
    if (!storyId) {
      toast({
        title: "Lỗi",
        description: "Không thể xóa story. ID không hợp lệ.",
        status: "error",
        duration: 3000,
        isClosable: true,
      });
      return;
    }

    try {
      const result = await deleteStory(storyId);
      if (result) {
        toast({
          title: "Đã xóa story",
          description: "Story đã được xóa thành công.",
          status: "success",
          duration: 3000,
          isClosable: true,
        });

        if (typeof onDeleteSuccess === 'function') {
          onDeleteSuccess(storyId);
        }

        handleClose();
      } else {
        toast({
          title: "Lỗi",
          description: "Không thể xóa story. Vui lòng thử lại.",
          status: "error",
          duration: 3000,
          isClosable: true,
        });
      }
    } catch (error) {
      console.error("Error deleting story:", error);
      toast({
        title: "Lỗi",
        description: (error as Error).message || "Có lỗi xảy ra khi xóa story.",
        status: "error",
        duration: 3000,
        isClosable: true,
      });
    }
  };

  if (!isOpen) return null;

  return (
    <div
      className="fixed inset-0 bg-black/90 backdrop-blur-sm flex justify-center items-center z-50 p-4"
      onClick={handleOutsideClick}
    >
      <div
        ref={modalContentRef}
        className="bg-black rounded-2xl relative w-full max-w-lg max-h-[90vh] shadow-2xl overflow-hidden flex items-center justify-center"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Close button */}
        <button
          className="absolute top-4 right-4 z-20 w-10 h-10 rounded-full bg-black/50 hover:bg-black/70 backdrop-blur-sm flex items-center justify-center transition-all group"
          onClick={handleClose}
        >
          <MdClose className="w-6 h-6 text-white group-hover:scale-110 transition-transform" />
        </button>

        {/* Progress bars */}
        <div className="absolute top-0 left-0 right-0 flex gap-1 px-3 pt-3 z-10">
          {stories.map((_, idx) => (
            <div
              key={idx}
              className="h-1 flex-1 rounded-full bg-white/20 overflow-hidden backdrop-blur-sm"
            >
              <div
                className={`h-full bg-gradient-to-r from-sky-400 to-blue-500 rounded-full transition-all ${!isVideo && idx === currentIndex
                    ? "duration-[10000ms] w-full"
                    : idx < currentIndex
                      ? "w-full"
                      : "w-0"
                  }`}
              />
            </div>
          ))}
        </div>

        {/* User info header */}
        <div className="absolute top-12 left-3 right-3 flex items-center gap-3 z-10 px-2">
          <div className="w-10 h-10 rounded-full overflow-hidden border-2 border-sky-400 shadow-lg">
            <img
              src={currentStory?.userProfileImage}
              alt="profile"
              className="w-full h-full object-cover"
            />
          </div>
          <div className="flex-1">
            <div className="text-white text-sm font-semibold drop-shadow-lg">
              {currentStory?.firstName
                ? `${currentStory.firstName} ${currentStory.lastName || ''}`
                : currentStory?.username || "User"}
            </div>
            <div className="text-white/80 text-xs drop-shadow">
              {timeDifference(currentStory?.createdAt)}
            </div>
          </div>

          {/* Menu button */}
          <button
            onClick={(e) => {
              e.stopPropagation();
              setShowMenu(!showMenu);
            }}
            className="w-9 h-9 rounded-full bg-black/50 hover:bg-black/70 backdrop-blur-sm flex items-center justify-center transition-all"
          >
            <MdMoreVert className="text-white w-5 h-5" />
          </button>

          {/* Menu dropdown */}
          {showMenu && (
            <div className="absolute right-0 top-12 bg-white rounded-xl shadow-xl overflow-hidden min-w-[160px]">
              <button
                onClick={() => {
                  handleDeleteStory(currentStory.id);
                  setShowMenu(false);
                }}
                className="w-full px-4 py-3 text-left text-sm text-red-600 hover:bg-red-50 transition-colors flex items-center gap-2"
              >
                <MdDelete className="w-5 h-5" />
                Xóa Story
              </button>
            </div>
          )}
        </div>

        {/* Media content - FIXED SIZING */}
        <div className="w-full h-full flex items-center justify-center">
          {currentStory?.mediaUrl && (
            isVideo ? (
              <video
                ref={videoRef}
                src={currentStory.mediaUrl}
                className="max-w-full max-h-[90vh] object-contain rounded-2xl"
                autoPlay
                playsInline
                muted={isMuted}
                onError={(e) => console.error("Video error:", e)}
              />
            ) : (
              <img
                src={currentStory.mediaUrl}
                alt="story"
                className="max-w-full max-h-[90vh] object-contain rounded-2xl"
              />
            )
          )}
        </div>

        {/* Caption */}
        {currentStory?.content && (
          <div className="absolute bottom-20 left-4 right-4 text-white px-4 py-3 bg-black/60 backdrop-blur-md rounded-xl shadow-lg">
            <p className="text-sm leading-relaxed">{currentStory.content}</p>
          </div>
        )}

        {/* Action buttons */}
        <div className="absolute bottom-4 left-4 right-4 flex items-center justify-between z-10 gap-3">
          {/* Like button */}
          <button
            onClick={(e) => {
              e.stopPropagation();
              if (currentStory && currentStory.id) {
                handleLikeStory(currentStory.id);
              }
            }}
            className="w-11 h-11 rounded-full bg-black/50 hover:bg-black/70 backdrop-blur-sm flex items-center justify-center transition-all group"
          >
            {isLiked ? (
              <AiFillHeart className="w-6 h-6 text-red-500 group-hover:scale-110 transition-transform" />
            ) : (
              <AiOutlineHeart className="w-6 h-6 text-white group-hover:scale-110 transition-transform" />
            )}
          </button>

          {/* Mute button for videos */}
          {isVideo && (
            <button
              onClick={(e) => {
                e.stopPropagation();
                toggleMute();
              }}
              className="w-11 h-11 rounded-full bg-black/50 hover:bg-black/70 backdrop-blur-sm flex items-center justify-center transition-all group"
            >
              {isMuted ? (
                <MdVolumeOff className="w-6 h-6 text-white group-hover:scale-110 transition-transform" />
              ) : (
                <MdVolumeUp className="w-6 h-6 text-white group-hover:scale-110 transition-transform" />
              )}
            </button>
          )}
        </div>

        {/* Navigation areas */}
        <div className="absolute top-0 left-0 h-full w-1/2 cursor-pointer z-[5]" onClick={handlePrev} />
        <div className="absolute top-0 right-0 h-full w-1/2 cursor-pointer z-[5]" onClick={handleNext} />
      </div>
    </div>
  );
};

export default StoryModal;
