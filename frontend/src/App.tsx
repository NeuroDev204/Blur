import React from "react"
import Router from "./Pages/Router/Router"
import { SocketProvider } from "./contexts/SocketContext"
import { NotificationProvider } from "./contexts/NotificationContext"
import { NotificationSocketProvider } from "./contexts/NotificationSocketContext"
import { ThemeProvider } from "./contexts/ThemeContext"

const App: React.FC = () => {
    return (
        <ThemeProvider>
            <SocketProvider>
                <NotificationProvider>
                    <NotificationSocketProvider>
                        <Router />
                    </NotificationSocketProvider>
                </NotificationProvider>
            </SocketProvider>
        </ThemeProvider>
    )
}

export default App
