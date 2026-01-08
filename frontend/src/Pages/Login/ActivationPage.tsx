import {
    Button,
    Card,
    CardBody,
    CardFooter,
    CardHeader,
    Heading,
    Image,
    Text,
} from "@chakra-ui/react";
import React from "react";

const ActivationPage: React.FC = () => {
    return (
        <div className="flex justify-center items-center h-screen bg-blue-400">
            <Card align="center">
                <CardHeader>
                    <Heading className="text-blue-600" size="md">
                        {" "}
                        Welcome to Blur Social Network
                    </Heading>
                    <hr />
                </CardHeader>

                <CardBody>
                    <div className="flex flex-col items-center justify-center">
                        <Image
                            className="rounded-md w-40 h-40 mb-3 cover"
                            src="../admin.jpg"
                            alt="profile"
                        />
                        <Text>I'm <b>Sy</b> Administrator of Blur </Text>
                        <Text className="text-red-400">Thank you for joining us!</Text>

                    </div>
                </CardBody>
                <CardFooter>
                    <Button colorScheme="blue">Activate Account</Button>
                </CardFooter>
            </Card>
        </div>
    );
};

export default ActivationPage;
