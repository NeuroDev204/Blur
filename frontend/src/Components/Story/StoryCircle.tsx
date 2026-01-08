// ============= StoryCircle.tsx =============
import React, { useState } from "react";
import StoryModal from "./StoryModal";
import AddStoryModal from "./AddStoryModal";

interface Story {
  id?: string;
  firstName?: string;
  lastName?: string;
  username?: string;
  mediaType?: string;
  mediaUrl?: string;
  thumbnailUrl?: string;
  [key: string]: unknown;
}

interface User {
  id?: string;
  imageUrl?: string;
  [key: string]: unknown;
}

interface StoryCircleProps {
  story?: Story | null;
  stories?: Story[];
  isAddNew?: boolean;
  onStoryCreated?: (story: Story) => void;
  user?: User | null;
}

const StoryCircle: React.FC<StoryCircleProps> = ({
  story = null,
  stories = [],
  isAddNew = false,
  onStoryCreated,
  user
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const [showCreateModal, setShowCreateModal] = useState(false);

  const handleOpenStory = () => {
    if (isAddNew) {
      setShowCreateModal(true);
    } else {
      setIsOpen(true);
    }
  };

  const handleStoryCreated = (newStory: Story) => {
    if (onStoryCreated && typeof onStoryCreated === 'function') {
      onStoryCreated(newStory);
    }
    setShowCreateModal(false);
  };

  const userStories = stories.length > 0 ? stories : story ? [story] : [];

  const getDisplayName = () => {
    if (isAddNew) return "Tạo tin";
    if (!story) return "Người dùng";
    if (story.firstName) return `${story.firstName} ${story.lastName || ""}`.trim();
    return story.username || "Người dùng";
  };

  const isVideo = story?.mediaType === "video";

  const getMediaPreview = () => {
    if (isVideo && story?.thumbnailUrl) {
      return story.thumbnailUrl;
    }
    return story?.thumbnailUrl;
  };

  const renderAddStory = () => (
    <div
      onClick={handleOpenStory}
      className="cursor-pointer w-28 h-48 rounded-2xl overflow-hidden bg-gradient-to-br from-sky-50 to-blue-50 relative group shadow-md hover:shadow-xl transition-all duration-300 border border-sky-100"
    >
      {/* Subtle gradient overlay */}
      <div className="absolute inset-0 bg-gradient-to-t from-sky-100/30 to-transparent group-hover:from-sky-200/40 transition-all duration-300" />

      {/* Plus icon with animated ring */}
      <div className="absolute inset-0 flex items-center justify-center">
        <div className="relative">
          {/* Animated ring */}
          <div className="absolute inset-0 w-16 h-16 rounded-full bg-sky-400/20 animate-ping" />
          {/* Main button */}
          <div className="relative w-16 h-16 rounded-full bg-gradient-to-br from-sky-400 to-blue-500 flex items-center justify-center text-white text-3xl font-light shadow-lg group-hover:scale-110 group-hover:shadow-xl transition-all duration-300">
            <span className="transform group-hover:rotate-90 transition-transform duration-300">+</span>
          </div>
        </div>
      </div>

      {/* Label with gradient text */}
      <div className="absolute bottom-3 left-0 right-0 text-center">
        <span className="text-sm font-semibold bg-gradient-to-r from-sky-600 to-blue-600 bg-clip-text text-transparent drop-shadow">
          Tạo tin
        </span>
      </div>
    </div>
  );

  const renderStoryItem = () => (
    <div
      onClick={handleOpenStory}
      className="cursor-pointer w-28 h-48 rounded-2xl overflow-hidden relative group shadow-md hover:shadow-xl transition-all duration-300"
    >
      {/* Media content */}
      {isVideo ? (
        <video
          src={story?.mediaUrl}
          className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
          muted
          loop
          autoPlay
          playsInline
        />
      ) : (
        <img
          src={getMediaPreview()}
          alt="story"
          className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
        />
      )}

      {/* Gradient overlays */}
      <div className="absolute inset-0 bg-gradient-to-t from-black/70 via-black/20 to-transparent" />
      <div className="absolute inset-0 bg-sky-500/0 group-hover:bg-sky-500/10 transition-colors duration-300" />

      {/* Avatar with sky blue ring */}
      <div className="absolute top-3 left-3 w-11 h-11 rounded-full bg-gradient-to-br from-sky-400 to-blue-500 p-0.5 shadow-lg">
        <div className="w-full h-full rounded-full overflow-hidden bg-white p-0.5">
          <img
            src={user?.imageUrl || "https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_640.png"}
            alt="avatar"
            className="w-full h-full object-cover rounded-full"
          />
        </div>
      </div>

      {/* Username with better readability */}
      <div className="absolute bottom-3 left-3 right-3">
        <p className="text-sm font-semibold text-white line-clamp-1 drop-shadow-lg">
          {getDisplayName()}
        </p>
      </div>

      {/* Video indicator */}
      {isVideo && (
        <div className="absolute top-3 right-3 bg-gradient-to-br from-sky-400 to-blue-500 rounded-full p-1.5 shadow-lg">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            className="h-4 w-4 text-white"
            fill="currentColor"
            viewBox="0 0 24 24"
          >
            <path d="M8 5v14l11-7z" />
          </svg>
        </div>
      )}
    </div>
  );

  return (
    <>
      {isAddNew ? renderAddStory() : renderStoryItem()}
      {!isAddNew && (
        <StoryModal
          isOpen={isOpen}
          onClose={() => setIsOpen(false)}
          stories={userStories}
          story={story}
        />
      )}
      {isAddNew && showCreateModal && (
        <AddStoryModal
          onClose={() => setShowCreateModal(false)}
          onStoryCreated={handleStoryCreated}
        />
      )}
    </>
  );
};

export default StoryCircle;