import WebSocket from "ws";

const SIGNAL_URL = process.env.SIGNAL_URL || "ws://127.0.0.1:8787";

function connectClient(name) {
  return new Promise((resolve, reject) => {
    const socket = new WebSocket(SIGNAL_URL);
    const messages = [];

    socket.on("message", (raw) => {
      messages.push(JSON.parse(raw.toString()));
    });

    socket.on("open", () => resolve({ name, socket, messages }));
    socket.on("error", reject);
  });
}

function waitFor(client, type) {
  return new Promise((resolve, reject) => {
    const existing = client.messages.find((message) => message.type === type);
    if (existing) {
      resolve(existing);
      return;
    }

    const timeout = setTimeout(() => {
      client.socket.off("message", onMessage);
      reject(new Error(`${client.name} did not receive ${type}`));
    }, 3000);

    function onMessage(raw) {
      const message = JSON.parse(raw.toString());
      if (message.type === type) {
        clearTimeout(timeout);
        client.socket.off("message", onMessage);
        resolve(message);
      }
    }

    client.socket.on("message", onMessage);
  });
}

const sender = await connectClient("sender");
sender.socket.send(JSON.stringify({ type: "room:create", role: "sender" }));

const created = await waitFor(sender, "room:created");
const receiver = await connectClient("receiver");
receiver.socket.send(JSON.stringify({ type: "room:join", roomCode: created.roomCode, role: "receiver" }));

await waitFor(receiver, "room:joined");
await waitFor(sender, "peer:joined");

sender.socket.send(
  JSON.stringify({
    type: "webrtc:offer",
    roomCode: created.roomCode,
    sdp: { type: "offer", sdp: "v=0" }
  })
);

await waitFor(receiver, "webrtc:offer");

sender.socket.close();
receiver.socket.close();

console.log(`Smoke test passed for room ${created.roomCode}`);
