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
  user,
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
    if (onStoryCreated && typeof onStoryCreated === "function") {
      onStoryCreated(newStory);
    }
    setShowCreateModal(false);
  };

  const userStories = stories.length > 0 ? stories : story ? [story] : [];

  const getDisplayName = () => {
    if (isAddNew) return "Add Story";
    if (!story) return "User";
    if (story.firstName)
      return `${story.firstName} ${story.lastName || ""}`.trim();
    return story.username || "User";
  };

  const getMediaPreview = () => story?.thumbnailUrl || story?.mediaUrl;
  const isVideo = story?.mediaType === "video";
  const avatarSrc =
    user?.imageUrl ||
    "https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_640.png";

  const renderAddStory = () => (
    <div
      onClick={handleOpenStory}
      className="flex flex-col items-center gap-2 cursor-pointer group"
    >
      {/* Circle with gradient ring */}
      <div className="relative">
        {/* Outer gradient ring (dashed/dotted for "add" style) */}
        <div className="w-[68px] h-[68px] rounded-full p-[2px] bg-gradient-to-br from-gray-300 to-gray-400 group-hover:from-sky-400 group-hover:to-blue-500 transition-all duration-300 shadow-md">
          <div className="w-full h-full rounded-full overflow-hidden border-2 border-white">
            <img
              src={avatarSrc}
              alt="You"
              className="w-full h-full object-cover"
            />
          </div>
        </div>
        {/* Plus badge */}
        <div className="absolute -bottom-0.5 -right-0.5 w-6 h-6 rounded-full bg-gradient-to-br from-sky-500 to-blue-600 border-2 border-white flex items-center justify-center shadow-lg group-hover:scale-110 transition-transform duration-200">
          <span className="text-white text-sm font-bold leading-none">+</span>
        </div>
      </div>
      {/* Label */}
      <p className="text-xs font-medium text-gray-600 group-hover:text-sky-600 transition-colors duration-200 text-center w-16 truncate">
        {getDisplayName()}
      </p>
    </div>
  );

  const renderStoryItem = () => (
    <div
      onClick={handleOpenStory}
      className="flex flex-col items-center gap-2 cursor-pointer group"
    >
      {/* Circle with colorful gradient ring */}
      <div className="relative">
        {/* Gradient ring */}
        <div className="w-[68px] h-[68px] rounded-full p-[2.5px] bg-gradient-to-tr from-yellow-400 via-pink-500 to-purple-600 group-hover:from-pink-500 group-hover:via-orange-400 group-hover:to-yellow-400 transition-all duration-300 shadow-lg group-hover:shadow-pink-200">
          <div className="w-full h-full rounded-full overflow-hidden border-2 border-white">
            {isVideo ? (
              <video
                src={story?.mediaUrl}
                className="w-full h-full object-cover"
                muted
                playsInline
              />
            ) : (
              <img
                src={getMediaPreview()}
                alt="story"
                className="w-full h-full object-cover group-hover:scale-110 transition-transform duration-300"
              />
            )}
          </div>
        </div>
        {/* Video badge */}
        {isVideo && (
          <div className="absolute -bottom-0.5 -right-0.5 w-5 h-5 rounded-full bg-gradient-to-br from-pink-500 to-purple-600 border-2 border-white flex items-center justify-center shadow">
            <svg
              className="w-2.5 h-2.5 text-white"
              fill="currentColor"
              viewBox="0 0 24 24"
            >
              <path d="M8 5v14l11-7z" />
            </svg>
          </div>
        )}
      </div>
      {/* Username */}
      <p className="text-xs font-medium text-gray-700 group-hover:text-pink-600 transition-colors duration-200 text-center w-16 truncate">
        {getDisplayName()}
      </p>
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
