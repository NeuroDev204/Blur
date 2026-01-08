import React from 'react';
import {
  Home,
  Search,
  MessageCircle,
  Bell,
  PlusCircle,
  User,
} from "lucide-react";

export interface MenuItem {
  title: string;
  icon: React.ReactNode;
  activeIcon: React.ReactNode;
  gradient: string;
  bgColor: string;
  hoverBg: string;
}

export const menuItems: MenuItem[] = [
  {
    title: "Home",
    icon: <Home className="text-gray-500" strokeWidth={1.5} />,
    activeIcon: <Home className="text-blue-500" strokeWidth={2} fill="rgba(59, 130, 246, 0.1)" />,
    gradient: "from-blue-400 to-blue-600",
    bgColor: "bg-blue-50",
    hoverBg: "hover:bg-blue-50/50",
  },
  {
    title: "Search",
    icon: <Search className="text-gray-500" strokeWidth={1.5} />,
    activeIcon: <Search className="text-indigo-500" strokeWidth={2} fill="rgba(99, 102, 241, 0.1)" />,
    gradient: "from-indigo-400 to-indigo-600",
    bgColor: "bg-indigo-50",
    hoverBg: "hover:bg-indigo-50/50",
  },
  {
    title: "Message",
    icon: <MessageCircle className="text-gray-500" strokeWidth={1.5} />,
    activeIcon: <MessageCircle className="text-emerald-500" strokeWidth={2} fill="rgba(16, 185, 129, 0.1)" />,
    gradient: "from-emerald-400 to-emerald-600",
    bgColor: "bg-emerald-50",
    hoverBg: "hover:bg-emerald-50/50",
  },
  {
    title: "Notification",
    icon: <Bell className="text-gray-500" strokeWidth={1.5} />,
    activeIcon: <Bell className="text-rose-500" strokeWidth={2} fill="rgba(244, 63, 94, 0.1)" />,
    gradient: "from-rose-400 to-rose-600",
    bgColor: "bg-rose-50",
    hoverBg: "hover:bg-rose-50/50",
  },
  {
    title: "Create",
    icon: <PlusCircle className="text-gray-500" strokeWidth={1.5} />,
    activeIcon: <PlusCircle className="text-violet-500" strokeWidth={2} fill="rgba(139, 92, 246, 0.1)" />,
    gradient: "from-violet-400 to-violet-600",
    bgColor: "bg-violet-50",
    hoverBg: "hover:bg-violet-50/50",
  },
  {
    title: "Profile",
    icon: <User className="text-gray-500" strokeWidth={1.5} />,
    activeIcon: <User className="text-amber-500" strokeWidth={2} fill="rgba(245, 158, 11, 0.1)" />,
    gradient: "from-amber-400 to-amber-600",
    bgColor: "bg-amber-50",
    hoverBg: "hover:bg-amber-50/50",
  },
];