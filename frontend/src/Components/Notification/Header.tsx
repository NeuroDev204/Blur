// ============= Header.tsx =============
import { CheckCircle2, Search } from 'lucide-react';
import React, { ChangeEvent } from 'react';

interface HeaderProps {
    unreadCount: number;
    onMarkAllRead: () => void;
    searchTerm: string;
    setSearchTerm: (term: string) => void;
}

const Header: React.FC<HeaderProps> = ({ unreadCount, onMarkAllRead, searchTerm, setSearchTerm }) => {
    return (
        <div className="bg-white border-b border-gray-100 sticky top-0 z-10 shadow-sm">
            {/* Top section */}
            <div className="px-6 py-5">
                <div className="flex items-center justify-between">
                    <div>
                        <h1 className="text-2xl font-bold bg-gradient-to-r from-sky-600 to-blue-600 bg-clip-text text-transparent">
                            Notifications
                        </h1>
                        {unreadCount > 0 && (
                            <p className="text-sm text-gray-500 mt-1">
                                You have {unreadCount} unread {unreadCount === 1 ? 'notification' : 'notifications'}
                            </p>
                        )}
                    </div>

                    {unreadCount > 0 && (
                        <button
                            onClick={onMarkAllRead}
                            className="flex items-center gap-2 bg-gradient-to-r from-sky-400 to-blue-500 hover:from-sky-500 hover:to-blue-600 text-white px-5 py-2.5 rounded-xl text-sm font-semibold transition-all shadow-md hover:shadow-lg hover:-translate-y-0.5 active:scale-95"
                        >
                            <CheckCircle2 size={18} />
                            <span>Mark all read</span>
                        </button>
                    )}
                </div>
            </div>

            {/* Search section */}
            <div className="px-6 pb-4">
                <div className="relative">
                    <Search
                        size={18}
                        className="absolute left-4 top-1/2 transform -translate-y-1/2 text-gray-400"
                    />
                    <input
                        type="text"
                        placeholder="Search notifications..."
                        value={searchTerm}
                        onChange={(e: ChangeEvent<HTMLInputElement>) => setSearchTerm(e.target.value)}
                        className="w-full pl-11 pr-4 py-3 bg-gray-50 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-sky-400 focus:border-transparent focus:bg-white transition-all placeholder-gray-400"
                    />
                </div>
            </div>
        </div>
    );
};

export default Header;
