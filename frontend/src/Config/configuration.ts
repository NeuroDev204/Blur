interface OAuthConfigType {
    clientId: string
    redirectUri: string
    authUri: string
}

export const OAuthConfig: OAuthConfigType = {
    clientId: "948254557534-ca906iu2s6s1a33hcuvhv1pdhp9qmoti.apps.googleusercontent.com",
    redirectUri: "http://localhost:3000/Authenticate",
    authUri: "https://accounts.google.com/o/oauth2/auth",
}
