import React from "react"
import Router from "./Pages/Router/Router"
import { SocketProvider } from "./contexts/SocketContext"
import { NotificationProvider } from "./contexts/NotificationContext"
import { NotificationSocketProvider } from "./contexts/NotificationSocketContext"
import { ThemeProvider } from "./contexts/ThemeContext"
import { CallProvider } from "./contexts/CallContext"

const App: React.FC = () => {
    return (
        <ThemeProvider>
            <SocketProvider>
                <NotificationProvider>
                    <NotificationSocketProvider>
                        {/* CallProvider o tang app -> nhan cuoc goi den o BAT KY trang nao (home, profile, message...) */}
                        <CallProvider>
                            <Router />
                        </CallProvider>
                    </NotificationSocketProvider>
                </NotificationProvider>
            </SocketProvider>
        </ThemeProvider>
    )
}

export default App
