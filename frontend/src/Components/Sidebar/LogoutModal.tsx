import {
  Button,
  Modal,
  ModalBody,
  ModalCloseButton,
  ModalContent,
  ModalFooter,
  ModalHeader,
  ModalOverlay,
  Text,
  Box,
} from "@chakra-ui/react";
import React from "react";
import { MdLogout } from "react-icons/md";

interface LogoutModalProps {
  isOpen: boolean;
  onClose: () => void;
}

const LogoutModal: React.FC<LogoutModalProps> = ({ isOpen, onClose }) => {
  return (
    <>
      <Modal isCentered isOpen={isOpen} onClose={onClose} size="md">
        <ModalOverlay
          bg="blackAlpha.400"
          backdropFilter="blur(8px)"
        />
        <ModalContent borderRadius="2xl" overflow="hidden" shadow="2xl">
          <ModalHeader
            textAlign="center"
            pt={8}
            pb={4}
            bgGradient="linear(to-r, sky.50, blue.50)"
            borderBottom="1px"
            borderColor="gray.100"
          >
            <Box display="flex" flexDirection="column" alignItems="center" gap={3}>
              <Box
                w="16"
                h="16"
                bg="gradient-to-br from-red-100 to-orange-100"
                borderRadius="full"
                display="flex"
                alignItems="center"
                justifyContent="center"
                mb={2}
              >
                <MdLogout size={32} className="text-red-500" />
              </Box>
              <Text
                fontSize="xl"
                fontWeight="bold"
                bgGradient="linear(to-r, red.500, orange.500)"
                bgClip="text"
              >
                Logged Out
              </Text>
            </Box>
          </ModalHeader>

          <ModalCloseButton
            top={4}
            right={4}
            rounded="full"
            _hover={{ bg: "gray.100" }}
          />

          <ModalBody textAlign="center" py={8} px={6}>
            <Text fontSize="md" color="gray.600" lineHeight="tall">
              You have been logged out successfully. Please sign in to continue using the app.
            </Text>

            <Box
              mt={4}
              p={3}
              bg="sky.50"
              borderRadius="lg"
              border="1px"
              borderColor="sky.100"
            >
              <Text fontSize="sm" color="gray.600">
                💡 <strong>Tip:</strong> Remember to log out from shared devices for security.
              </Text>
            </Box>
          </ModalBody>

          <ModalFooter
            justifyContent="center"
            gap={3}
            pb={6}
            bg="gray.50"
            borderTop="1px"
            borderColor="gray.100"
          >
            <Button
              onClick={onClose}
              bgGradient="linear(to-r, sky.400, blue.500)"
              color="white"
              rounded="xl"
              px={8}
              fontWeight="semibold"
              _hover={{
                bgGradient: "linear(to-r, sky.500, blue.600)",
                transform: "translateY(-2px)",
                shadow: "lg"
              }}
              _active={{
                transform: "scale(0.98)"
              }}
              transition="all 0.2s"
            >
              Got it
            </Button>
          </ModalFooter>
        </ModalContent>
      </Modal>
    </>
  );
};

export default LogoutModal;