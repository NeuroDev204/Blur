import React, { useEffect, useState, useRef } from "react";
import { likeStory, unlikeStory, deleteStory } from "../../api/storyApi";
import { timeDifference } from "../../Config/Logic";
import { useToast } from "@chakra-ui/react";
import {
  MdClose,
  MdVolumeOff,
  MdVolumeUp,
  MdMoreVert,
  MdDelete,
  MdChevronLeft,
  MdChevronRight,
} from "react-icons/md";
import { AiFillHeart, AiOutlineHeart } from "react-icons/ai";

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

const STORY_DURATION = 10000;

const StoryModal: React.FC<StoryModalProps> = ({
  isOpen,
  onClose,
  stories,
  story,
  onDeleteSuccess,
}) => {
  const [currentIndex, setCurrentIndex] = useState(0);
  const [isLiked, setIsLiked] = useState(false);
  const [isMuted, setIsMuted] = useState(true);
  const [showMenu, setShowMenu] = useState(false);
  const [progress, setProgress] = useState(0);
  const [isPaused, setIsPaused] = useState(false);

  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const videoRef = useRef<HTMLVideoElement>(null);
  const modalContentRef = useRef<HTMLDivElement>(null);
  const progressStartRef = useRef<number>(0);
  const toast = useToast();

  const currentStory = stories[currentIndex] || {};
  const isVideo =
    currentStory.mediaUrl &&
    /\.(mp4|mov|webm)$/i.test(currentStory.mediaUrl as string);

  /* ── helpers ── */
  const stopTimer = () => {
    if (timerRef.current) {
      clearInterval(timerRef.current);
      timerRef.current = null;
    }
  };

  const goToNext = () => {
    stopTimer();
    videoRef.current?.pause();
    setProgress(0);
    if (currentIndex < stories.length - 1) {
      setCurrentIndex((i) => i + 1);
    } else {
      handleClose();
    }
  };

  const goToPrev = () => {
    stopTimer();
    videoRef.current?.pause();
    setProgress(0);
    if (currentIndex > 0) setCurrentIndex((i) => i - 1);
  };

  const handleClose = () => {
    stopTimer();
    videoRef.current?.pause();
    setProgress(0);
    setCurrentIndex(0);
    onClose();
  };

  const handleVideoEnd = () => goToNext();

  const handleOutsideClick = (e: React.MouseEvent) => {
    if (
      modalContentRef.current &&
      !modalContentRef.current.contains(e.target as Node)
    ) {
      handleClose();
    }
  };

  /* ── progress timer for images ── */
  useEffect(() => {
    if (!isOpen || isVideo || isPaused) return;

    const tick = 100;
    const steps = STORY_DURATION / tick;
    progressStartRef.current = 0;

    timerRef.current = setInterval(() => {
      progressStartRef.current += 1;
      const pct = (progressStartRef.current / steps) * 100;
      setProgress(pct);
      if (pct >= 100) goToNext();
    }, tick);

    return stopTimer;
  }, [currentIndex, isOpen, isVideo, isPaused]);

  /* ── video event ── */
  useEffect(() => {
    const vid = videoRef.current;
    if (!vid) return;
    vid.addEventListener("ended", handleVideoEnd);
    return () => vid.removeEventListener("ended", handleVideoEnd);
  }, [currentIndex]);

  /* ── reset on open ── */
  useEffect(() => {
    if (isOpen) {
      setCurrentIndex(0);
      setProgress(0);
      setIsMuted(true);
      setIsLiked(false);
    }
  }, [isOpen]);

  /* ── sync mute ── */
  useEffect(() => {
    if (videoRef.current) videoRef.current.muted = isMuted;
  }, [isMuted, currentIndex]);

  /* ── pause/resume ── */
  const handlePressStart = () => {
    setIsPaused(true);
    stopTimer();
    videoRef.current?.pause();
  };
  const handlePressEnd = () => {
    setIsPaused(false);
    videoRef.current?.play();
  };

  const handleLikeStory = async (storyId: string) => {
    try {
      if (isLiked) await unlikeStory(storyId);
      else await likeStory(storyId);
      setIsLiked(!isLiked);
    } catch {}
  };

  const handleDeleteStory = async (storyId: string) => {
    if (!storyId) return;
    try {
      const result = await deleteStory(storyId);
      if (result) {
        toast({
          title: "Story deleted",
          status: "success",
          duration: 3000,
          isClosable: true,
        });
        onDeleteSuccess?.(storyId);
        handleClose();
      }
    } catch (error) {
      toast({
        title: "Error",
        description: (error as Error).message,
        status: "error",
        duration: 3000,
        isClosable: true,
      });
    }
  };

  if (!isOpen) return null;

  const displayName = currentStory.firstName
    ? `${currentStory.firstName} ${currentStory.lastName || ""}`.trim()
    : (currentStory.username as string) || "User";

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-md"
      onClick={handleOutsideClick}
    >
      {/* ── Phone-shaped story container ── */}
      <div
        ref={modalContentRef}
        className="relative bg-black rounded-3xl overflow-hidden shadow-2xl"
        style={{ width: "min(400px, 95vw)", height: "min(710px, 92vh)" }}
        onClick={(e) => e.stopPropagation()}
        onMouseDown={handlePressStart}
        onMouseUp={handlePressEnd}
        onTouchStart={handlePressStart}
        onTouchEnd={handlePressEnd}
      >
        {/* ── Media ── */}
        <div className="absolute inset-0">
          {currentStory.mediaUrl &&
            (isVideo ? (
              <video
                ref={videoRef}
                src={currentStory.mediaUrl as string}
                className="w-full h-full object-cover"
                autoPlay
                playsInline
                muted={isMuted}
                onError={() => {}}
              />
            ) : (
              <img
                src={currentStory.mediaUrl as string}
                alt="story"
                className="w-full h-full object-cover"
              />
            ))}
          {/* bottom vignette */}
          <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-black/40 pointer-events-none" />
        </div>

        {/* ── Progress bars ── */}
        <div className="absolute top-0 left-0 right-0 flex gap-1 px-3 pt-3 z-20">
          {stories.map((_, idx) => (
            <div
              key={idx}
              className="h-[3px] flex-1 rounded-full bg-white/30 overflow-hidden"
            >
              <div
                className="h-full bg-white rounded-full transition-none"
                style={{
                  width:
                    idx < currentIndex
                      ? "100%"
                      : idx === currentIndex
                      ? isVideo
                        ? "0%"
                        : `${progress}%`
                      : "0%",
                  transition:
                    idx === currentIndex && !isVideo && !isPaused
                      ? "none"
                      : "none",
                }}
              />
            </div>
          ))}
        </div>

        {/* ── Header ── */}
        <div className="absolute top-6 left-0 right-0 z-20 flex items-center px-4 pt-2 gap-3">
          {/* Avatar */}
          <div className="w-10 h-10 rounded-full p-[2px] bg-gradient-to-tr from-yellow-400 via-pink-500 to-purple-600 flex-shrink-0 shadow-lg">
            <div className="w-full h-full rounded-full overflow-hidden border-2 border-black">
              <img
                src={
                  (currentStory.userProfileImage as string) ||
                  "https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_640.png"
                }
                alt="profile"
                className="w-full h-full object-cover"
              />
            </div>
          </div>

          {/* Name + time */}
          <div className="flex-1 min-w-0">
            <p className="text-white text-sm font-semibold leading-tight truncate drop-shadow">
              {displayName}
            </p>
            <p className="text-white/70 text-xs mt-0.5 drop-shadow">
              {timeDifference(currentStory.createdAt as string)}
            </p>
          </div>

          {/* Menu */}
          <div className="relative flex-shrink-0 flex items-center gap-1">
            <button
              onClick={(e) => {
                e.stopPropagation();
                setShowMenu((v) => !v);
              }}
              onMouseDown={(e) => e.stopPropagation()}
              onMouseUp={(e) => e.stopPropagation()}
              className="w-9 h-9 rounded-full flex items-center justify-center hover:bg-white/10 transition-colors"
            >
              <MdMoreVert className="text-white w-5 h-5" />
            </button>

            {/* Close */}
            <button
              onClick={(e) => {
                e.stopPropagation();
                handleClose();
              }}
              onMouseDown={(e) => e.stopPropagation()}
              onMouseUp={(e) => e.stopPropagation()}
              className="w-9 h-9 rounded-full flex items-center justify-center hover:bg-white/10 transition-colors"
            >
              <MdClose className="text-white w-5 h-5" />
            </button>

            {showMenu && (
              <div className="absolute right-0 top-11 bg-white/95 backdrop-blur-sm rounded-2xl shadow-2xl overflow-hidden min-w-[160px] border border-gray-100">
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    handleDeleteStory(currentStory.id as string);
                    setShowMenu(false);
                  }}
                  onMouseDown={(e) => e.stopPropagation()}
                  className="w-full px-4 py-3 text-left text-sm text-red-500 hover:bg-red-50 transition-colors flex items-center gap-2 font-medium"
                >
                  <MdDelete className="w-4 h-4" />
                  Delete Story
                </button>
              </div>
            )}
          </div>
        </div>

        {/* ── Left / Right Nav ── */}
        <button
          className="absolute left-0 top-0 h-full w-1/3 z-10 flex items-center justify-start pl-2 opacity-0 hover:opacity-100 transition-opacity"
          onClick={(e) => {
            e.stopPropagation();
            goToPrev();
          }}
          onMouseDown={(e) => e.stopPropagation()}
          onMouseUp={(e) => e.stopPropagation()}
        >
          {currentIndex > 0 && (
            <div className="w-9 h-9 rounded-full bg-white/20 backdrop-blur-sm flex items-center justify-center">
              <MdChevronLeft className="text-white w-6 h-6" />
            </div>
          )}
        </button>
        <button
          className="absolute right-0 top-0 h-full w-1/3 z-10 flex items-center justify-end pr-2 opacity-0 hover:opacity-100 transition-opacity"
          onClick={(e) => {
            e.stopPropagation();
            goToNext();
          }}
          onMouseDown={(e) => e.stopPropagation()}
          onMouseUp={(e) => e.stopPropagation()}
        >
          <div className="w-9 h-9 rounded-full bg-white/20 backdrop-blur-sm flex items-center justify-center">
            <MdChevronRight className="text-white w-6 h-6" />
          </div>
        </button>

        {/* ── Caption ── */}
        {currentStory.content && (
          <div className="absolute bottom-20 left-4 right-4 z-20">
            <p className="text-white text-sm leading-relaxed bg-black/40 backdrop-blur-sm rounded-2xl px-4 py-3 shadow-lg">
              {currentStory.content as string}
            </p>
          </div>
        )}

        {/* ── Bottom actions ── */}
        <div className="absolute bottom-0 left-0 right-0 z-20 px-4 pb-5 pt-10 bg-gradient-to-t from-black/50 to-transparent">
          <div className="flex items-center justify-between">
            {/* Like */}
            <button
              onClick={(e) => {
                e.stopPropagation();
                if (currentStory.id) handleLikeStory(currentStory.id as string);
              }}
              onMouseDown={(e) => e.stopPropagation()}
              onMouseUp={(e) => e.stopPropagation()}
              className="flex items-center gap-2 group"
            >
              <div className="w-10 h-10 rounded-full bg-white/10 hover:bg-white/20 backdrop-blur-sm flex items-center justify-center transition-all group-hover:scale-110">
                {isLiked ? (
                  <AiFillHeart className="w-5 h-5 text-red-400" />
                ) : (
                  <AiOutlineHeart className="w-5 h-5 text-white" />
                )}
              </div>
            </button>

            {/* Mute (video only) */}
            {isVideo && (
              <button
                onClick={(e) => {
                  e.stopPropagation();
                  setIsMuted((m) => !m);
                }}
                onMouseDown={(e) => e.stopPropagation()}
                onMouseUp={(e) => e.stopPropagation()}
                className="w-10 h-10 rounded-full bg-white/10 hover:bg-white/20 backdrop-blur-sm flex items-center justify-center transition-all hover:scale-110"
              >
                {isMuted ? (
                  <MdVolumeOff className="w-5 h-5 text-white" />
                ) : (
                  <MdVolumeUp className="w-5 h-5 text-white" />
                )}
              </button>
            )}
          </div>
        </div>

        {/* ── Pause indicator ── */}
        {isPaused && (
          <div className="absolute inset-0 z-30 flex items-center justify-center pointer-events-none">
            <div className="bg-black/30 rounded-full p-4 backdrop-blur-sm">
              <div className="flex gap-1.5">
                <div className="w-1.5 h-7 bg-white rounded-full" />
                <div className="w-1.5 h-7 bg-white rounded-full" />
              </div>
            </div>
          </div>
        )}
      </div>

      {/* ── External prev/next arrows (outside modal) ── */}
      {currentIndex > 0 && (
        <button
          onClick={goToPrev}
          className="absolute left-4 md:left-12 w-12 h-12 rounded-full bg-white/10 hover:bg-white/20 backdrop-blur-sm border border-white/20 flex items-center justify-center text-white transition-all hover:scale-110 shadow-xl"
        >
          <MdChevronLeft className="w-7 h-7" />
        </button>
      )}
      {currentIndex < stories.length - 1 && (
        <button
          onClick={goToNext}
          className="absolute right-4 md:right-12 w-12 h-12 rounded-full bg-white/10 hover:bg-white/20 backdrop-blur-sm border border-white/20 flex items-center justify-center text-white transition-all hover:scale-110 shadow-xl"
        >
          <MdChevronRight className="w-7 h-7" />
        </button>
      )}
    </div>
  );
};

export default StoryModal;
