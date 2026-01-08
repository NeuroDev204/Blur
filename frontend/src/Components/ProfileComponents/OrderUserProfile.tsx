import { useEffect, useState } from "react";
import { LuCircleDashed } from "react-icons/lu";
import { MdGridOn } from "react-icons/md";
import { BiMessageRounded } from "react-icons/bi";
import { useNavigate, useSearchParams } from "react-router-dom";
import { getToken } from "../../service/LocalStorageService";
import {
  fetchUserInfo,
  fetchUserProfileById,
  getFollowers,
  getFollowings,
  followUser,
  unfollowUser,
} from "../../api/userApi";
import { getPostsByUserId } from "../../api/postApi";
import { createConversation } from "../../service/chatApi";
import ReqUserPostCard from "./ReqUserPostCard";

const ProfileUserDetails = () => {
  const navigate = useNavigate();
  const [posts, setPosts] = useState([]);
  const [user, setUser] = useState(null);
  const [followers, setFollowers] = useState([]);
  const [followings, setFollowings] = useState([]);
  const [isFollowing, setIsFollowing] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [isActionLoading, setIsActionLoading] = useState(false);
  const [isMessageLoading, setIsMessageLoading] = useState(false);
  const [currentUser, setCurrentUser] = useState(null);
  const token = getToken();
  const [params] = useSearchParams();
  const profileId = params.get("profileId");

  useEffect(() => {
    const fetchData = async () => {
      try {
        setIsLoading(true);

        const loggedInUser = await fetchUserInfo(token);
        setCurrentUser(loggedInUser);

        const profileData = await fetchUserProfileById(profileId, token);
        setUser(profileData);

        if (profileData?.id) {
          const [followerData, followingData] = await Promise.all([
            getFollowers(profileData.id, token),
            getFollowings(profileData.id, token),
          ]);
          setFollowers(followerData || []);
          setFollowings(followingData || []);

          const isUserFollowing = followerData?.some(
            (follower) => follower.id === loggedInUser.id
          );
          setIsFollowing(isUserFollowing);
        }

        const postData = await getPostsByUserId(profileData.userId, token);
        setPosts(postData);
      } catch (error) {
        console.error("Error fetching data:", error);
      } finally {
        setIsLoading(false);
      }
    };

    if (token && profileId) {
      fetchData();
    }
  }, [profileId, token]);

  const handleFollowToggle = async () => {
    if (!currentUser) return;

    try {
      setIsActionLoading(true);
      if (isFollowing) {
        await unfollowUser(user.id, token);
        setFollowers((prev) =>
          prev.filter((follower) => follower.id !== currentUser.id)
        );
      } else {
        await followUser(user.id, token);
        setFollowers((prev) => [...prev, currentUser]);
      }
      setIsFollowing(!isFollowing);
    } catch (error) {
      console.error("Error toggling follow status:", error);
    } finally {
      setIsActionLoading(false);
    }
  };

  const handleSendMessage = async () => {
    if (!user?.id) return;

    try {
      setIsMessageLoading(true);
      
      // Gọi API tạo conversation
      const conversationData = await createConversation(
        {
          type: "DIRECT",
          participantIds: [user.userId]
        },
        token
      );

      // Navigate đến trang chat với conversationId
      navigate(`/message`);
    } catch (error) {
      console.error("Error creating conversation:", error);
      // Có thể hiển thị toast notification ở đây
      alert("Unable to start conversation. Please try again.");
    } finally {
      setIsMessageLoading(false);
    }
  };

  const isOwnProfile = currentUser?.id === user?.id;

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gradient-to-b from-gray-50 to-white py-10 px-4">
        <div className="max-w-5xl mx-auto">
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
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-b from-gray-50 to-white py-10 px-4">
      <div className="max-w-5xl mx-auto">
        {/* Profile Card */}
        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-8 mb-6 hover:shadow-md transition-shadow duration-300">
          <div className="flex flex-col md:flex-row items-center gap-8">
            {/* Avatar */}
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
            </div>

            {/* User Info */}
            <div className="flex-1 space-y-5 text-center md:text-left w-full">
              {/* Username and Actions */}
              <div className="flex flex-col md:flex-row items-center justify-center md:justify-start gap-4">
                <h2 className="text-2xl font-bold text-gray-800">
                  {user?.firstName} {user?.lastName}
                </h2>
                
                {isOwnProfile ? (
                  <div className="flex gap-3">
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
                ) : (
                  <div className="flex gap-3">
                    <button
                      onClick={handleFollowToggle}
                      disabled={isActionLoading}
                      className={`px-8 py-2.5 rounded-xl font-semibold transition-all shadow-md hover:shadow-lg hover:-translate-y-0.5 active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed ${
                        isFollowing
                          ? "bg-gray-100 text-gray-800 hover:bg-gray-200"
                          : "bg-gradient-to-r from-sky-400 to-blue-500 text-white hover:from-sky-500 hover:to-blue-600"
                      }`}
                    >
                      {isActionLoading ? (
                        <span className="flex items-center gap-2">
                          <div className="w-4 h-4 border-2 border-current border-t-transparent rounded-full animate-spin"></div>
                          Loading...
                        </span>
                      ) : isFollowing ? (
                        "Following"
                      ) : (
                        "Follow"
                      )}
                    </button>

                    <button
                      onClick={handleSendMessage}
                      disabled={isMessageLoading}
                      className="px-6 py-2.5 bg-white border-2 border-gray-200 text-gray-800 rounded-xl font-semibold hover:bg-gray-50 hover:border-sky-300 transition-all shadow-md hover:shadow-lg hover:-translate-y-0.5 active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
                    >
                      {isMessageLoading ? (
                        <>
                          <div className="w-4 h-4 border-2 border-gray-400 border-t-transparent rounded-full animate-spin"></div>
                          <span>Loading...</span>
                        </>
                      ) : (
                        <>
                          <BiMessageRounded className="w-5 h-5" />
                          <span>Message</span>
                        </>
                      )}
                    </button>
                  </div>
                )}
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
                {user?.bio ? (
                  <p className="text-gray-600 text-sm leading-relaxed max-w-md">
                    {user.bio}
                  </p>
                ) : (
                  <p className="text-gray-400 text-sm italic">No bio yet</p>
                )}
              </div>
            </div>
          </div>
        </div>

        {/* Posts Section */}
        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6">
          <div className="flex items-center gap-3 mb-6">
            <MdGridOn className="w-6 h-6 text-sky-500" />
            <h3 className="text-xl font-bold text-gray-800">Posts</h3>
            <span className="px-3 py-1 bg-sky-50 text-sky-600 text-xs font-semibold rounded-full">
              {posts.length}
            </span>
          </div>

          {posts.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-20">
              <div className="w-24 h-24 bg-gradient-to-br from-sky-100 to-blue-100 rounded-full flex items-center justify-center mb-6">
                <MdGridOn className="w-12 h-12 text-sky-400" />
              </div>
              <h3 className="text-xl font-bold text-gray-800 mb-2">
                No posts yet
              </h3>
              <p className="text-sm text-gray-500 text-center max-w-sm">
                {isOwnProfile 
                  ? "Share your first photo or video to get started"
                  : `${user?.firstName} hasn't posted anything yet`
                }
              </p>
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-4">
              {posts.map((post) => (
                <ReqUserPostCard key={post.id} post={post} />
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default ProfileUserDetails;