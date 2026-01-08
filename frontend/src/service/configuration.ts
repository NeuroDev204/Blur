interface ConfigType {
    API_GATEWAY: string
}

interface ApiEndpoints {
    MY_CONVERSATIONS: string
    CREATE_CONVERSATION: string
    CREATE_CHATMESSAGE: string
    GET_MESSAGES: string
}

export const CONFIG: ConfigType = {
    API_GATEWAY: "http://localhost:8888/api/"
}

export const API: ApiEndpoints = {
    MY_CONVERSATIONS: "chat/conversations/my-conversations",
    CREATE_CONVERSATION: "chat/conversations/create",
    CREATE_CHATMESSAGE: "chat/messages/create",
    GET_MESSAGES: "chat/messages"
}
