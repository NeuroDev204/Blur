import React from 'react';
import { FileText } from 'lucide-react';

interface Attachment {
  url?: string;
  fileName?: string;
  fileType?: string;
  [key: string]: unknown;
}

interface MessageAttachmentProps {
  attachment: Attachment;
  isMe?: boolean;
}

const MessageAttachment: React.FC<MessageAttachmentProps> = ({ attachment, isMe }) => {
  if (attachment.fileType?.startsWith('image/')) {
    return (
      <img
        src={attachment.url}
        alt={attachment.fileName}
        className="max-w-sm max-h-96 rounded-lg cursor-pointer hover:opacity-90 transition-opacity"
        onClick={() => window.open(attachment.url, '_blank')}
      />
    );
  }

  if (attachment.fileType?.startsWith('video/')) {
    return (
      <video
        src={attachment.url}
        controls
        className="max-w-sm max-h-96 rounded-lg"
      />
    );
  }

  return (
    <a
      href={attachment.url}
      target="_blank"
      rel="noopener noreferrer"
      className="flex items-center gap-2 p-3 bg-gray-100 rounded-lg hover:bg-gray-200 transition-colors"
    >
      <FileText size={24} />
      <span className="text-sm">{attachment.fileName}</span>
    </a>
  );
};

export default MessageAttachment;