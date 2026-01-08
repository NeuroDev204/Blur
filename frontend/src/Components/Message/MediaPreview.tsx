import React, { useState, useEffect } from 'react';
import { X, Film, FileText } from 'lucide-react';

const MediaPreview = ({ file, onRemove }) => {
  const [preview, setPreview] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (file.type.startsWith('image/')) {
      const reader = new FileReader();
      reader.onload = (e) => {
        setPreview(e.target.result);
        setLoading(false);
      };
      reader.readAsDataURL(file);
    } else {
      setLoading(false);
    }
    
    return () => {
      if (preview && preview.startsWith('blob:')) {
        URL.revokeObjectURL(preview);
      }
    };
  }, [file]);

  const formatFileSize = (bytes) => {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / 1048576).toFixed(1) + ' MB';
  };

  return (
    <div className="relative group">
      <div className="relative w-20 h-20 rounded-xl overflow-hidden bg-gray-100 border border-gray-200">
        {/* Loading State */}
        {loading && (
          <div className="absolute inset-0 flex items-center justify-center">
            <div className="w-6 h-6 border-2 border-blue-500 border-t-transparent rounded-full animate-spin"></div>
          </div>
        )}

        {/* Image Preview */}
        {file.type.startsWith('image/') && preview && (
          <img 
            src={preview} 
            alt="Preview" 
            className="w-full h-full object-cover"
          />
        )}

        {/* Video Preview */}
        {file.type.startsWith('video/') && (
          <div className="w-full h-full bg-gradient-to-br from-purple-500 to-pink-500 flex items-center justify-center">
            <Film size={32} className="text-white opacity-80" />
          </div>
        )}

        {/* Other File Types */}
        {!file.type.startsWith('image/') && !file.type.startsWith('video/') && (
          <div className="w-full h-full bg-gradient-to-br from-blue-500 to-cyan-500 flex items-center justify-center">
            <FileText size={32} className="text-white opacity-80" />
          </div>
        )}

        {/* Overlay on Hover */}
        <div className="absolute inset-0 bg-black bg-opacity-0 group-hover:bg-opacity-20 transition-all duration-200"></div>

        {/* Remove Button */}
        <button
          onClick={onRemove}
          className="absolute -top-2 -right-2 w-6 h-6 bg-gray-900 bg-opacity-80 hover:bg-opacity-100 text-white rounded-full flex items-center justify-center transition-all duration-200 shadow-lg opacity-0 group-hover:opacity-100"
          title="Xóa"
        >
          <X size={14} strokeWidth={3} />
        </button>

        {/* File Size Badge */}
        <div className="absolute bottom-1 right-1 bg-black bg-opacity-60 text-white text-[10px] px-1.5 py-0.5 rounded">
          {formatFileSize(file.size)}
        </div>
      </div>

      {/* File Name */}
      <div className="mt-1 text-[10px] text-gray-600 text-center truncate w-20" title={file.name}>
        {file.name}
      </div>
    </div>
  );
};

export default MediaPreview;