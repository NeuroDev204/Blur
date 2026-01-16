import { useToast } from "@chakra-ui/react";
import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { setToken, setUserId } from "../../service/LocalStorageService";

interface UserDetails {
    id?: string;
    noPassword?: boolean;
    [key: string]: unknown;
}

interface AuthResponse {
    code?: number;
    result?: {
        userId?: string;
        authenticated?: boolean;
    };
}

interface UserResponse {
    result?: UserDetails;
}

const Authenticate: React.FC = () => {
    const [userDetails, setUserDetails] = useState<UserDetails>({});
    const navigate = useNavigate();
    const [isLoggedIn, setIsLoggedIn] = useState(false);
    const toast = useToast();

    const showToast = (title: string, description: string, status: "success" | "error" | "warning" | "info" = "info") => {
        toast({
            title,
            description,
            status,
            duration: 5000,
            isClosable: true,
        });
    };

    useEffect(() => {
        console.log("windows href: ", window.location.href);
        const authCodeRegex = /code=([^&]+)/;
        const isMatch = window.location.href.match(authCodeRegex);
        if (isMatch) {
            const authCode = isMatch[1];
            fetch(
                `/api/identity/auth/outbound/authentication?code=${authCode}`,
                {
                    method: "POST",
                    credentials: "include", // Cookie tự động được set
                }
            )
                .then((response) => {
                    return response.json() as Promise<AuthResponse>;
                })
                .then((data) => {
                    console.log("data: ", data);
                    if (data.code === 1000 && data.result?.authenticated) {
                        // Lưu flag authenticated (token đã được set trong cookie)
                        setToken("authenticated");

                        // Lưu userId từ response nếu có
                        if (data.result.userId) {
                            setUserId(data.result.userId);
                        }

                        // Lấy thông tin user để kiểm tra noPassword
                        getUserDetails();
                    }
                });
        }
    }, []);

    const getUserDetails = async () => {
        try {
            const response = await fetch(
                "http://localhost:8888/api/identity/users/me",
                {
                    method: "GET",
                    credentials: "include", // Cookie tự động được gửi
                }
            );
            const data: UserResponse = await response.json();
            if (data.result) {
                setUserDetails(data.result);
                // Lưu userId để dùng cho socket
                if (data.result.id) {
                    setUserId(data.result.id);
                }
            }
            setIsLoggedIn(true);
        } catch (error) {
            const err = error as Error;
            showToast("Error fetching user details", err.message, "error");
        }
    };

    useEffect(() => {
        // Kiểm tra xem đã có auth flag chưa
        const isAuth = localStorage.getItem("token") === "authenticated";

        if (isAuth) {
            // Lấy thông tin người dùng
            getUserDetails();
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [navigate]);

    // Lắng nghe sự thay đổi của userDetails
    useEffect(() => {
        console.log("User details: ", userDetails); // Debug giá trị userDetails
        if (userDetails.noPassword === true && !isLoggedIn) {
            navigate("/create-password");
        } else if (userDetails.noPassword === false && isLoggedIn) {
            navigate("/");
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [userDetails, navigate]); // Thêm userDetails vào mảng phụ thuộc

    return <>{!isLoggedIn && <div>Login</div>}</>;
};

export default Authenticate;
