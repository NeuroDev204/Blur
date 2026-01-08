import { Input, Skeleton, SkeletonCircle } from "@chakra-ui/react";
import axios from "axios";
import React, { useState, ChangeEvent, FormEvent, KeyboardEvent } from "react";
import { IoSearchOutline, IoPersonOutline } from "react-icons/io5";
import { useNavigate } from "react-router-dom";
import { getToken } from "../../service/LocalStorageService";

interface SearchResult {
    id: string;
    firstName: string;
    lastName: string;
    username?: string;
    imageUrl?: string;
}

interface ApiResponse {
    code: number;
    message?: string;
    result: SearchResult[];
}

const SearchPage: React.FC = () => {
    const [search, setSearch] = useState("");
    const [results, setResults] = useState<SearchResult[]>([]);
    const [loading, setLoading] = useState(false);
    const navigate = useNavigate();
    const token = getToken();

    const handleSearch = async (e: FormEvent<HTMLFormElement> | KeyboardEvent<HTMLInputElement>) => {
        e.preventDefault();
        if (!search.trim()) return;

        setLoading(true);
        try {
            const response = await axios.get<ApiResponse>(
                `http://localhost:8888/api/profile/users/search/${search}`,
                {
                    headers: {
                        Authorization: `Bearer ${token}`,
                        "Content-Type": "application/json",
                    },
                }
            );

            if (response?.data.code !== 1000) {
                throw new Error(response?.data.message);
            }
            setResults(response?.data.result);

        } catch (error) {
            const err = error as Error;
            console.error(err.message);
            setResults([]);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="min-h-screen bg-gradient-to-b from-gray-50 to-white">
            <div className="flex flex-col items-center px-6 py-8 max-w-2xl mx-auto">
                {/* Header */}
                <div className="w-full mb-8 text-center">
                    <h1 className="text-3xl font-bold bg-gradient-to-r from-sky-600 to-blue-600 bg-clip-text text-transparent mb-2">
                        Search
                    </h1>
                    <p className="text-sm text-gray-500">
                        Find friends and connect with people
                    </p>
                </div>

                {/* Search Bar */}
                <form
                    onSubmit={handleSearch}
                    className="w-full max-w-xl mb-8"
                >
                    <div className="relative group">
                        <div className="absolute inset-y-0 left-0 flex items-center pl-4 pointer-events-none">
                            <IoSearchOutline className="text-xl text-gray-400 group-focus-within:text-sky-500 transition-colors" />
                        </div>
                        <Input
                            placeholder="Search for people..."
                            onChange={(e: ChangeEvent<HTMLInputElement>) => setSearch(e.target.value)}
                            value={search}
                            onKeyDown={(e: KeyboardEvent<HTMLInputElement>) => e.key === "Enter" && handleSearch(e)}
                            className="w-full pl-12 pr-4 py-6 text-sm"
                            size="lg"
                            borderRadius="xl"
                            borderColor="gray.200"
                            _hover={{ borderColor: "sky.300" }}
                            _focus={{
                                borderColor: "sky.400",
                                boxShadow: "0 0 0 3px rgba(14, 165, 233, 0.1)",
                                outline: "none"
                            }}
                            bg="white"
                            shadow="sm"
                        />
                        {search && (
                            <button
                                type="button"
                                onClick={() => setSearch("")}
                                className="absolute inset-y-0 right-0 flex items-center pr-4 text-gray-400 hover:text-gray-600"
                            >
                                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                                </svg>
                            </button>
                        )}
                    </div>
                </form>

                {/* Results Container */}
                <div className="w-full max-w-xl">
                    {loading ? (
                        // Loading Skeletons
                        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
                            {[...Array(5)].map((_, index) => (
                                <div key={index} className="flex items-center p-4 gap-3 border-b border-gray-50 last:border-b-0">
                                    <SkeletonCircle size="12" startColor="sky.100" endColor="blue.100" />
                                    <div className="flex-1 space-y-2">
                                        <Skeleton height="16px" width="60%" startColor="gray.100" endColor="gray.200" borderRadius="lg" />
                                        <Skeleton height="12px" width="40%" startColor="gray.50" endColor="gray.100" borderRadius="lg" />
                                    </div>
                                </div>
                            ))}
                        </div>
                    ) : results.length > 0 ? (
                        // Results List
                        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
                            {results.map((item) => (
                                <div
                                    key={item.id}
                                    className="group p-4 border-b border-gray-50 last:border-b-0 cursor-pointer hover:bg-sky-50/50 transition-all duration-200"
                                    onClick={() => navigate(`/profile/user/?profileId=${item.id}`)}
                                >
                                    <div className="flex items-center gap-3">
                                        {/* Avatar with gradient ring */}
                                        <div className="relative flex-shrink-0">
                                            <div className="absolute inset-0 bg-gradient-to-br from-sky-400 to-blue-500 rounded-full blur-sm opacity-0 group-hover:opacity-30 transition-opacity"></div>
                                            <img
                                                src={
                                                    item.imageUrl ||
                                                    "https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_640.png"
                                                }
                                                alt={item.firstName}
                                                className="relative w-12 h-12 rounded-full object-cover border-2 border-white shadow-sm group-hover:scale-105 transition-transform"
                                            />
                                        </div>

                                        {/* User Info */}
                                        <div className="flex-1 min-w-0">
                                            <p className="font-semibold text-gray-800 group-hover:text-sky-600 transition-colors truncate">
                                                {item.firstName} {item.lastName}
                                            </p>
                                            <p className="text-sm text-gray-500 truncate">
                                                @{item.username || 'user'}
                                            </p>
                                        </div>

                                        {/* Arrow Icon */}
                                        <div className="flex-shrink-0 text-gray-400 group-hover:text-sky-500 group-hover:translate-x-1 transition-all">
                                            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                                            </svg>
                                        </div>
                                    </div>
                                </div>
                            ))}
                        </div>
                    ) : search && (
                        // No Results Found
                        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-12 text-center">
                            <div className="w-20 h-20 mx-auto mb-6 rounded-full bg-gradient-to-br from-sky-100 to-blue-100 flex items-center justify-center">
                                <IoPersonOutline className="w-10 h-10 text-sky-400" />
                            </div>
                            <h3 className="text-lg font-semibold text-gray-800 mb-2">
                                No results found
                            </h3>
                            <p className="text-sm text-gray-500">
                                Try searching with different keywords
                            </p>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};

export default SearchPage;
