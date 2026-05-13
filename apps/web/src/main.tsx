import React, { useEffect, useMemo, useRef, useState } from "react";
import { createRoot } from "react-dom/client";
import { QRCodeSVG } from "qrcode.react";
import {
  Airplay,
  Cast,
  CheckCircle2,
  Copy,
  Expand,
  MonitorUp,
  Pause,
  Play,
  QrCode,
  Radio,
  RotateCcw,
  ScanLine,
  Settings2,
  ShieldCheck,
  Signal,
  Smartphone,
  StopCircle,
  Tablet,
  Tv,
  Volume2,
  Wifi
} from "lucide-react";
import "./styles.css";

type Role = "home" | "sender" | "receiver" | "tv";
type ConnectionState =
  | "idle"
  | "connecting"
  | "room-ready"
  | "waiting"
  | "paired"
  | "streaming"
  | "error";

type SignalMessage =
  | { type: "room:create"; role: "sender" }
  | { type: "room:created"; roomCode: string }
  | { type: "room:join"; roomCode: string; role: "receiver" }
  | { type: "room:joined"; roomCode: string }
  | { type: "peer:joined"; roomCode: string }
  | { type: "webrtc:offer"; roomCode: string; sdp: RTCSessionDescriptionInit }
  | { type: "webrtc:answer"; roomCode: string; sdp: RTCSessionDescriptionInit }
  | { type: "webrtc:ice-candidate"; roomCode: string; candidate: RTCIceCandidateInit }
  | { type: "room:error"; message: string }
  | { type: "room:closed"; reason?: string };

const rtcConfig: RTCConfiguration = {
  iceServers: [{ urls: "stun:stun.l.google.com:19302" }]
};

const getDefaultSignalUrl = () => {
  const protocol = window.location.protocol === "https:" ? "wss" : "ws";
  return `${protocol}://${window.location.hostname}:8787`;
};

function App() {
  const [role, setRole] = useState<Role>("home");
  const [roomCode, setRoomCode] = useState("");
  const [joinCode, setJoinCode] = useState("");
  const [status, setStatus] = useState<ConnectionState>("idle");
  const [statusText, setStatusText] = useState("Choose how this device should connect.");
  const [signalUrl, setSignalUrl] = useState(getDefaultSignalUrl);
  const [quality, setQuality] = useState("Auto");
  const [audioEnabled, setAudioEnabled] = useState(true);
  const [isFullscreenMode, setIsFullscreenMode] = useState(false);

  const socketRef = useRef<WebSocket | null>(null);
  const peerRef = useRef<RTCPeerConnection | null>(null);
  const localStreamRef = useRef<MediaStream | null>(null);
  const localVideoRef = useRef<HTMLVideoElement | null>(null);
  const remoteVideoRef = useRef<HTMLVideoElement | null>(null);

  const receiverUrl = useMemo(() => {
    const url = new URL(window.location.href);
    url.searchParams.set("room", roomCode);
    url.searchParams.set("mode", "receive");
    return url.toString();
  }, [roomCode]);

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const mode = params.get("mode");
    const room = params.get("room");
    if (mode === "receive") {
      setRole("receiver");
      if (room) setJoinCode(room);
    }
  }, []);

  useEffect(() => {
    return () => cleanup();
  }, []);

  const connectSocket = () =>
    new Promise<WebSocket>((resolve, reject) => {
      const socket = new WebSocket(signalUrl);
      socketRef.current = socket;
      socket.onopen = () => resolve(socket);
      socket.onerror = () => reject(new Error("Could not connect to signaling server."));
      socket.onmessage = (event) => handleSignal(JSON.parse(event.data));
    });

  const ensurePeer = (activeRoom: string) => {
    if (peerRef.current) return peerRef.current;
    const peer = new RTCPeerConnection(rtcConfig);
    peerRef.current = peer;

    peer.onicecandidate = (event) => {
      if (event.candidate) {
        send({ type: "webrtc:ice-candidate", roomCode: activeRoom, candidate: event.candidate.toJSON() });
      }
    };

    peer.onconnectionstatechange = () => {
      if (peer.connectionState === "connected") {
        setStatus("streaming");
        setStatusText("Stream connected.");
      }
      if (["failed", "disconnected"].includes(peer.connectionState)) {
        setStatus("error");
        setStatusText("The peer connection was interrupted.");
      }
    };

    peer.ontrack = (event) => {
      const [stream] = event.streams;
      if (remoteVideoRef.current) {
        remoteVideoRef.current.srcObject = stream;
      }
      setStatus("streaming");
      setStatusText("Receiving live screen stream.");
    };

    return peer;
  };

  const send = (message: SignalMessage) => {
    const socket = socketRef.current;
    if (!socket || socket.readyState !== WebSocket.OPEN) return;
    socket.send(JSON.stringify(message));
  };

  const handleSignal = async (message: SignalMessage) => {
    if (message.type === "room:created") {
      setRoomCode(message.roomCode);
      setStatus("room-ready");
      setStatusText("Room is ready. Ask the receiver to scan or enter the code.");
    }

    if (message.type === "room:joined") {
      setRoomCode(message.roomCode);
      setStatus("paired");
      setStatusText("Receiver joined. Waiting for the sender stream.");
    }

    if (message.type === "peer:joined") {
      setStatus("paired");
      setStatusText("Receiver paired. Preparing WebRTC offer.");
      await createOffer(message.roomCode);
    }

    if (message.type === "webrtc:offer") {
      const peer = ensurePeer(message.roomCode);
      await peer.setRemoteDescription(message.sdp);
      const answer = await peer.createAnswer();
      await peer.setLocalDescription(answer);
      send({ type: "webrtc:answer", roomCode: message.roomCode, sdp: answer });
      setStatusText("Answer sent. Connecting stream.");
    }

    if (message.type === "webrtc:answer") {
      const peer = ensurePeer(message.roomCode);
      await peer.setRemoteDescription(message.sdp);
      setStatusText("Answer received. Connecting stream.");
    }

    if (message.type === "webrtc:ice-candidate") {
      const peer = ensurePeer(message.roomCode);
      await peer.addIceCandidate(message.candidate);
    }

    if (message.type === "room:error") {
      setStatus("error");
      setStatusText(message.message);
    }

    if (message.type === "room:closed") {
      setStatus("idle");
      setStatusText(message.reason || "Room closed.");
      cleanup(false);
    }
  };

  const startShare = async () => {
    try {
      setRole("sender");
      setStatus("connecting");
      setStatusText("Connecting to signaling server.");
      await connectSocket();

      const stream = await navigator.mediaDevices.getDisplayMedia({
        video: {
          frameRate: quality === "Smooth" ? 60 : 30
        },
        audio: audioEnabled
      });

      localStreamRef.current = stream;
      if (localVideoRef.current) localVideoRef.current.srcObject = stream;
      send({ type: "room:create", role: "sender" });
      setStatus("waiting");
      setStatusText("Creating room.");
    } catch (error) {
      setStatus("error");
      setStatusText(error instanceof Error ? error.message : "Could not start sharing.");
    }
  };

  const createOffer = async (activeRoom: string) => {
    const peer = ensurePeer(activeRoom);
    localStreamRef.current?.getTracks().forEach((track) => {
      if (localStreamRef.current) peer.addTrack(track, localStreamRef.current);
    });
    const offer = await peer.createOffer();
    await peer.setLocalDescription(offer);
    send({ type: "webrtc:offer", roomCode: activeRoom, sdp: offer });
    setStatusText("Offer sent. Waiting for receiver answer.");
  };

  const joinRoom = async (mode: Role = "receiver") => {
    const normalizedCode = joinCode.trim().toUpperCase();
    if (!normalizedCode) {
      setStatus("error");
      setStatusText("Enter a room code first.");
      return;
    }

    try {
      setRole(mode);
      setStatus("connecting");
      setStatusText("Connecting to signaling server.");
      await connectSocket();
      ensurePeer(normalizedCode);
      send({ type: "room:join", roomCode: normalizedCode, role: "receiver" });
      setStatus("waiting");
      setStatusText("Joining room.");
    } catch (error) {
      setStatus("error");
      setStatusText(error instanceof Error ? error.message : "Could not join room.");
    }
  };

  const cleanup = (resetState = true) => {
    localStreamRef.current?.getTracks().forEach((track) => track.stop());
    localStreamRef.current = null;
    peerRef.current?.close();
    peerRef.current = null;
    socketRef.current?.close();
    socketRef.current = null;
    if (localVideoRef.current) localVideoRef.current.srcObject = null;
    if (remoteVideoRef.current) remoteVideoRef.current.srcObject = null;
    if (resetState) {
      setStatus("idle");
      setStatusText("Choose how this device should connect.");
      setRoomCode("");
    }
  };

  const copyReceiverLink = async () => {
    await navigator.clipboard.writeText(receiverUrl);
    setStatusText("Receiver link copied.");
  };

  return (
    <main className={`app ${role === "tv" ? "tv-mode" : ""}`}>
      <header className="topbar">
        <button className="brand" onClick={() => setRole("home")} aria-label="MirrorLink home">
          <span className="brand-mark">
            <Cast size={19} />
          </span>
          <span>MirrorLink</span>
        </button>
        <nav className="mode-tabs" aria-label="Device modes">
          <button className={role === "sender" ? "active" : ""} onClick={() => setRole("sender")}>
            <MonitorUp size={16} />
            Sender
          </button>
          <button className={role === "receiver" ? "active" : ""} onClick={() => setRole("receiver")}>
            <Smartphone size={16} />
            Receiver
          </button>
          <button className={role === "tv" ? "active" : ""} onClick={() => setRole("tv")}>
            <Tv size={16} />
            TV
          </button>
        </nav>
      </header>

      <section className="status-strip">
        <span className={`status-dot ${status}`} />
        <span>{statusText}</span>
      </section>

      {role === "home" && (
        <section className="home-grid">
          <div className="intro">
            <h1>Mirror a screen to any nearby browser.</h1>
            <p>
              Start with laptop-to-mobile and Android-to-browser. Receivers work on phones, tablets, laptops, and TV
              browsers.
            </p>
            <div className="hero-actions">
              <button className="primary" onClick={startShare}>
                <Play size={18} />
                Share this screen
              </button>
              <button className="secondary" onClick={() => setRole("receiver")}>
                <ScanLine size={18} />
                Receive a screen
              </button>
            </div>
          </div>

          <DevicePreview />
        </section>
      )}

      {role === "sender" && (
        <section className="workspace">
          <div className="stream-panel">
            <div className="panel-head">
              <div>
                <h2>Laptop sender</h2>
                <p>Share a browser tab, window, or full screen.</p>
              </div>
              <div className="toolbar">
                <button className="icon-button" title="Toggle audio" onClick={() => setAudioEnabled((value) => !value)}>
                  <Volume2 size={18} />
                </button>
                <button className="icon-button" title="Settings">
                  <Settings2 size={18} />
                </button>
                <button className="danger" onClick={() => cleanup()}>
                  <StopCircle size={18} />
                  Stop
                </button>
              </div>
            </div>
            <div className="video-frame">
              <video ref={localVideoRef} autoPlay muted playsInline />
              {!localStreamRef.current && (
                <div className="empty-video">
                  <Airplay size={42} />
                  <span>No local preview yet</span>
                </div>
              )}
            </div>
            <div className="quality-row">
              {["Auto", "Sharp", "Smooth"].map((item) => (
                <button key={item} className={quality === item ? "selected" : ""} onClick={() => setQuality(item)}>
                  {item}
                </button>
              ))}
            </div>
          </div>

          <aside className="pairing-panel">
            <h2>Pair receiver</h2>
            <div className="room-code">{roomCode || "------"}</div>
            <div className="qr-box">
              {roomCode ? <QRCodeSVG value={receiverUrl} size={152} /> : <QrCode size={76} />}
            </div>
            <button className="primary full" onClick={roomCode ? copyReceiverLink : startShare}>
              {roomCode ? <Copy size={18} /> : <Play size={18} />}
              {roomCode ? "Copy receiver link" : "Start sharing"}
            </button>
            <ConnectionChecklist status={status} />
          </aside>
        </section>
      )}

      {role === "receiver" && (
        <section className={`receiver-layout ${isFullscreenMode ? "focus" : ""}`}>
          <div className="join-panel">
            <h2>Receive screen</h2>
            <p>Enter the sender room code or open a QR link from another device.</p>
            <label>
              Room code
              <input
                value={joinCode}
                maxLength={6}
                onChange={(event) => setJoinCode(event.target.value.toUpperCase())}
                placeholder="A1B2C3"
              />
            </label>
            <label>
              Signaling server
              <input value={signalUrl} onChange={(event) => setSignalUrl(event.target.value)} />
            </label>
            <button className="primary full" onClick={() => joinRoom("receiver")}>
              <Radio size={18} />
              Join room
            </button>
          </div>
          <ReceiverPlayer videoRef={remoteVideoRef} onFullscreen={() => setIsFullscreenMode((value) => !value)} />
        </section>
      )}

      {role === "tv" && (
        <section className="tv-lobby">
          <div className="tv-card">
            <Tv size={54} />
            <h1>Ready to receive</h1>
            <input
              value={joinCode}
              maxLength={6}
              onChange={(event) => setJoinCode(event.target.value.toUpperCase())}
              placeholder="ROOM CODE"
              aria-label="Room code"
            />
            <button className="primary tv-button" onClick={() => joinRoom("tv")}>
              <Wifi size={24} />
              Connect TV
            </button>
          </div>
          <ReceiverPlayer videoRef={remoteVideoRef} onFullscreen={() => setIsFullscreenMode((value) => !value)} />
        </section>
      )}
    </main>
  );
}

function DevicePreview() {
  return (
    <div className="device-preview" aria-label="Responsive device preview">
      <div className="preview-laptop">
        <div className="preview-topline" />
        <div className="preview-video">
          <Signal size={34} />
        </div>
      </div>
      <div className="preview-phone">
        <div className="phone-speaker" />
        <div className="phone-stream">
          <Smartphone size={30} />
        </div>
      </div>
      <div className="preview-tablet">
        <Tablet size={34} />
      </div>
    </div>
  );
}

function ReceiverPlayer({
  videoRef,
  onFullscreen
}: {
  videoRef: React.RefObject<HTMLVideoElement | null>;
  onFullscreen: () => void;
}) {
  return (
    <div className="receiver-player">
      <div className="video-frame receiver">
        <video ref={videoRef} autoPlay playsInline />
        <div className="empty-video">
          <Cast size={46} />
          <span>Waiting for stream</span>
        </div>
      </div>
      <div className="receiver-controls">
        <button className="icon-button" title="Fullscreen" onClick={onFullscreen}>
          <Expand size={19} />
        </button>
        <button className="icon-button" title="Rotate">
          <RotateCcw size={19} />
        </button>
        <button className="icon-button" title="Pause">
          <Pause size={19} />
        </button>
      </div>
    </div>
  );
}

function ConnectionChecklist({ status }: { status: ConnectionState }) {
  const items = [
    ["Room", status !== "idle"],
    ["Receiver", ["paired", "streaming"].includes(status)],
    ["Stream", status === "streaming"]
  ] as const;

  return (
    <div className="checklist">
      {items.map(([label, complete]) => (
        <div key={label}>
          <CheckCircle2 size={17} className={complete ? "done" : ""} />
          <span>{label}</span>
        </div>
      ))}
      <div>
        <ShieldCheck size={17} />
        <span>Local room</span>
      </div>
    </div>
  );
}

createRoot(document.getElementById("root")!).render(<App />);
