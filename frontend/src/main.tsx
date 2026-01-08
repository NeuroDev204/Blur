import React from 'react'
import ReactDOM from 'react-dom/client'
import './index.css'
import App from './App'
import { BrowserRouter } from 'react-router-dom'
import { ChakraProvider } from '@chakra-ui/react'

const rootElement = document.getElementById('root')

if (!rootElement) {
    throw new Error('Root element not found')
}

const root = ReactDOM.createRoot(rootElement)
root.render(
    <BrowserRouter>
        <ChakraProvider>
            <App />
        </ChakraProvider>
    </BrowserRouter>
)
