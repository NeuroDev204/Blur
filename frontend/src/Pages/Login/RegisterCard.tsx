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
    InputGroup,
    InputRightElement,
    IconButton,
    FormControl,
    FormLabel,
    Input,
    Text,
    Box,
    Divider,
    VStack,
    HStack,
} from "@chakra-ui/react";
import React, { useState, useCallback, ChangeEvent, FormEvent } from "react";
import { FaGoogle, FaEye, FaEyeSlash } from "react-icons/fa";
import { useNavigate } from "react-router-dom";
import { OAuthConfig } from "../../Config/configuration";
import axios, { AxiosError } from "axios";

const API_REGISTER_URL = "http://localhost:8888/api/identity/users/registration";

interface FormData {
    username: string;
    email: string;
    password: string;
    confirmPassword: string;
    firstName: string;
    lastName: string;
    dob: string;
}

const RegisterCard: React.FC = () => {
    const [formData, setFormData] = useState<FormData>({
        username: "",
        email: "",
        password: "",
        confirmPassword: "",
        firstName: "",
        lastName: "",
        dob: "",
    });
    const [showPassword, setShowPassword] = useState(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState(false);
    const [isLoading, setIsLoading] = useState(false);
    const navigate = useNavigate();
    const toast = useToast();

    const showToast = useCallback((title: string, description: string, status: "success" | "error" | "warning" | "info") => {
        toast({
            title,
            description,
            status,
            duration: 5000,
            position: "top-right",
            isClosable: true,
        });
    }, [toast]);

    const handleInputChange = (e: ChangeEvent<HTMLInputElement>) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
    };

    const validateForm = (): boolean => {
        const { username, email, password, confirmPassword, firstName, lastName, dob } = formData;

        if (!username.trim() || !email.trim() || !password.trim() || !firstName.trim() || !lastName.trim() || !dob) {
            showToast("Validation Error", "Please fill in all required fields", "warning");
            return false;
        }

        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(email)) {
            showToast("Validation Error", "Please enter a valid email address", "warning");
            return false;
        }

        if (password.length < 6) {
            showToast("Validation Error", "Password must be at least 6 characters", "warning");
            return false;
        }

        if (password !== confirmPassword) {
            showToast("Validation Error", "Passwords do not match", "warning");
            return false;
        }

        return true;
    };

    const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();

        if (!validateForm()) return;

        setIsLoading(true);

        try {
            const { confirmPassword: _, ...registrationData } = formData;
            const response = await axios.post(API_REGISTER_URL, registrationData);

            if (response.data.code !== 1000) {
                throw new Error(response.data.message || "Registration failed");
            }

            showToast("Success!", "Registration successful. Please login.", "success");
            navigate("/login");
        } catch (error) {
            const axiosError = error as AxiosError<{ message?: string }>;
            const errorMessage = axiosError.response?.data?.message || axiosError.message || "Registration failed. Please try again.";
            showToast("Registration Failed", errorMessage, "error");
        } finally {
            setIsLoading(false);
        }
    };

    const handleGoogleLogin = () => {
        const { authUri, redirectUri, clientId } = OAuthConfig;
        const targetUrl = `${authUri}?redirect_uri=${encodeURIComponent(
            redirectUri
        )}&response_type=code&client_id=${clientId}&scope=openid%20email%20profile`;
        window.location.href = targetUrl;
    };

    const togglePasswordVisibility = () => setShowPassword(prev => !prev);
    const toggleConfirmPasswordVisibility = () => setShowConfirmPassword(prev => !prev);

    return (
        <Box className="flex-row" minH="100vh" display="flex" alignItems="center" justifyContent="center">
            <Card
                direction={{ base: "column", sm: "row" }}
                overflow="hidden"
                variant="outline"
                maxW="1200px"
                w="full"
                boxShadow="xl"
                borderRadius="lg"
            >
                {/* Image Section - Hidden on mobile */}
                <Box
                    display={{ base: "none", md: "flex" }}
                    flex={1}
                    position="relative"
                    overflow="hidden"
                >
                    <Image
                        src="../blur.jpg"
                        alt="Register background"
                        objectFit="cover"
                        w="full"
                        h="full"
                    />
                </Box>

                {/* Form Section */}
                <Stack flex={1} spacing={0}>
                    <CardHeader textAlign="center" pt={6} pb={2}>
                        <Heading size="md" fontWeight="light">
                            Sign Up To Experience Blur
                        </Heading>
                        <Text fontSize="xs" color="gray.500" mt={1}>
                            Create your account to get started
                        </Text>
                    </CardHeader>

                    <CardBody py={3}>
                        <form onSubmit={handleSubmit}>
                            <VStack spacing={2} maxW="400px" mx="auto">
                                <FormControl isRequired>
                                    <FormLabel fontSize="xs" fontWeight="medium" mb={1}>Username</FormLabel>
                                    <Input
                                        name="username"
                                        type="text"
                                        placeholder="Enter your username"
                                        value={formData.username}
                                        onChange={handleInputChange}
                                        size="md"
                                        autoFocus
                                        disabled={isLoading}
                                    />
                                </FormControl>

                                <FormControl isRequired>
                                    <FormLabel fontSize="xs" fontWeight="medium" mb={1}>Email</FormLabel>
                                    <Input
                                        name="email"
                                        type="email"
                                        placeholder="Enter your email"
                                        value={formData.email}
                                        onChange={handleInputChange}
                                        size="md"
                                        disabled={isLoading}
                                    />
                                </FormControl>

                                <FormControl isRequired>
                                    <FormLabel fontSize="xs" fontWeight="medium" mb={1}>Password</FormLabel>
                                    <InputGroup size="md">
                                        <Input
                                            name="password"
                                            type={showPassword ? "text" : "password"}
                                            placeholder="Enter your password"
                                            value={formData.password}
                                            onChange={handleInputChange}
                                            disabled={isLoading}
                                        />
                                        <InputRightElement>
                                            <IconButton
                                                aria-label={showPassword ? "Hide password" : "Show password"}
                                                icon={showPassword ? <FaEyeSlash /> : <FaEye />}
                                                onClick={togglePasswordVisibility}
                                                variant="ghost"
                                                size="sm"
                                                tabIndex={-1}
                                            />
                                        </InputRightElement>
                                    </InputGroup>
                                </FormControl>

                                <FormControl isRequired>
                                    <FormLabel fontSize="xs" fontWeight="medium" mb={1}>Confirm Password</FormLabel>
                                    <InputGroup size="md">
                                        <Input
                                            name="confirmPassword"
                                            type={showConfirmPassword ? "text" : "password"}
                                            placeholder="Confirm your password"
                                            value={formData.confirmPassword}
                                            onChange={handleInputChange}
                                            disabled={isLoading}
                                        />
                                        <InputRightElement>
                                            <IconButton
                                                aria-label={showConfirmPassword ? "Hide password" : "Show password"}
                                                icon={showConfirmPassword ? <FaEyeSlash /> : <FaEye />}
                                                onClick={toggleConfirmPasswordVisibility}
                                                variant="ghost"
                                                size="sm"
                                                tabIndex={-1}
                                            />
                                        </InputRightElement>
                                    </InputGroup>
                                </FormControl>

                                <HStack spacing={3} w="full">
                                    <FormControl isRequired flex={1}>
                                        <FormLabel fontSize="xs" fontWeight="medium" mb={1}>First Name</FormLabel>
                                        <Input
                                            name="firstName"
                                            type="text"
                                            placeholder="First name"
                                            value={formData.firstName}
                                            onChange={handleInputChange}
                                            size="md"
                                            disabled={isLoading}
                                        />
                                    </FormControl>

                                    <FormControl isRequired flex={1}>
                                        <FormLabel fontSize="xs" fontWeight="medium" mb={1}>Last Name</FormLabel>
                                        <Input
                                            name="lastName"
                                            type="text"
                                            placeholder="Last name"
                                            value={formData.lastName}
                                            onChange={handleInputChange}
                                            size="md"
                                            disabled={isLoading}
                                        />
                                    </FormControl>
                                </HStack>

                                <FormControl isRequired>
                                    <FormLabel fontSize="xs" fontWeight="medium" mb={1}>Date of Birth</FormLabel>
                                    <Input
                                        name="dob"
                                        type="date"
                                        value={formData.dob}
                                        onChange={handleInputChange}
                                        size="md"
                                        disabled={isLoading}
                                    />
                                </FormControl>

                                <Button
                                    type="submit"
                                    colorScheme="blue"
                                    size="md"
                                    w="full"
                                    mt={2}
                                    isLoading={isLoading}
                                    loadingText="Creating account..."
                                >
                                    Register
                                </Button>

                                <Box w="full" position="relative" py={1}>
                                    <Divider />
                                    <Text
                                        position="absolute"
                                        top="50%"
                                        left="50%"
                                        transform="translate(-50%, -50%)"
                                        bg="white"
                                        px={3}
                                        fontSize="xs"
                                        color="gray.500"
                                    >
                                        or
                                    </Text>
                                </Box>

                                <Button
                                    onClick={handleGoogleLogin}
                                    variant="outline"
                                    colorScheme="red"
                                    size="md"
                                    w="full"
                                    leftIcon={<FaGoogle />}
                                    disabled={isLoading}
                                >
                                    Sign up with Google
                                </Button>

                                <Text fontSize="xs" color="gray.600" textAlign="center" pt={1}>
                                    Already have an account?{" "}
                                    <Button
                                        variant="link"
                                        colorScheme="blue"
                                        fontSize="xs"
                                        onClick={() => navigate("/login")}
                                        disabled={isLoading}
                                    >
                                        Login now
                                    </Button>
                                </Text>
                            </VStack>
                        </form>
                    </CardBody>

                    <CardFooter />
                </Stack>
            </Card>
        </Box>
    );
};

export default RegisterCard;
