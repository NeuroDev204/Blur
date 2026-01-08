import { useToast } from "@chakra-ui/react";
import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import "../../service/LocalStorageService";
import { setToken } from "../../service/LocalStorageService";

interface UserDetails {
    noPassword?: boolean;
    [key: string]: unknown;
}

interface AuthResponse {
    result?: {
        token?: string;
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
                }
            )
                .then((response) => {
                    return response.json() as Promise<AuthResponse>;
                })
                .then((data) => {
                    console.log("data: ", data);
                    if (data.result?.token) {
                        setToken(data.result.token);
                    }
                });
        }
    }, []);

    const getUserDetails = async (accessToken: string) => {
        try {
            const response = await fetch(
                "http://localhost:8888/api/identity/users/",
                {
                    method: "GET",
                    headers: {
                        Authorization: `Bearer ${accessToken}`,
                    },
                }
            );
            const data: UserResponse = await response.json();
            setUserDetails(data.result || {});
            setIsLoggedIn(true);
        } catch (error) {
            const err = error as Error;
            showToast("Error fetching user details", err.message, "error");
        }
    };

    useEffect(() => {
        const accessToken = localStorage.getItem("token");

        if (accessToken) {
            // Lấy thông tin người dùng
            getUserDetails(accessToken);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [navigate]);

    // Lắng nghe sự thay đổi của userDetails
    useEffect(() => {
        console.log("User details: ", userDetails); // Debug giá trị userDetails
        if (userDetails.noPassword === true && !isLoggedIn) {
            navigate("http://localhost:8888/api/identity/create-password");
        } else if (userDetails.noPassword === false && isLoggedIn) {
            navigate("/");
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [userDetails, navigate]); // Thêm userDetails vào mảng phụ thuộc

    return <>{!isLoggedIn && <div>Login</div>}</>;
};

export default Authenticate;
