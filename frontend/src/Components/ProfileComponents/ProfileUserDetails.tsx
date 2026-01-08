import React, { useEffect, useState } from "react";
import { LuCircleDashed } from "react-icons/lu";
import { MdEdit } from "react-icons/md";
import { useNavigate } from "react-router-dom";
import { getToken } from "../../service/LocalStorageService";
import { fetchUserInfo, getFollowers, getFollowings } from "../../api/userApi";
import { fetchUserPosts } from "../../api/postApi";

const ProfileUserDetails = () => {
  const navigate = useNavigate();
  const [posts, setPosts] = useState([]);
  const [user, setUser] = useState(null);
  const [followers, setFollowers] = useState([]);
  const [followings, setFollowings] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const token = getToken();

  useEffect(() => {
    const getUserInfo = async () => {
      try {
        setIsLoading(true);
        const result = await fetchUserInfo(token);
        setUser(result);

        if (result?.id) {
          const [followerData, followingData] = await Promise.all([
            getFollowers(result.id, token),
            getFollowings(result.id, token),
          ]);
          setFollowers(followerData || []);
          setFollowings(followingData || []);
        }
      } catch (error) {
        console.log("Error fetching user:", error);
      } finally {
        setIsLoading(false);
      }
    };

    const getUserPosts = async () => {
      try {
        const result = await fetchUserPosts(token);
        setPosts(result);
      } catch (error) {
        console.log("Error fetching posts:", error);
      }
    };

    if (token) {
      getUserInfo();
      getUserPosts();
    }
  }, [token]);

  if (isLoading) {
    return (
      <div className="py-10 w-full px-4">
        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-8 animate-pulse">
          <div className="flex flex-col md:flex-row items-center gap-8">
            <div className="w-32 h-32 rounded-full bg-gradient-to-br from-sky-100 to-blue-100"></div>
            <div className="flex-1 space-y-4 w-full">
              <div className="h-6 bg-gray-200 rounded-lg w-48"></div>
              <div className="h-4 bg-gray-100 rounded-lg w-64"></div>
              <div className="h-4 bg-gray-100 rounded-lg w-32"></div>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="py-10 w-full px-4">
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-8 hover:shadow-md transition-shadow duration-300">
        <div className="flex flex-col md:flex-row items-center gap-8">
          {/* Profile Image with gradient ring */}
          <div className="relative group">
            <div className="absolute inset-0 bg-gradient-to-br from-sky-400 to-blue-500 rounded-full blur-md opacity-20 group-hover:opacity-30 transition-opacity"></div>
            <img
              className="relative w-32 h-32 rounded-full object-cover border-4 border-white shadow-lg"
              src={
                user?.imageUrl ||
                "https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_640.png"
              }
              alt="Profile"
            />
            <button
              onClick={() => navigate("/account/edit")}
              className="absolute bottom-0 right-0 w-10 h-10 bg-gradient-to-br from-sky-400 to-blue-500 rounded-full flex items-center justify-center text-white shadow-lg hover:scale-110 transition-transform"
            >
              <MdEdit className="w-5 h-5" />
            </button>
          </div>

          {/* User Info */}
          <div className="flex-1 space-y-5 text-center md:text-left w-full">
            {/* Username and Actions */}
            <div className="flex flex-col md:flex-row items-center justify-center md:justify-start gap-4">
              <h2 className="text-2xl font-bold text-gray-800">
                {user?.firstName || "User"}
              </h2>
              <button
                onClick={() => navigate("/account/edit")}
                className="px-6 py-2 bg-gradient-to-r from-sky-400 to-blue-500 text-white rounded-xl font-semibold hover:from-sky-500 hover:to-blue-600 transition-all shadow-md hover:shadow-lg hover:-translate-y-0.5 active:scale-95"
              >
                Edit Profile
              </button>
              <button
                onClick={() => navigate("/account/edit")}
                className="w-10 h-10 rounded-xl bg-gray-100 hover:bg-sky-50 flex items-center justify-center transition-colors"
              >
                <LuCircleDashed className="w-5 h-5 text-gray-600 hover:text-sky-600" />
              </button>
            </div>

            {/* Stats */}
            <div className="flex justify-center md:justify-start gap-8">
              <div className="text-center">
                <div className="text-2xl font-bold bg-gradient-to-r from-sky-600 to-blue-600 bg-clip-text text-transparent">
                  {posts?.length || 0}
                </div>
                <div className="text-sm text-gray-500 font-medium">Posts</div>
              </div>
              <div className="text-center cursor-pointer group">
                <div className="text-2xl font-bold text-gray-800 group-hover:text-sky-600 transition-colors">
                  {followers.length || 0}
                </div>
                <div className="text-sm text-gray-500 font-medium">Followers</div>
              </div>
              <div className="text-center cursor-pointer group">
                <div className="text-2xl font-bold text-gray-800 group-hover:text-sky-600 transition-colors">
                  {followings.length || 0}
                </div>
                <div className="text-sm text-gray-500 font-medium">Following</div>
              </div>
            </div>

            {/* Bio */}
            <div className="space-y-1">
              <p className="font-bold text-gray-800">
                {user?.firstName} {user?.lastName}
              </p>
              {user?.bio && (
                <p className="text-gray-600 text-sm leading-relaxed max-w-md">
                  {user.bio}
                </p>
              )}
              {!user?.bio && (
                <p className="text-gray-400 text-sm italic">
                  No bio yet
                </p>
              )}
            </div>

            {/* Additional Info Pills */}
            <div className="flex flex-wrap justify-center md:justify-start gap-2 pt-2">
              {user?.email && (
                <span className="px-3 py-1 bg-sky-50 text-sky-600 text-xs font-medium rounded-full border border-sky-100">
                  📧 {user.email}
                </span>
              )}
              {user?.username && (
                <span className="px-3 py-1 bg-blue-50 text-blue-600 text-xs font-medium rounded-full border border-blue-100">
                  @{user.username}
                </span>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ProfileUserDetails;