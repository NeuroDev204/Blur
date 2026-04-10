import React, { useState, useEffect, useCallback } from "react"
import { Route, Routes, useLocation, Navigate } from "react-router-dom"
import HomePage from "../HomePage/HomePage"
import Profile from "../Profile/Profile"
import MessagePage from "../MessagePage/MessagePage"
import LoginPage from "../Login/LoginPage"
import RegisterPage from "../Login/RegisterPage"
import Authenticate from "../Login/Authenticate"
import CreatePassword from "../Login/CreatePassword"
import ActivationPage from "../Login/ActivationPage"
import EditAccountPage from "../Account/EditAccountPage"
import OtherUserProfile from "../../Components/ProfileComponents/OrderUserProfile"
import SearchPage from "../Search/SearchPage"
import { SidebarComponent } from "../../Components/Sidebar/SidebarComponent"
import NotificationsPage from "../Notification/NotificationPage"
import PostDetailPage from "../../Components/Post/PostDetailPage"
import SuggestionsPage from "../Suggestions/SuggestionsPage"
import { introspectToken } from "../../api/authAPI"

const Router: React.FC = () => {
    const location = useLocation()
    const [isAuthenticated, setIsAuthenticated] = useState<boolean | null>(null)
    const [isMobile, setIsMobile] = useState(false)
    const [sidebarOpen, setSidebarOpen] = useState(false)

    const authRoutes = ["/login", "/register", "/create-password", "/activate", "/authenticate"]
    const isAuthPage = authRoutes.includes(location.pathname)

    // Check authentication status asynchronously
    useEffect(() => {
        const checkAuth = async () => {
            // ⭐ Auth pages không cần check - cho phép render
            if (isAuthPage) {
                return // Không set isAuthenticated để giữ trạng thái hiện tại
            }

            try {
                const valid = await introspectToken()
                setIsAuthenticated(valid)
            } catch (error) {
                setIsAuthenticated(false)
            }
        }
        checkAuth()
    }, [location.pathname, isAuthPage])

    // ⭐ Listen for login success event
    useEffect(() => {
        const handleLoginSuccess = () => {
            setIsAuthenticated(true)
        }

        window.addEventListener('auth-login-success', handleLoginSuccess)
        return () => window.removeEventListener('auth-login-success', handleLoginSuccess)
    }, [])

    useEffect(() => {
        const checkMobile = () => {
            setIsMobile(window.innerWidth < 768)
        }
        checkMobile()
        window.addEventListener('resize', checkMobile)
        return () => window.removeEventListener('resize', checkMobile)
    }, [])

    useEffect(() => {
        if (isMobile) {
            setSidebarOpen(false)
        }
    }, [location.pathname, isMobile])

    // ⭐ Auth pages: cho render bình thường
    if (isAuthPage) {
        return (
            <div className="flex min-h-screen">
                <div className="flex-1 min-h-screen w-full max-w-full overflow-x-hidden px-4 md:px-0">
                    <div className="w-full max-w-full">
                        <Routes>
                            <Route path="/login" element={<LoginPage />} />
                            <Route path="/register" element={<RegisterPage />} />
                            <Route path="/create-password" element={<CreatePassword />} />
                            <Route path="/activate" element={<ActivationPage />} />
                            <Route path="/authenticate" element={<Authenticate />} />
                        </Routes>
                    </div>
                </div>
            </div>
        )
    }

    // Show loading while checking auth status
    if (isAuthenticated === null) {
        return (
            <div className="min-h-screen flex items-center justify-center">
                <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-sky-500"></div>
            </div>
        )
    }

    // Not authenticated - redirect to login
    if (!isAuthenticated) {
        return <Navigate to="/login" replace />
    }

    // Authenticated - show main app
    return (
        <div className="flex min-h-screen">
            {isMobile && (
                <button
                    onClick={() => setSidebarOpen(!sidebarOpen)}
                    className="fixed top-4 left-4 z-50 p-2 bg-white rounded-md shadow-md border md:hidden"
                    aria-label="Toggle menu"
                >
                    <svg
                        className="w-6 h-6"
                        fill="none"
                        stroke="currentColor"
                        viewBox="0 0 24 24"
                    >
                        {sidebarOpen ? (
                            <path
                                strokeLinecap="round"
                                strokeLinejoin="round"
                                strokeWidth={2}
                                d="M6 18L18 6M6 6l12 12"
                            />
                        ) : (
                            <path
                                strokeLinecap="round"
                                strokeLinejoin="round"
                                strokeWidth={2}
                                d="M4 6h16M4 12h16M4 18h16"
                            />
                        )}
                    </svg>
                </button>
            )}

            {isMobile && sidebarOpen && (
                <div
                    className="fixed inset-0 bg-black bg-opacity-50 z-40 md:hidden"
                    onClick={() => setSidebarOpen(false)}
                />
            )}

            <div className="hidden md:block w-[240px] h-screen bg-white border-r flex-shrink-0">
                <SidebarComponent />
            </div>

            <div
                className={`
                    md:hidden fixed top-0 left-0 w-64 h-screen bg-white border-r z-50
                    transition-transform duration-300 ease-in-out
                    ${sidebarOpen ? 'translate-x-0' : '-translate-x-full'}
                `}
            >
                <SidebarComponent />
            </div>

            <div
                className={`
                    flex-1 min-h-screen w-full max-w-full overflow-x-clip
                    ${isMobile ? 'pt-16 px-4' : 'pl-3'}
                `}
            >
                <div className="w-full max-w-full">
                    <Routes>
                        <Route path="/" element={<HomePage />} />
                        <Route path="/profile" element={<Profile />} />
                        <Route path="/profile/user" element={<OtherUserProfile />} />
                        <Route path="/message" element={<MessagePage />} />
                        <Route path="/account/edit" element={<EditAccountPage />} />
                        <Route path="/search" element={<SearchPage />} />
                        <Route path="/notification" element={<NotificationsPage />} />
                        <Route path="/post/:postId" element={<PostDetailPage />} />
                        <Route path="/suggestions" element={<SuggestionsPage />} />
                    </Routes>
                </div>
            </div>
        </div>
    )
}

export default Router
