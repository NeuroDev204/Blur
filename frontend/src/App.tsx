import React from "react"
import Router from "./Pages/Router/Router"
import { SocketProvider } from "./contexts/SocketContext"
import { NotificationProvider } from "./contexts/NotificationContext"
import { NotificationSocketProvider } from "./contexts/NotificationSocketContext"

const App: React.FC = () => {
    return (
        <SocketProvider>
            <NotificationProvider>
                <NotificationSocketProvider>
                    <Router />
                </NotificationSocketProvider>
            </NotificationProvider>
        </SocketProvider>
    )
}

export default App
