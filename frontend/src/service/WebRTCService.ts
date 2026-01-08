// src/services/WebRTCService.ts

type ConnectionStateChangeCallback = (state: RTCPeerConnectionState | string) => void

interface RTCConfiguration {
    iceServers: RTCIceServer[]
    iceCandidatePoolSize: number
}

class WebRTCService {
    private peerConnection: RTCPeerConnection | null = null
    public localStream: MediaStream | null = null
    private remoteStream: MediaStream | null = null
    private pendingIceCandidates: RTCIceCandidateInit[] = []

    private configuration: RTCConfiguration = {
        iceServers: [
            {
                urls: [
                    'stun:stun.l.google.com:19302',
                    'stun:stun1.l.google.com:19302',
                    'stun:stun2.l.google.com:19302'
                ]
            }
        ],
        iceCandidatePoolSize: 10
    }

    async initializeMedia(isVideo: boolean = false): Promise<MediaStream> {
        try {
            const constraints: MediaStreamConstraints = {
                audio: {
                    echoCancellation: true,
                    noiseSuppression: true,
                    autoGainControl: true,
                },
                video: isVideo ? {
                    width: { ideal: 1280, max: 1920 },
                    height: { ideal: 720, max: 1080 },
                    frameRate: { ideal: 30, max: 60 },
                    facingMode: 'user'
                } : false
            }

            this.localStream = await navigator.mediaDevices.getUserMedia(constraints)
            return this.localStream

        } catch (error) {
            const err = error as Error & { name: string }
            if (err.name === 'NotAllowedError') {
                throw new Error('Camera/microphone permission denied. Please allow access in browser settings.')
            } else if (err.name === 'NotFoundError') {
                throw new Error('No camera/microphone found on this device.')
            } else if (err.name === 'NotReadableError') {
                throw new Error('Camera/microphone is already in use by another application.')
            } else {
                throw new Error(`Media error: ${err.message}`)
            }
        }
    }

    createPeerConnection(
        onIceCandidate: (candidate: RTCIceCandidate) => void,
        onTrack: (stream: MediaStream) => void,
        onConnectionStateChange?: ConnectionStateChangeCallback
    ): RTCPeerConnection {
        if (
            this.peerConnection &&
            this.peerConnection.connectionState !== 'closed'
        ) {
            return this.peerConnection
        }

        if (this.peerConnection) {
            this.peerConnection.close()
        }

        this.peerConnection = new RTCPeerConnection(this.configuration)

        // Add local tracks
        if (this.localStream) {
            const tracks = this.localStream.getTracks()
            tracks.forEach((track) => {
                this.peerConnection!.addTrack(track, this.localStream!)
            })
        }

        // ICE candidate handler
        this.peerConnection.onicecandidate = (event: RTCPeerConnectionIceEvent) => {
            if (event.candidate) {
                onIceCandidate(event.candidate)
            }
        }

        // Remote track handler
        this.peerConnection.ontrack = (event: RTCTrackEvent) => {
            if (event.streams && event.streams[0]) {
                this.remoteStream = event.streams[0]
                onTrack(event.streams[0])
            }
        }

        // Connection state handler
        this.peerConnection.onconnectionstatechange = () => {
            const state = this.peerConnection!.connectionState

            if (onConnectionStateChange) {
                onConnectionStateChange(state)
            }

            if (state === 'failed') {
                this.peerConnection!.restartIce()
            }
        }

        // ICE connection state handler
        this.peerConnection.oniceconnectionstatechange = () => {
            const iceState = this.peerConnection!.iceConnectionState
            const connState = this.peerConnection!.connectionState

            if (iceState === 'connected' || iceState === 'completed') {
                if (connState !== 'connected') {
                    if (onConnectionStateChange) {
                        onConnectionStateChange('connected')
                    }
                }
            }
        }

        this.peerConnection.onicegatheringstatechange = () => {
            // ICE gathering state changed
        }

        return this.peerConnection
    }

    async createOffer(): Promise<RTCSessionDescriptionInit> {
        try {
            const offer = await this.peerConnection!.createOffer({
                offerToReceiveAudio: true,
                offerToReceiveVideo: true
            })

            await this.peerConnection!.setLocalDescription(offer)
            return offer
        } catch (error) {
            throw error
        }
    }

    async createAnswer(offer: RTCSessionDescriptionInit): Promise<RTCSessionDescriptionInit> {
        try {
            await this.peerConnection!.setRemoteDescription(offer)

            // Process pending ICE candidates
            for (const candidate of this.pendingIceCandidates) {
                try {
                    await this.peerConnection!.addIceCandidate(new RTCIceCandidate(candidate))
                } catch {
                    // Error adding pending ICE candidate
                }
            }
            this.pendingIceCandidates = []

            const answer = await this.peerConnection!.createAnswer()
            await this.peerConnection!.setLocalDescription(answer)

            return answer
        } catch (error) {
            throw error
        }
    }

    async setRemoteAnswer(answer: RTCSessionDescriptionInit): Promise<void> {
        try {
            await this.peerConnection!.setRemoteDescription(answer)

            // Process pending ICE candidates
            for (const candidate of this.pendingIceCandidates) {
                try {
                    await this.peerConnection!.addIceCandidate(new RTCIceCandidate(candidate))
                } catch {
                    // Error adding pending ICE candidate
                }
            }
            this.pendingIceCandidates = []

        } catch (error) {
            throw error
        }
    }

    async addIceCandidate(candidate: RTCIceCandidateInit): Promise<void> {
        try {
            if (!this.peerConnection) {
                this.pendingIceCandidates.push(candidate)
                return
            }

            if (!this.peerConnection.remoteDescription) {
                this.pendingIceCandidates.push(candidate)
                return
            }

            await this.peerConnection.addIceCandidate(
                new RTCIceCandidate(candidate)
            )
        } catch {
            // Don't throw - ICE candidate errors shouldn't break the call
        }
    }

    toggleAudio(enabled: boolean): void {
        if (this.localStream) {
            this.localStream.getAudioTracks().forEach(track => {
                track.enabled = enabled
            })
        }
    }

    toggleVideo(enabled: boolean): void {
        if (this.localStream) {
            this.localStream.getVideoTracks().forEach(track => {
                track.enabled = enabled
            })
        }
    }

    cleanup(): void {
        this.pendingIceCandidates = []

        if (this.localStream) {
            this.localStream.getTracks().forEach(track => {
                track.stop()
            })
            this.localStream = null
        }

        if (this.peerConnection) {
            this.peerConnection.close()
            this.peerConnection = null
        }

        this.remoteStream = null
    }
}

export default new WebRTCService()
