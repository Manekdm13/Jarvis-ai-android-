package com.manekdm13.jarvis

import android.content.Context
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Audio-only WebRTC media engine.
 *
 * This class deliberately does not start recording by itself: callers must obtain
 * Android RECORD_AUDIO permission and only create/enable it after the user accepts
 * an incoming call. Supabase signalling is supplied by the call coordinator.
 */
internal class JarvisWebRtcEngine(
    context: Context,
    private val callbacks: Callbacks
) {
    interface Callbacks {
        fun onLocalIceCandidate(candidate: IceCandidate)
        fun onConnectionState(state: PeerConnection.PeerConnectionState)
        fun onIceState(state: PeerConnection.IceConnectionState)
        fun onRemoteAudioTrack(track: AudioTrack)
        fun onError(message: String)
    }

    private val appContext = context.applicationContext
    private val factory: PeerConnectionFactory
    private val audioSource: AudioSource
    private val audioTrack: AudioTrack
    private var peer: PeerConnection? = null
    private val closed = AtomicBoolean(false)

    init {
        synchronized(initialized) {
            if (initialized.compareAndSet(false, true)) {
                PeerConnectionFactory.initialize(
                    PeerConnectionFactory.InitializationOptions.builder(appContext).createInitializationOptions()
                )
            }
        }
        factory = PeerConnectionFactory.builder().createPeerConnectionFactory()
        audioSource = factory.createAudioSource(MediaConstraints())
        audioTrack = factory.createAudioTrack("jarvis-mic", audioSource).apply { setEnabled(true) }
    }

    fun start() {
        check(!closed.get()) { "Call engine is closed." }
        if (peer != null) return
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        )
        val config = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }
        peer = factory.createPeerConnection(config, observer)
            ?: throw IllegalStateException("WebRTC could not create a peer connection.")
        peer?.addTrack(audioTrack, listOf("jarvis-audio"))
    }

    fun createOffer(onReady: (String) -> Unit) {
        val connection = requirePeer()
        connection.createOffer(sdpObserver(onReady), MediaConstraints())
    }

    fun acceptOffer(offerSdp: String, onAnswerReady: (String) -> Unit) {
        val connection = requirePeer()
        val offer = SessionDescription(SessionDescription.Type.OFFER, offerSdp)
        connection.setRemoteDescription(object : SdpObserver by EmptySdpObserver() {
            override fun onSetSuccess() {
                connection.createAnswer(sdpObserver(onAnswerReady), MediaConstraints())
            }
            override fun onSetFailure(error: String) {
                callbacks.onError("Could not apply incoming call offer: $error")
            }
        }, offer)
    }

    fun acceptAnswer(answerSdp: String, onApplied: (() -> Unit)? = null) {
        val answer = SessionDescription(SessionDescription.Type.ANSWER, answerSdp)
        requirePeer().setRemoteDescription(object : SdpObserver by EmptySdpObserver() {
            override fun onSetSuccess() { onApplied?.invoke() }
            override fun onSetFailure(error: String) {
                callbacks.onError("Could not apply call answer: $error")
            }
        }, answer)
    }

    fun addRemoteIceCandidate(candidate: IceCandidate) {
        val accepted = requirePeer().addIceCandidate(candidate)
        if (!accepted) callbacks.onError("WebRTC rejected a remote network candidate.")
    }

    fun setMicrophoneEnabled(enabled: Boolean) {
        audioTrack.setEnabled(enabled)
    }

    fun close() {
        if (!closed.compareAndSet(false, true)) return
        runCatching { audioTrack.setEnabled(false) }
        runCatching { peer?.close() }
        runCatching { peer?.dispose() }
        peer = null
        runCatching { audioTrack.dispose() }
        runCatching { audioSource.dispose() }
        runCatching { factory.dispose() }
    }

    private fun requirePeer(): PeerConnection =
        peer ?: throw IllegalStateException("Start the call engine before signalling.")

    private fun sdpObserver(onLocalDescription: (String) -> Unit) =
        object : SdpObserver by EmptySdpObserver() {
            override fun onCreateSuccess(description: SessionDescription) {
                val connection = peer ?: return
                connection.setLocalDescription(object : SdpObserver by EmptySdpObserver() {
                    override fun onSetSuccess() { onLocalDescription(description.description) }
                    override fun onSetFailure(error: String) {
                        callbacks.onError("Could not set local call description: $error")
                    }
                }, description)
            }
            override fun onCreateFailure(error: String) {
                callbacks.onError("Could not create call description: $error")
            }
        }

    private val observer = object : PeerConnection.Observer {
        override fun onSignalingChange(state: PeerConnection.SignalingState) = Unit
        override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
            callbacks.onIceState(state)
        }
        override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit
        override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) = Unit
        override fun onIceCandidate(candidate: IceCandidate) {
            callbacks.onLocalIceCandidate(candidate)
        }
        override fun onIceCandidatesRemoved(candidates: Array<IceCandidate>) = Unit
        override fun onAddStream(stream: org.webrtc.MediaStream) {
            stream.audioTracks.firstOrNull()?.let(callbacks::onRemoteAudioTrack)
        }
        override fun onRemoveStream(stream: org.webrtc.MediaStream) = Unit
        override fun onDataChannel(channel: org.webrtc.DataChannel) = Unit
        override fun onRenegotiationNeeded() = Unit
        override fun onAddTrack(receiver: org.webrtc.RtpReceiver, streams: Array<org.webrtc.MediaStream>) {
            (receiver.track() as? AudioTrack)?.let(callbacks::onRemoteAudioTrack)
        }
        override fun onConnectionChange(state: PeerConnection.PeerConnectionState) {
            callbacks.onConnectionState(state)
        }
    }

    private class EmptySdpObserver : SdpObserver {
        override fun onCreateSuccess(description: SessionDescription) = Unit
        override fun onSetSuccess() = Unit
        override fun onCreateFailure(error: String) = Unit
        override fun onSetFailure(error: String) = Unit
    }

    companion object {
        private val initialized = AtomicBoolean(false)
    }
}
