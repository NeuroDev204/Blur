import {
    Button,
    Card,
    CardBody,
    CardFooter,
    CardHeader,
    Heading,
    Image,
    Stack,
    useToast,
} from "@chakra-ui/react";
import React, { useEffect, useState, FormEvent, ChangeEvent } from "react";
import { FaEye, FaEyeSlash } from "react-icons/fa";
import { useNavigate } from "react-router-dom";
import axiosClient from "../../api/axiosClient";
import { introspectToken } from "../../api/authAPI";

interface UserDetails {
    noPassword?: boolean;
    [key: string]: unknown;
}

interface ApiResponse {
    code: number;
    message: string;
    result?: UserDetails;
}

const CreatePassword: React.FC = () => {
    const [password, setPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [showPassword, setShowPassword] = useState(false);
    const [userDetails, setUserDetails] = useState<UserDetails>({});
    const toast = useToast();
    const navigate = useNavigate();

    const showToast = (title: string, description: string, status: "success" | "error" | "warning" | "info" = "info") => {
        toast({
            title,
            description,
            status,
            duration: 5000,
            isClosable: true,
        });
    };

    const checkPassword = (): boolean => password === confirmPassword;

    const getUserDetails = async () => {
        try {
            const response = await axiosClient.get<{ result?: UserDetails }>('/identity/users/');
            setUserDetails(response.data?.result || {});
        } catch (error) {
        }
    };

    const addPassword = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();
        if (checkPassword()) {
            try {
                const response = await axiosClient.post<ApiResponse>(
                    '/identity/users/create-password',
                    { password }
                );

                if (response.data.code !== 1000) {
                    throw new Error(response.data.message);
                }

                getUserDetails();
                showToast("Password created", response.data.message, "success");
                navigate("/login");
            } catch (error) {
                const err = error as Error;
                showToast("Error creating password", err.message, "error");
            }
        } else {
            showToast(
                "Password mismatch",
                "Password and Confirm Password must be the same.",
                "error"
            );
        }
    };

    useEffect(() => {
        const checkAuth = async () => {
            const isValid = await introspectToken();
            if (!isValid) {
                navigate("/login");
            } else {
                getUserDetails();
            }
        };
        checkAuth();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [navigate]);

    return (
        <div className="flex justify-center items-center bg-gradient-to-r from-[#0062E6] to-[#33AEFF] h-screen">
            <form onSubmit={addPassword}>
                <div className="flex-row">
                    <Card
                        direction={{ base: "column", sm: "row" }}
                        overflow="hidden"
                        variant="outline"
                    >
                        <div className="d-none d-md-flex" style={{ flex: 1, padding: 0 }}>
                            <Image
                                className=""
                                src="../blur.jpg"
                                alt="blur"
                                style={{ objectFit: "cover", width: "100%", height: "100%" }}
                            />
                        </div>

                        <Stack style={{ flex: 1 }}>
                            <CardHeader className="text-center mb-5 fw-light fs-5">
                                <Heading>Create your password</Heading>
                            </CardHeader>
                            <CardBody className="flex flex-col items-center justify-center">
                                <div className="w-full max-w-sm">
                                    <div className="mb-3 relative">
                                        <input
                                            className="w-full border border-gray-300 rounded-lg px-4 py-2"
                                            type={showPassword ? "text" : "password"}
                                            placeholder="Password"
                                            value={password}
                                            onChange={(e: ChangeEvent<HTMLInputElement>) => setPassword(e.target.value)}
                                            required
                                        />
                                        <button
                                            type="button"
                                            className="absolute top-2 right-3"
                                            onClick={() => setShowPassword(!showPassword)}
                                        >
                                            {showPassword ? <FaEyeSlash /> : <FaEye />}
                                        </button>
                                    </div>
                                    <div className="relative">
                                        <input
                                            className="w-full border border-gray-300 rounded-lg px-4 py-2"
                                            type={showPassword ? "text" : "password"}
                                            placeholder="Confirm Password"
                                            value={confirmPassword}
                                            onChange={(e: ChangeEvent<HTMLInputElement>) => setConfirmPassword(e.target.value)}
                                        />
                                        <button
                                            type="button"
                                            className="absolute top-2 right-3"
                                            onClick={() => setShowPassword(!showPassword)}
                                        >
                                            {showPassword ? <FaEyeSlash /> : <FaEye />}
                                        </button>
                                    </div>
                                </div>
                            </CardBody>

                            <CardFooter className="flex flex-col items-center justify-center">
                                <Button
                                    type="submit"
                                    variant="solid"
                                    colorScheme="blue"
                                    className="mb-3 w-full "
                                >
                                    Create
                                </Button>
                            </CardFooter>
                        </Stack>
                    </Card>
                </div>
            </form>
        </div>
    );
};

export default CreatePassword;
