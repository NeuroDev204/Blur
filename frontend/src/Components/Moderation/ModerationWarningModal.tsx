import React from "react";
import {
  Modal,
  ModalOverlay,
  ModalContent,
  ModalBody,
  ModalFooter,
  Button,
} from "@chakra-ui/react";

interface ModerationWarningModalProps {
  isOpen: boolean;
  onClose: () => void;
}

const ModerationWarningModal: React.FC<ModerationWarningModalProps> = ({
  isOpen,
  onClose,
}) => {
  return (
    <Modal isOpen={isOpen} onClose={onClose} isCentered size="md">
      <ModalOverlay bg="blackAlpha.600" backdropFilter="blur(4px)" />
      <ModalContent borderRadius="2xl" overflow="hidden" mx={4}>
        <ModalBody pt={8} pb={4} px={8} textAlign="center">
          <div className="flex justify-center mb-4">
            <div className="w-16 h-16 rounded-full bg-red-100 flex items-center justify-center">
              <svg
                className="w-8 h-8 text-red-500"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth="2"
                  d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
                />
              </svg>
            </div>
          </div>
          <h2 className="text-xl font-bold text-gray-900 mb-2">
            Bình luận đã bị ẩn
          </h2>
          <p className="text-gray-600 text-sm leading-relaxed">
            Bình luận của bạn đã bị ẩn vì vi phạm tiêu chuẩn cộng đồng. Vui
            lòng tuân thủ quy tắc ứng xử để giữ cho cộng đồng an toàn và thân
            thiện.
          </p>
        </ModalBody>
        <ModalFooter justifyContent="center" pb={8}>
          <Button
            onClick={onClose}
            colorScheme="blue"
            borderRadius="xl"
            px={8}
            size="lg"
          >
            Tôi đã hiểu
          </Button>
        </ModalFooter>
      </ModalContent>
    </Modal>
  );
};

export default ModerationWarningModal;
