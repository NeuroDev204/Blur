import React, { useState, useEffect, useCallback, ChangeEvent, FormEvent } from "react"
import { FaGoogle, FaEye, FaEyeSlash } from "react-icons/fa"
import { useNavigate } from "react-router-dom"
import { useToast } from "@chakra-ui/react"
import { OAuthConfig } from "../../Config/configuration"
import { setToken } from "../../service/LocalStorageService"
import axios from "axios"

const STORAGE_KEY = "rememberedCredentials"
const API_LOGIN_URL = "http://localhost:8888/api/identity/auth/token"

interface FormData {
    username: string
    password: string
}

const LoginCard: React.FC = () => {
    const [formData, setFormData] = useState<FormData>({
        username: "",
        password: "",
    })
    const [showPassword, setShowPassword] = useState(false)
    const [rememberMe, setRememberMe] = useState(false)
    const [isLoading, setIsLoading] = useState(false)
    const navigate = useNavigate()
    const toast = useToast()

    useEffect(() => {
        const savedCredentials = localStorage.getItem(STORAGE_KEY)
        if (savedCredentials) {
            try {
                const { username, password } = JSON.parse(savedCredentials)
                setFormData({ username, password })
                setRememberMe(true)
            } catch {
                localStorage.removeItem(STORAGE_KEY)
            }
        }
    }, [])

    const showToast = useCallback((title: string, description: string, status: "success" | "error" | "warning" | "info") => {
        toast({
            title,
            description,
            status,
            duration: 5000,
            position: "top-right",
            isClosable: true,
        })
    }, [toast])

    const handleInputChange = (e: ChangeEvent<HTMLInputElement>) => {
        const { name, value } = e.target
        setFormData(prev => ({ ...prev, [name]: value }))
    }

    const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault()

        if (!formData.username.trim() || !formData.password.trim()) {
            showToast("Validation Error", "Please enter username and password", "warning")
            return
        }

        setIsLoading(true)

        try {
            const response = await axios.post(API_LOGIN_URL, formData)

            if (response.data.code !== 1000) {
                throw new Error("Invalid credentials")
            }

            if (rememberMe) {
                localStorage.setItem(STORAGE_KEY, JSON.stringify(formData))
            } else {
                localStorage.removeItem(STORAGE_KEY)
            }

            setToken(response.data.result?.token)
            showToast("Welcome back!", "Login successful", "success")
            navigate("/")
        } catch {
            showToast("Login Failed", "Invalid username or password", "error")
        } finally {
            setIsLoading(false)
        }
    }

    const handleGoogleLogin = () => {
        const { authUri, redirectUri, clientId } = OAuthConfig
        const targetUrl = `${authUri}?redirect_uri=${encodeURIComponent(
            redirectUri
        )}&response_type=code&client_id=${clientId}&scope=openid%20email%20profile`
        window.location.href = targetUrl
    }

    const handleRememberMeChange = (e: ChangeEvent<HTMLInputElement>) => {
        const checked = e.target.checked
        setRememberMe(checked)
        if (!checked) {
            localStorage.removeItem(STORAGE_KEY)
        }
    }

    const togglePasswordVisibility = () => setShowPassword(prev => !prev)

    return (
        <div className="min-h-screen w-full flex items-center justify-center p-4">
            <div className="flex flex-col md:flex-row w-full max-w-6xl bg-white rounded-2xl shadow-2xl overflow-hidden border border-gray-100">
                <div className="hidden md:flex md:flex-1 relative overflow-hidden">
                    <img
                        src="../blur.jpg"
                        alt="Login background"
                        className="w-full h-full object-cover"
                    />
                    <div className="absolute inset-0 bg-gradient-to-br from-sky-500/20 to-blue-600/20"></div>
                </div>

                <div className="flex-1 flex flex-col">
                    <div className="text-center pt-8 md:pt-12 pb-4 px-6">
                        <h1 className="text-3xl md:text-4xl font-light text-gray-800 mb-2">
                            Login To Experience Blur
                        </h1>
                        <p className="text-sm text-gray-500 font-medium">
                            Welcome back! Please enter your details
                        </p>
                    </div>

                    <div className="flex-1 px-6 md:px-12 pb-8">
                        <form onSubmit={handleSubmit} className="max-w-md mx-auto space-y-5">
                            <div>
                                <label htmlFor="username" className="block text-sm font-medium text-gray-700 mb-2">
                                    Username <span className="text-red-500">*</span>
                                </label>
                                <input
                                    id="username"
                                    name="username"
                                    type="text"
                                    placeholder="Enter your username"
                                    value={formData.username}
                                    onChange={handleInputChange}
                                    disabled={isLoading}
                                    autoFocus
                                    className="w-full px-4 py-3 text-base border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent transition-all duration-200 disabled:bg-gray-100 disabled:cursor-not-allowed"
                                    required
                                />
                            </div>

                            <div>
                                <label htmlFor="password" className="block text-sm font-medium text-gray-700 mb-2">
                                    Password <span className="text-red-500">*</span>
                                </label>
                                <div className="relative">
                                    <input
                                        id="password"
                                        name="password"
                                        type={showPassword ? "text" : "password"}
                                        placeholder="Enter your password"
                                        value={formData.password}
                                        onChange={handleInputChange}
                                        disabled={isLoading}
                                        className="w-full px-4 py-3 pr-12 text-base border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent transition-all duration-200 disabled:bg-gray-100 disabled:cursor-not-allowed"
                                        required
                                    />
                                    <button
                                        type="button"
                                        onClick={togglePasswordVisibility}
                                        className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 hover:text-gray-700 focus:outline-none transition-colors duration-200"
                                        tabIndex={-1}
                                        aria-label={showPassword ? "Hide password" : "Show password"}
                                    >
                                        {showPassword ? (
                                            <FaEyeSlash className="w-5 h-5" />
                                        ) : (
                                            <FaEye className="w-5 h-5" />
                                        )}
                                    </button>
                                </div>
                            </div>

                            <div className="flex items-center justify-between">
                                <label className="flex items-center cursor-pointer group">
                                    <input
                                        type="checkbox"
                                        checked={rememberMe}
                                        onChange={handleRememberMeChange}
                                        disabled={isLoading}
                                        className="w-4 h-4 text-sky-500 border-gray-300 rounded focus:ring-2 focus:ring-sky-500 transition-all duration-200 cursor-pointer disabled:cursor-not-allowed"
                                    />
                                    <span className="ml-2 text-sm text-gray-600 group-hover:text-gray-800 transition-colors duration-200">
                                        Remember me
                                    </span>
                                </label>
                            </div>

                            <button
                                type="submit"
                                disabled={isLoading}
                                className="w-full bg-gradient-to-r from-sky-500 to-blue-600 hover:from-sky-600 hover:to-blue-700 text-white font-semibold py-3 px-6 rounded-lg shadow-md hover:shadow-lg transition-all duration-300 transform hover:-translate-y-0.5 disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none flex items-center justify-center"
                            >
                                {isLoading ? (
                                    <>
                                        <svg className="animate-spin -ml-1 mr-3 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                                            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                                            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                                        </svg>
                                        Logging in...
                                    </>
                                ) : (
                                    "Login"
                                )}
                            </button>

                            <div className="relative py-4">
                                <div className="absolute inset-0 flex items-center">
                                    <div className="w-full border-t border-gray-300"></div>
                                </div>
                                <div className="relative flex justify-center">
                                    <span className="bg-white px-4 text-sm text-gray-500">or</span>
                                </div>
                            </div>

                            <button
                                type="button"
                                onClick={handleGoogleLogin}
                                disabled={isLoading}
                                className="w-full bg-white border-2 border-red-500 text-red-500 hover:bg-red-50 font-semibold py-3 px-6 rounded-lg shadow-md hover:shadow-lg transition-all duration-300 transform hover:-translate-y-0.5 disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none flex items-center justify-center gap-2"
                            >
                                <FaGoogle className="w-5 h-5" />
                                Login with Google
                            </button>

                            <p className="text-sm text-gray-600 text-center pt-2">
                                Don't have an account?{" "}
                                <button
                                    type="button"
                                    onClick={() => navigate("/register")}
                                    disabled={isLoading}
                                    className="text-sky-600 hover:text-sky-700 font-semibold hover:underline transition-colors duration-200 disabled:opacity-50 disabled:cursor-not-allowed"
                                >
                                    Register now
                                </button>
                            </p>
                        </form>
                    </div>
                </div>
            </div>
        </div>
    )
}

export default LoginCard
