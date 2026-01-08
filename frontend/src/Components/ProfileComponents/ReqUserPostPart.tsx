import React, { useEffect, useState } from "react";
import { BsFillBookmarkFill } from "react-icons/bs";
import { MdGridOn } from "react-icons/md";
import ReqUserPostCard from "./ReqUserPostCard";
import { fetchUserPosts } from "../../api/postApi";
import { getToken } from "../../service/LocalStorageService";

const ReqUserPostPart = () => {
  const [activeTab, setActiveTab] = useState("Post");
  const [posts, setPosts] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const token = getToken();

  useEffect(() => {
    const getUserPosts = async () => {
      try {
        setIsLoading(true);
        const result = await fetchUserPosts(token);
        setPosts(result);
      } catch (error) {
        console.error("Error:", error);
      } finally {
        setIsLoading(false);
      }
    };

    if (token) {
      getUserPosts();
    }
  }, [token]);

  const tabs = [
    { tab: "Post", icon: <MdGridOn className="w-5 h-5" />, label: "Posts" },
    { tab: "Saved", icon: <BsFillBookmarkFill className="w-5 h-5" />, label: "Saved" },
  ];

  const LoadingSkeleton = () => (
    <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-4">
      {[...Array(6)].map((_, index) => (
        <div
          key={index}
          className="w-full aspect-square rounded-2xl bg-gradient-to-br from-sky-50 to-blue-50 animate-pulse"
        >
          <div className="w-full h-full flex items-center justify-center">
            <div className="w-16 h-16 rounded-full bg-white/50"></div>
          </div>
        </div>
      ))}
    </div>
  );

  const EmptyState = () => (
    <div className="flex flex-col items-center justify-center py-20">
      <div className="w-24 h-24 mb-6 rounded-full bg-gradient-to-br from-sky-100 to-blue-100 flex items-center justify-center">
        {activeTab === "Post" && <MdGridOn className="w-12 h-12 text-sky-400" />}
        {activeTab === "Saved" && <BsFillBookmarkFill className="w-12 h-12 text-sky-400" />}
      </div>
      <h3 className="text-xl font-bold text-gray-800 mb-2">
        {activeTab === "Post" && "No posts yet"}
        {activeTab === "Saved" && "No saved posts"}
      </h3>
      <p className="text-sm text-gray-500 text-center max-w-sm">
        {activeTab === "Post" && "Share your first photo or video to get started"}
        {activeTab === "Saved" && "Save posts that you want to see again"}
      </p>
    </div>
  );

  return (
    <div className="mt-8 pb-10">
      {/* Tabs Navigation */}
      <div className="border-t border-gray-200">
        <div className="flex justify-center gap-12 py-4">
          {tabs.map((item) => (
            <button
              key={item.tab}
              onClick={() => setActiveTab(item.tab)}
              className={`group flex items-center gap-2 px-4 py-2 rounded-lg transition-all duration-200 ${
                activeTab === item.tab
                  ? "text-sky-600 font-semibold"
                  : "text-gray-500 hover:text-gray-700"
              }`}
            >
              <div
                className={`transition-all duration-200 ${
                  activeTab === item.tab
                    ? "scale-110"
                    : "group-hover:scale-105"
                }`}
              >
                {item.icon}
              </div>
              <span className="text-sm font-medium uppercase tracking-wide">
                {item.label}
              </span>
              {activeTab === item.tab && (
                <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-gradient-to-r from-sky-400 to-blue-500"></div>
              )}
            </button>
          ))}
        </div>
        
        {/* Active tab indicator line */}
        <div className="relative h-0.5 bg-gradient-to-r from-transparent via-sky-400 to-transparent opacity-50"></div>
      </div>

      {/* Content */}
      <div className="px-4 pt-8">
        {isLoading ? (
          <LoadingSkeleton />
        ) : posts.length === 0 ? (
          <EmptyState />
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-4 animate-fadeIn">
            {posts.map((post) => (
              <ReqUserPostCard key={post.id} post={post} />
            ))}
          </div>
        )}
      </div>

      {/* Animation styles */}
      <style>{`
        @keyframes fadeIn {
          from {
            opacity: 0;
            transform: translateY(10px);
          }
          to {
            opacity: 1;
            transform: translateY(0);
          }
        }

        .animate-fadeIn {
          animation: fadeIn 0.4s ease-out;
        }
      `}</style>
    </div>
  );
};

export default ReqUserPostPart;
