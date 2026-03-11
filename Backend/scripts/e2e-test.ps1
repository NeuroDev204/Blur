$ErrorActionPreference = 'Stop'

$base = 'http://localhost:8888/api'
$suffix = [guid]::NewGuid().ToString('N').Substring(0,6)
$user1 = "e2euser${suffix}"
$user2 = "e2euser2${suffix}"
$password = 'Test@123456'
$dob = '2000-01-01'

Write-Host "== Register user1: $user1 =="
$body1 = @{
    username = $user1
    email = "$user1@example.com"
    password = $password
    firstName = 'E2E'
    lastName = 'User1'
    dob = $dob
    city = 'Ha Noi'
} | ConvertTo-Json
$user1Response = Invoke-RestMethod -Method Post -Uri "$base/users/registration" -ContentType 'application/json' -Body $body1
$user1Id = $user1Response.result.id
Write-Host "User1 ID: $user1Id"

Write-Host "== Register user2: $user2 =="
$body2 = @{
    username = $user2
    email = "$user2@example.com"
    password = $password
    firstName = 'E2E'
    lastName = 'User2'
    dob = $dob
    city = 'Da Nang'
} | ConvertTo-Json
$user2Response = Invoke-RestMethod -Method Post -Uri "$base/users/registration" -ContentType 'application/json' -Body $body2
$user2Id = $user2Response.result.id
Write-Host "User2 ID: $user2Id"

Write-Host "== Login user1 =="
$authBody = @{ username = $user1; password = $password } | ConvertTo-Json
$authResponse = Invoke-RestMethod -Method Post -Uri "$base/auth/token" -ContentType 'application/json' -Body $authBody -SessionVariable session
$cookieJar = $session.Cookies.GetCookies('http://localhost:8888')
$accessCookie = $cookieJar['access_token']
Write-Host "Access token cookie set: $([bool]$accessCookie)"

Write-Host "== Introspect token =="
$introspect = Invoke-RestMethod -Method Post -Uri "$base/auth/introspect" -ContentType 'application/json' -Body '{}' -WebSession $session
Write-Host "Introspect valid: $($introspect.result.valid)"

Write-Host "== Profile myInfo =="
$myInfo = Invoke-RestMethod -Method Get -Uri "$base/profile/users/myInfo" -WebSession $session
Write-Host "Profile userId: $($myInfo.result.userId)"

Write-Host "== Create post =="
$postBody = @{ content = 'Hello from E2E'; mediaUrls = @('https://example.com/a.png') } | ConvertTo-Json
$postResponse = Invoke-RestMethod -Method Post -Uri "$base/post/create" -ContentType 'application/json' -Body $postBody -WebSession $session
$postId = $postResponse.result.id
Write-Host "Post ID: $postId"

Write-Host "== Create comment =="
$commentBody = @{ content = 'Nice post!' } | ConvertTo-Json
$commentResponse = Invoke-RestMethod -Method Post -Uri "$base/post/comment/$postId/create" -ContentType 'application/json' -Body $commentBody -WebSession $session
$commentId = $commentResponse.result.id
Write-Host "Comment ID: $commentId"

Write-Host "== Create story =="
$storyBody = @{
    content = 'Story content'
    mediaUrl = 'https://example.com/story.png'
    thumbnailUrl = 'https://example.com/thumb.png'
    timestamp = '2026-03-11T12:00:00Z'
} | ConvertTo-Json
$storyResponse = Invoke-RestMethod -Method Post -Uri "$base/stories/create" -ContentType 'application/json' -Body $storyBody -WebSession $session
$storyId = $storyResponse.result.id
Write-Host "Story ID: $storyId"

Write-Host "== Create conversation with user2 =="
$conversationBody = @{ type = 'DIRECT'; participantIds = @($user2Id) } | ConvertTo-Json
$conversationResponse = Invoke-RestMethod -Method Post -Uri "$base/chat/conversations/create" -ContentType 'application/json' -Body $conversationBody -WebSession $session
$conversationId = $conversationResponse.result.id
Write-Host "Conversation ID: $conversationId"

Write-Host "== Send chat message =="
$messageBody = @{ conversationId = $conversationId; message = 'Hello from E2E' } | ConvertTo-Json
$messageResponse = Invoke-RestMethod -Method Post -Uri "$base/chat/messages/create" -ContentType 'application/json' -Body $messageBody -WebSession $session
$messageId = $messageResponse.result.id
Write-Host "Message ID: $messageId"

Write-Host "== Fetch messages =="
$messages = Invoke-RestMethod -Method Get -Uri "$base/chat/messages?conversationId=$conversationId" -WebSession $session
Write-Host "Messages count: $($messages.result.Count)"

Write-Host "== E2E completed =="
