
// ============= AddStoryModal.tsx =============
import React, { useState, useRef } from "react";
import { useToast } from "@chakra-ui/react";
import { uploadToCloudnary } from "../../Config/UploadToCloudinary";
import { createStory } from "../../api/storyApi";
import { BsEmojiSmile, BsImage, BsCameraVideo } from "react-icons/bs";
import { MdClose } from "react-icons/md";
import EmojiPicker, { EmojiClickData } from "emoji-picker-react";

interface Story {
  id?: string;
  content?: string;
  mediaUrl?: string;
  mediaType?: string;
  thumbnailUrl?: string | null;
  timestamp?: number;
  [key: string]: unknown;
}

interface AddStoryModalProps {
  onClose: () => void;
  onStoryCreated?: (story: Story) => void;
}

const AddStoryModal: React.FC<AddStoryModalProps> = ({ onClose, onStoryCreated }) => {
  const [media, setMedia] = useState<File | null>(null);
  const [caption, setCaption] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [showEmojiPicker, setShowEmojiPicker] = useState(false);
  const toast = useToast();
  const videoRef = useRef<HTMLVideoElement>(null);

  const handleMediaChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      if (!file.type.startsWith('image/') && !file.type.startsWith('video/')) {
        setError("Vui lòng chọn file hình ảnh hoặc video");
        return;
      }
      if (file.size > 10 * 1024 * 1024) {
        setError("File vượt quá kích thước cho phép (10MB)");
        return;
      }
      setMedia(file);
      setError("");
    }
  };

  const generateThumbnail = (videoFile) => {
    return new Promise((resolve, reject) => {
      const video = document.createElement('video');
      video.preload = 'metadata';
      video.onloadedmetadata = () => {
        video.currentTime = Math.min(video.duration / 3, 2);
      };
      video.onseeked = () => {
        const canvas = document.createElement('canvas');
        canvas.width = video.videoWidth;
        canvas.height = video.videoHeight;
        const ctx = canvas.getContext('2d');
        ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
        canvas.toBlob((blob) => {
          if (blob) {
            const thumbnailFile = new File([blob], `thumbnail_${videoFile.name.split('.')[0]}.jpg`, {
              type: 'image/jpeg',
              lastModified: Date.now(),
            });
            resolve(thumbnailFile);
          } else {
            reject(new Error("Failed to generate thumbnail"));
          }
        }, 'image/jpeg', 0.7);
      };
      video.onerror = () => reject(new Error("Error loading video"));
      video.src = URL.createObjectURL(videoFile);
      video.load();
    });
  };

  const handlePost = async () => {
    if (!media) {
      setError("Vui lòng chọn hình ảnh hoặc video");
      return;
    }
    setLoading(true);
    setError("");
    try {
      let mediaUrl = null;
      let thumbnailUrl = null;
      const isVideo = media.type.startsWith('video/');
      mediaUrl = await uploadToCloudnary(media);
      if (isVideo) {
        try {
          const thumbnailFile = await generateThumbnail(media as File);
          thumbnailUrl = await uploadToCloudnary(thumbnailFile as File);
        } catch (thumbnailError) {
          console.error("Error generating thumbnail:", thumbnailError);
        }
      }
      const storyData = {
        content: caption,
        mediaUrl,
        mediaType: isVideo ? "video" : "image",
        thumbnailUrl: thumbnailUrl || (isVideo ? null : mediaUrl),
        timestamp: Date.now()
      };
      const result = await createStory(storyData);
      if (result) {
        toast({
          title: "Thành công!",
          description: "Story của bạn đã được đăng.",
          status: "success",
          duration: 3000,
          isClosable: true,
        });
        if (onStoryCreated && typeof onStoryCreated === 'function') {
          onStoryCreated(result);
        }
        onClose();
      }
    } catch (err) {
      console.error("Error creating story:", err);
      setError("Có lỗi xảy ra khi tạo story. Vui lòng thử lại sau.");
      toast({
        title: "Lỗi",
        description: "Không thể tạo story. Vui lòng thử lại.",
        status: "error",
        duration: 3000,
        isClosable: true,
      });
    } finally {
      setLoading(false);
    }
  };

  const onEmojiClick = (emojiData) => {
    setCaption((prev) => prev + emojiData.emoji);
  };

  return (
    <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50 p-4">
      <div className="bg-white w-full max-w-3xl h-[500px] rounded-2xl flex overflow-hidden shadow-2xl">
        {/* Left side – Preview */}
        <div className="flex-1 flex items-center justify-center bg-gradient-to-br from-sky-50 via-blue-50 to-sky-100 relative">
          {media ? (
            media.type.startsWith("video") ? (
              <video ref={videoRef} controls className="max-h-full max-w-full object-contain rounded-lg shadow-lg">
                <source src={URL.createObjectURL(media)} />
              </video>
            ) : (
              <img src={URL.createObjectURL(media)} alt="preview" className="max-h-full max-w-full object-contain rounded-lg shadow-lg" />
            )
          ) : (
            <div className="text-center">
              <div className="w-24 h-24 mx-auto mb-4 rounded-full bg-gradient-to-br from-sky-400 to-blue-500 flex items-center justify-center">
                <BsImage className="w-12 h-12 text-white" />
              </div>
              <p className="text-gray-400 text-sm font-medium">Chưa chọn media</p>
            </div>
          )}
        </div>

        {/* Right side – Form */}
        <div className="w-80 p-6 flex flex-col justify-between relative bg-white">
          {/* Close button */}
          <button
            onClick={onClose}
            className="absolute top-4 right-4 w-8 h-8 rounded-full bg-gray-100 hover:bg-gray-200 flex items-center justify-center transition-colors"
          >
            <MdClose className="w-5 h-5 text-gray-600" />
          </button>

          <div>
            <h2 className="text-xl font-bold mb-1 bg-gradient-to-r from-sky-600 to-blue-600 bg-clip-text text-transparent">
              Tạo Story Mới
            </h2>
            <p className="text-xs text-gray-500 mb-4">Chia sẻ khoảnh khắc của bạn</p>

            {/* Caption textarea */}
            <div className="relative mb-4">
              <textarea
                placeholder="Viết caption cho story..."
                value={caption}
                onChange={(e) => setCaption(e.target.value)}
                className="w-full border border-gray-200 rounded-xl p-3 pr-10 resize-none h-28 text-sm focus:outline-none focus:ring-2 focus:ring-sky-400 focus:border-transparent transition-all"
              />
              <button
                type="button"
                onClick={() => setShowEmojiPicker(!showEmojiPicker)}
                className="absolute bottom-3 right-3 text-xl text-gray-400 hover:text-sky-500 transition-colors"
              >
                <BsEmojiSmile />
              </button>
              {showEmojiPicker && (
                <div className="absolute z-50 top-full right-0 mt-2 shadow-xl rounded-xl overflow-hidden">
                  <EmojiPicker onEmojiClick={onEmojiClick} height={320} width={280} />
                </div>
              )}
            </div>

            {/* File upload button */}
            <label className="block border-2 border-dashed border-sky-200 rounded-xl p-4 text-center cursor-pointer hover:border-sky-400 hover:bg-sky-50/50 transition-all group">
              <div className="flex flex-col items-center gap-2">
                <div className="w-12 h-12 rounded-full bg-gradient-to-br from-sky-400 to-blue-500 flex items-center justify-center group-hover:scale-110 transition-transform">
                  <BsCameraVideo className="w-6 h-6 text-white" />
                </div>
                <span className="text-sm font-semibold text-gray-700">
                  {media ? media.name : "Chọn hình ảnh/video"}
                </span>
                <span className="text-xs text-gray-400">Tối đa 10MB</span>
              </div>
              <input type="file" accept="image/*,video/*" onChange={handleMediaChange} className="hidden" />
            </label>

            {error && (
              <div className="mt-3 p-2 bg-red-50 border border-red-200 rounded-lg">
                <p className="text-red-600 text-xs">{error}</p>
              </div>
            )}
          </div>

          {/* Action buttons */}
          <div className="flex gap-3 mt-4">
            <button
              onClick={onClose}
              className="flex-1 px-4 py-2.5 rounded-xl text-sm font-medium text-gray-600 hover:bg-gray-100 transition-colors"
            >
              Hủy
            </button>
            <button
              onClick={handlePost}
              disabled={!media || loading}
              className={`flex-1 px-4 py-2.5 rounded-xl text-sm font-semibold text-white transition-all ${!media || loading
                ? "bg-gray-300 cursor-not-allowed"
                : "bg-gradient-to-r from-sky-400 to-blue-500 hover:from-sky-500 hover:to-blue-600 shadow-lg hover:shadow-xl"
                }`}
            >
              {loading ? (
                <span className="flex items-center justify-center gap-2">
                  <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  Đang đăng...
                </span>
              ) : (
                "Đăng"
              )}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default AddStoryModal;