import React, { useEffect, useState, ChangeEvent, FormEvent } from "react";
import DatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";
import { fetchUserInfo, updateUserProfile, deleteAccount } from "../../api/userApi";
import { useToast } from "@chakra-ui/react";
import { useNavigate } from "react-router-dom";
import { uploadToCloudnary } from "../../Config/UploadToCloudinary";
import { MdPhotoCamera, MdArrowBack } from "react-icons/md";

interface FormData {
    firstName: string;
    lastName: string;
    bio: string;
    city: string;
    phone: string;
    email: string;
    gender: string;
    website: string;
    imageUrl: string;
    address: string;
    dob: string;
    [key: string]: unknown;
}

const EditAccountPage: React.FC = () => {
    const [formData, setFormData] = useState<FormData>({
        firstName: "",
        lastName: "",
        bio: "",
        city: "",
        phone: "",
        email: "",
        gender: "",
        website: "",
        imageUrl: "",
        address: "",
        dob: "",
    });

    const [isLoading, setIsLoading] = useState(false);
    const [isUploading, setIsUploading] = useState(false);
    const [showDeleteModal, setShowDeleteModal] = useState(false);
    const [deleteLoading, setDeleteLoading] = useState(false);
    const [currentUserId, setCurrentUserId] = useState<string>('');
    const toast = useToast();
    const navigate = useNavigate();

    useEffect(() => {
        const fetchUser = async () => {
            try {
                const userInfo = await fetchUserInfo();
                if (userInfo) {
                    setCurrentUserId(userInfo.userId || '');
                    setFormData({
                        firstName: userInfo.firstName || "",
                        lastName: userInfo.lastName || "",
                        bio: userInfo.bio || "",
                        city: userInfo.city || "",
                        phone: userInfo.phone || "",
                        email: userInfo.email || "",
                        gender: userInfo.gender || "",
                        website: userInfo.website || "",
                        imageUrl: userInfo.imageUrl || "",
                        address: userInfo.address || "",
                        dob: typeof userInfo.dob === 'string' ? userInfo.dob : "",
                    });
                }
            } catch (error) {
                toast({
                    title: "Failed to load user info",
                    status: "error",
                    duration: 3000,
                    isClosable: true,
                    position: "top-right",
                });
            }
        };
        fetchUser();
    }, [toast]);

    const handleChange = (e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
        const { name, value } = e.target;
        setFormData((prev) => ({ ...prev, [name]: value }));
    };

    const handleImageChange = async (e: ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        if (!file) return;

        // Validate file type
        if (!file.type.startsWith('image/')) {
            toast({
                title: "Invalid file type",
                description: "Please select an image file",
                status: "error",
                duration: 3000,
                isClosable: true,
                position: "top-right",
            });
            return;
        }

        // Validate file size (10MB max)
        if (file.size > 10485760) {
            toast({
                title: "File too large",
                description: "Please select an image smaller than 10MB",
                status: "error",
                duration: 3000,
                isClosable: true,
                position: "top-right",
            });
            return;
        }
        try {
            setIsUploading(true);
            const uploadedUrl = await uploadToCloudnary(file);
            if (!uploadedUrl) {
                throw new Error("Upload failed - no URL returned");
            }

            if (typeof uploadedUrl !== 'string' || uploadedUrl.trim() === '') {
                throw new Error("Invalid URL returned");
            }

            setFormData((prev) => ({
                ...prev,
                imageUrl: uploadedUrl
            }));

            toast({
                title: "Image uploaded successfully",
                status: "success",
                duration: 2000,
                position: "top-right",
                isClosable: true,
            });
        } catch (err) {
            const error = err as Error;
            toast({
                title: "Image upload failed",
                description: error.message || "Please try again",
                status: "error",
                duration: 3000,
                isClosable: true,
                position: "top-right",
            });
        } finally {
            setIsUploading(false);
        }
    };

    const handleDeleteAccount = async () => {
        setDeleteLoading(true);
        try {
            await deleteAccount(currentUserId);
            toast({
                title: "Account deleted",
                description: "Your account has been permanently deleted.",
                status: "success",
                duration: 3000,
                position: "top-right",
                isClosable: true,
            });
            navigate("/login");
        } catch (error) {
            const err = error as Error;
            toast({
                title: "Delete failed",
                description: err.message || "Something went wrong",
                status: "error",
                duration: 3000,
                position: "top-right",
                isClosable: true,
            });
        } finally {
            setDeleteLoading(false);
            setShowDeleteModal(false);
        }
    };

    const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        setIsLoading(true);
        try {
            // ⭐ Sử dụng API functions thay vì raw axios với manual token
            const userInfo = await fetchUserInfo();
            await updateUserProfile(userInfo.id, formData);

            toast({
                title: "Profile updated successfully!",
                status: "success",
                duration: 3000,
                position: "top-right",
                isClosable: true,
            });

            navigate("/profile");
        } catch (error) {
            const err = error as Error;
            toast({
                title: "Update failed",
                description: err.message || "Something went wrong",
                status: "error",
                duration: 3000,
                position: "top-right",
                isClosable: true,
            });
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="min-h-screen bg-gradient-to-b from-gray-50 to-white py-8">
            <div className="max-w-3xl mx-auto px-4 sm:px-6">
                {/* Header */}
                <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6 mb-6">
                    <div className="flex items-center gap-4">
                        <button
                            onClick={() => navigate("/profile")}
                            className="p-2 rounded-full hover:bg-gray-100 transition-colors"
                        >
                            <MdArrowBack className="w-6 h-6 text-gray-600" />
                        </button>
                        <div className="flex-1">
                            <h2 className="text-2xl font-bold bg-gradient-to-r from-sky-600 to-blue-600 bg-clip-text text-transparent">
                                Edit Profile
                            </h2>
                            <p className="text-sm text-gray-500 mt-1">
                                Update your personal information
                            </p>
                        </div>
                    </div>
                </div>

                {/* Form Container */}
                <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6 sm:p-8 mb-6">
                    <form onSubmit={handleSubmit} className="space-y-6">
                        {/* Avatar Upload */}
                        <div className="flex flex-col items-center space-y-4">
                            <div className="relative group">
                                <div className="absolute inset-0 bg-gradient-to-br from-sky-400 to-blue-500 rounded-full blur-md opacity-20 group-hover:opacity-30 transition-opacity"></div>
                                <img
                                    src={
                                        formData.imageUrl ||
                                        "https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_640.png"
                                    }
                                    alt="avatar"
                                    className="relative w-28 h-28 rounded-full object-cover border-4 border-white shadow-lg"
                                />
                                <label className="absolute bottom-0 right-0 w-10 h-10 bg-gradient-to-br from-sky-400 to-blue-500 rounded-full flex items-center justify-center cursor-pointer shadow-lg hover:scale-110 transition-transform">
                                    {isUploading ? (
                                        <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                                    ) : (
                                        <MdPhotoCamera className="w-5 h-5 text-white" />
                                    )}
                                    <input
                                        type="file"
                                        accept="image/*"
                                        onChange={handleImageChange}
                                        className="hidden"
                                        disabled={isUploading}
                                    />
                                </label>
                            </div>
                            <p className="text-sm text-gray-500">
                                Click the camera icon to change your profile picture
                            </p>
                        </div>

                        {/* Name Fields */}
                        <div className="space-y-4">
                            <label className="block text-sm font-semibold text-gray-700">
                                Full Name
                            </label>
                            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                                <input
                                    type="text"
                                    name="firstName"
                                    placeholder="First Name"
                                    value={formData.firstName}
                                    onChange={handleChange}
                                    className="border border-gray-200 rounded-xl px-4 py-3 focus:outline-none focus:ring-2 focus:ring-sky-400 focus:border-transparent transition-all"
                                />
                                <input
                                    type="text"
                                    name="lastName"
                                    placeholder="Last Name"
                                    value={formData.lastName}
                                    onChange={handleChange}
                                    className="border border-gray-200 rounded-xl px-4 py-3 focus:outline-none focus:ring-2 focus:ring-sky-400 focus:border-transparent transition-all"
                                />
                            </div>
                        </div>

                        {/* Date of Birth */}
                        <div className="space-y-4">
                            <label className="block text-sm font-semibold text-gray-700">
                                Date of Birth
                            </label>
                            <DatePicker
                                selected={formData.dob ? new Date(formData.dob) : null}
                                onChange={(date: Date | null) =>
                                    setFormData((prev) => ({
                                        ...prev,
                                        dob: date ? date.toISOString().split("T")[0] : "",
                                    }))
                                }
                                dateFormat="yyyy-MM-dd"
                                showMonthDropdown
                                showYearDropdown
                                dropdownMode="select"
                                placeholderText="Select Date of Birth"
                                className="w-full border border-gray-200 rounded-xl px-4 py-3 focus:outline-none focus:ring-2 focus:ring-sky-400 focus:border-transparent transition-all"
                            />
                        </div>

                        {/* Bio */}
                        <div className="space-y-4">
                            <label className="block text-sm font-semibold text-gray-700">
                                Bio
                            </label>
                            <textarea
                                name="bio"
                                placeholder="Tell us about yourself..."
                                value={formData.bio}
                                onChange={handleChange}
                                rows={4}
                                className="w-full border border-gray-200 rounded-xl px-4 py-3 resize-none focus:outline-none focus:ring-2 focus:ring-sky-400 focus:border-transparent transition-all"
                            />
                        </div>

                        {/* Location */}
                        <div className="space-y-4">
                            <label className="block text-sm font-semibold text-gray-700">
                                Location
                            </label>
                            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                                <input
                                    type="text"
                                    name="city"
                                    placeholder="City"
                                    value={formData.city}
                                    onChange={handleChange}
                                    className="border border-gray-200 rounded-xl px-4 py-3 focus:outline-none focus:ring-2 focus:ring-sky-400 focus:border-transparent transition-all"
                                />
                                <input
                                    type="text"
                                    name="address"
                                    placeholder="Address"
                                    value={formData.address}
                                    onChange={handleChange}
                                    className="border border-gray-200 rounded-xl px-4 py-3 focus:outline-none focus:ring-2 focus:ring-sky-400 focus:border-transparent transition-all"
                                />
                            </div>
                        </div>

                        {/* Contact Info */}
                        <div className="space-y-4">
                            <label className="block text-sm font-semibold text-gray-700">
                                Contact Information
                            </label>
                            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                                <input
                                    type="email"
                                    name="email"
                                    placeholder="Email"
                                    value={formData.email}
                                    onChange={handleChange}
                                    className="border border-gray-200 rounded-xl px-4 py-3 focus:outline-none focus:ring-2 focus:ring-sky-400 focus:border-transparent transition-all"
                                />
                                <input
                                    type="tel"
                                    name="phone"
                                    placeholder="Phone"
                                    value={formData.phone}
                                    onChange={handleChange}
                                    className="border border-gray-200 rounded-xl px-4 py-3 focus:outline-none focus:ring-2 focus:ring-sky-400 focus:border-transparent transition-all"
                                />
                            </div>
                        </div>

                        {/* Additional Info */}
                        <div className="space-y-4">
                            <label className="block text-sm font-semibold text-gray-700">
                                Additional Information
                            </label>
                            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                                <select
                                    name="gender"
                                    value={formData.gender}
                                    onChange={handleChange}
                                    className="border border-gray-200 rounded-xl px-4 py-3 focus:outline-none focus:ring-2 focus:ring-sky-400 focus:border-transparent transition-all bg-white"
                                >
                                    <option value="">Select Gender</option>
                                    <option value="male">Male</option>
                                    <option value="female">Female</option>
                                    <option value="other">Other</option>
                                </select>
                                <input
                                    type="text"
                                    name="website"
                                    placeholder="Website"
                                    value={formData.website}
                                    onChange={handleChange}
                                    className="border border-gray-200 rounded-xl px-4 py-3 focus:outline-none focus:ring-2 focus:ring-sky-400 focus:border-transparent transition-all"
                                />
                            </div>
                        </div>

                        {/* Action Buttons */}
                        <div className="flex gap-4 pt-6">
                            <button
                                type="button"
                                onClick={() => navigate("/profile")}
                                className="flex-1 px-6 py-3 rounded-xl font-semibold text-gray-600 bg-gray-100 hover:bg-gray-200 transition-all"
                            >
                                Cancel
                            </button>
                            <button
                                type="submit"
                                disabled={isLoading}
                                className="flex-1 px-6 py-3 rounded-xl font-semibold text-white bg-gradient-to-r from-sky-400 to-blue-500 hover:from-sky-500 hover:to-blue-600 transition-all shadow-lg hover:shadow-xl hover:-translate-y-0.5 disabled:opacity-50 disabled:cursor-not-allowed"
                            >
                                {isLoading ? (
                                    <span className="flex items-center justify-center gap-2">
                                        <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                                        Saving...
                                    </span>
                                ) : (
                                    "Save Changes"
                                )}
                            </button>
                        </div>
                    </form>
                </div>
                {/* Danger Zone */}
                <div className="bg-white rounded-2xl shadow-sm border border-red-100 p-6">
                    <h3 className="text-base font-semibold text-red-600 mb-1">Danger Zone</h3>
                    <p className="text-sm text-gray-500 mb-4">
                        Permanently delete your account and all associated data. This action cannot be undone.
                    </p>
                    <button
                        type="button"
                        onClick={() => setShowDeleteModal(true)}
                        className="px-5 py-2.5 rounded-xl text-sm font-semibold text-white bg-red-500 hover:bg-red-600 transition-colors"
                    >
                        Delete Account
                    </button>
                </div>
            </div>

            {/* Delete Confirmation Modal */}
            {showDeleteModal && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 px-4">
                    <div className="bg-white rounded-2xl shadow-xl max-w-md w-full p-6">
                        <div className="flex items-center gap-3 mb-4">
                            <div className="w-10 h-10 rounded-full bg-red-100 flex items-center justify-center flex-shrink-0">
                                <svg className="w-5 h-5 text-red-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z" />
                                </svg>
                            </div>
                            <div>
                                <h3 className="text-base font-bold text-gray-900">Delete your account?</h3>
                                <p className="text-sm text-gray-500">This action is permanent and cannot be undone.</p>
                            </div>
                        </div>
                        <p className="text-sm text-gray-600 mb-6">
                            All your posts, followers, messages, and personal data will be permanently removed.
                        </p>
                        <div className="flex gap-3">
                            <button
                                type="button"
                                onClick={() => setShowDeleteModal(false)}
                                disabled={deleteLoading}
                                className="flex-1 px-4 py-2.5 rounded-xl text-sm font-semibold text-gray-700 bg-gray-100 hover:bg-gray-200 transition-colors disabled:opacity-50"
                            >
                                Cancel
                            </button>
                            <button
                                type="button"
                                onClick={handleDeleteAccount}
                                disabled={deleteLoading}
                                className="flex-1 px-4 py-2.5 rounded-xl text-sm font-semibold text-white bg-red-500 hover:bg-red-600 transition-colors disabled:opacity-50 flex items-center justify-center gap-2"
                            >
                                {deleteLoading ? (
                                    <>
                                        <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                                        Deleting...
                                    </>
                                ) : (
                                    "Yes, delete my account"
                                )}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default EditAccountPage;
