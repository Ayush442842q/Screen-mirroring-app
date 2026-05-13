import { WebSocketServer } from "ws";

const PORT = Number(process.env.PORT || 8787);
const ROOM_TTL_MS = Number(process.env.ROOM_TTL_MS || 10 * 60 * 1000);

const rooms = new Map();
const clients = new Map();

const server = new WebSocketServer({ port: PORT });

function createRoomCode() {
  const alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  let code = "";
  for (let index = 0; index < 6; index += 1) {
    code += alphabet[Math.floor(Math.random() * alphabet.length)];
  }
  if (rooms.has(code)) return createRoomCode();
  return code;
}

function send(socket, message) {
  if (socket.readyState === socket.OPEN) {
    socket.send(JSON.stringify(message));
  }
}

function sendError(socket, message) {
  send(socket, { type: "room:error", message });
}

function closeRoom(roomCode, reason = "Room closed.") {
  const room = rooms.get(roomCode);
  if (!room) return;
  clearTimeout(room.expiresAt);
  if (room.sender) send(room.sender, { type: "room:closed", reason });
  if (room.receiver) send(room.receiver, { type: "room:closed", reason });
  rooms.delete(roomCode);
}

function relay(roomCode, fromSocket, message) {
  const room = rooms.get(roomCode);
  if (!room) {
    sendError(fromSocket, "Room not found.");
    return;
  }
  const target = fromSocket === room.sender ? room.receiver : room.sender;
  if (!target) {
    sendError(fromSocket, "Peer is not connected yet.");
    return;
  }
  send(target, message);
}

server.on("connection", (socket) => {
  clients.set(socket, { roomCode: null, role: null });

  socket.on("message", (raw) => {
    let message;
    try {
      message = JSON.parse(raw.toString());
    } catch {
      sendError(socket, "Invalid signaling message.");
      return;
    }

    if (message.type === "room:create") {
      const roomCode = createRoomCode();
      const expiresAt = setTimeout(() => closeRoom(roomCode, "Room expired."), ROOM_TTL_MS);
      rooms.set(roomCode, { sender: socket, receiver: null, expiresAt });
      clients.set(socket, { roomCode, role: "sender" });
      send(socket, { type: "room:created", roomCode });
      return;
    }

    if (message.type === "room:join") {
      const roomCode = String(message.roomCode || "").toUpperCase();
      const room = rooms.get(roomCode);
      if (!room) {
        sendError(socket, "Room not found. Check the code and try again.");
        return;
      }
      if (room.receiver && room.receiver.readyState === room.receiver.OPEN) {
        sendError(socket, "Room already has a receiver.");
        return;
      }
      room.receiver = socket;
      clients.set(socket, { roomCode, role: "receiver" });
      send(socket, { type: "room:joined", roomCode });
      send(room.sender, { type: "peer:joined", roomCode });
      return;
    }

    if (["webrtc:offer", "webrtc:answer", "webrtc:ice-candidate"].includes(message.type)) {
      relay(String(message.roomCode || "").toUpperCase(), socket, message);
      return;
    }

    if (message.type === "heartbeat") {
      send(socket, { type: "heartbeat" });
      return;
    }

    sendError(socket, "Unsupported signaling message.");
  });

  socket.on("close", () => {
    const client = clients.get(socket);
    clients.delete(socket);
    if (!client?.roomCode) return;
    const room = rooms.get(client.roomCode);
    if (!room) return;
    if (client.role === "sender") {
      closeRoom(client.roomCode, "Sender disconnected.");
      return;
    }
    if (client.role === "receiver") {
      room.receiver = null;
      if (room.sender) send(room.sender, { type: "room:closed", reason: "Receiver disconnected." });
    }
  });
});

console.log(`MirrorLink signaling server listening on ws://0.0.0.0:${PORT}`);
