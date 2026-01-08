import React, { useState, useEffect } from 'react';
import { WifiOff, X, AlertCircle } from 'lucide-react';

const ConnectionStatus = ({ error }) => {
  const [isVisible, setIsVisible] = useState(false);
  const [isDismissed, setIsDismissed] = useState(false);

  useEffect(() => {
    if (error && !isDismissed) {
      setIsVisible(true);
    } else {
      setIsVisible(false);
    }
  }, [error, isDismissed]);

  const handleDismiss = () => {
    setIsDismissed(true);
    setIsVisible(false);
    setTimeout(() => setIsDismissed(false), 5000);
  };

  if (!isVisible) return null;

  return (
    <div className="fixed top-4 left-1/2 -translate-x-1/2 z-[10000] animate-slide-down">
      <div className="bg-white rounded-2xl shadow-2xl border border-gray-200 px-4 py-3 min-w-[320px] max-w-md">
        <div className="flex items-center gap-3">
          {/* Icon */}
          <div className="flex-shrink-0 w-10 h-10 bg-red-100 rounded-full flex items-center justify-center">
            <WifiOff size={20} className="text-red-600" />
          </div>

          {/* Content */}
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2 mb-0.5">
              <AlertCircle size={14} className="text-red-600 flex-shrink-0" />
              <h4 className="font-semibold text-gray-900 text-sm">
                Lỗi kết nối
              </h4>
            </div>
            <p className="text-xs text-gray-600 line-clamp-2">
              {error}
            </p>
          </div>

          {/* Close Button */}
          <button
            onClick={handleDismiss}
            className="flex-shrink-0 text-gray-400 hover:text-gray-600 transition-colors p-1"
          >
            <X size={18} />
          </button>
        </div>

        {/* Progress Bar */}
        <div className="mt-3 h-1 bg-gray-100 rounded-full overflow-hidden">
          <div className="h-full bg-gradient-to-r from-red-500 to-pink-500 animate-progress"></div>
        </div>
      </div>

      <style>{`
        @keyframes slideDown {
          from {
            transform: translate(-50%, -100%);
            opacity: 0;
          }
          to {
            transform: translate(-50%, 0);
            opacity: 1;
          }
        }

        @keyframes progress {
          from {
            width: 0%;
          }
          to {
            width: 100%;
          }
        }

        .animate-slide-down {
          animation: slideDown 0.3s ease-out;
        }

        .animate-progress {
          animation: progress 5s linear;
        }
      `}</style>
    </div>
  );
};

export default ConnectionStatus;
