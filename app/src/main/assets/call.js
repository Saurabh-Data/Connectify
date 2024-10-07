let localVideo = document.getElementById("local-video");
let remoteVideo = document.getElementById("remote-video");

localVideo.style.opacity = 0;
remoteVideo.style.opacity = 0;

localVideo.onplaying = () => { localVideo.style.opacity = 1; };
remoteVideo.onplaying = () => { remoteVideo.style.opacity = 1; };

let peer;
let currentCall;
let localStream;

function init(userId) {
    peer = new Peer(userId, {
        port: 443,
        path: '/'
    });

    peer.on('open', () => {
        Android.onPeerConnected();
    });

    listen();
}

function listen() {
    peer.on('call', (call) => {
        navigator.getUserMedia({
            audio: true,
            video: true
        }, (stream) => {
            localVideo.srcObject = stream;
            localStream = stream;

            call.answer(stream);
            call.on('stream', (remoteStream) => {
                remoteVideo.srcObject = remoteStream;

                remoteVideo.className = "primary-video";
                localVideo.className = "secondary-video";
            });

            call.on('close', () => {
                endCall();
            });

            currentCall = call;
        });
    });
}

function startCall(otherUserId) {
    navigator.getUserMedia({
        audio: true,
        video: true
    }, (stream) => {
        localVideo.srcObject = stream;
        localStream = stream;

        const call = peer.call(otherUserId, stream);
        call.on('stream', (remoteStream) => {
            remoteVideo.srcObject = remoteStream;

            remoteVideo.className = "primary-video";
            localVideo.className = "secondary-video";
        });

        call.on('close', () => {
            endCall();
        });

        currentCall = call;
    });
}

function endCall() {
    if (currentCall) {
        currentCall.close();
        currentCall = null;
    }

    if (localStream) {
        localStream.getTracks().forEach(track => track.stop());
        localStream = null;
    }

    localVideo.srcObject = null;
    remoteVideo.srcObject = null;

    localVideo.className = "";
    remoteVideo.className = "";
}

function toggleVideo(b) {
    if (localStream) {
        localStream.getVideoTracks()[0].enabled = (b === "true");
    }
}

function toggleAudio(b) {
    if (localStream) {
        localStream.getAudioTracks()[0].enabled = (b === "true");
    }
}
