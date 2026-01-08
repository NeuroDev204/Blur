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
import { getToken } from "../../service/LocalStorageService";

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

    const getUserDetails = async (accessToken: string) => {
        const response = await fetch("/api/identity/users/", {
            method: "GET",
            headers: {
                Authorization: `Bearer ${accessToken}`,
            },
        });
        const data: { result?: UserDetails } = await response.json();
        setUserDetails(data.result || {});
    };

    const addPassword = (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();
        if (checkPassword()) {
            const body = { password };
            fetch("http://localhost:8888/api/identity/users/create-password", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    Authorization: `Bearer ${getToken()}`,
                },
                body: JSON.stringify(body),
            })
                .then((response) => {
                    return response.json() as Promise<ApiResponse>;
                })
                .then((data) => {
                    if (data.code !== 1000) throw new Error(data.message);
                    const token = getToken();
                    if (token) {
                        getUserDetails(token);
                    }
                    showToast("Password created", data.message, "success");
                    navigate("/login");
                })
                .catch((error: Error) => {
                    showToast("Error creating password", error.message, "error");
                });
        } else {
            showToast(
                "Password mismatch",
                "Password and Confirm Password must be the same.",
                "error"
            );
        }
    };

    useEffect(() => {
        const accessToken = getToken();
        if (!accessToken) {
            navigate("/login");
        } else {
            getUserDetails(accessToken);
        }
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
