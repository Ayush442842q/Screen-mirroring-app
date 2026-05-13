package dev.mirrorlink.app;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.ScreenCapturerAndroid;
import org.webrtc.SessionDescription;
import org.webrtc.SdpObserver;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class MainActivity extends Activity {
    private static final int TEAL = Color.rgb(12, 102, 88);
    private static final int INK = Color.rgb(24, 32, 31);
    private static final int MUTED = Color.rgb(93, 111, 107);
    private static final int BG = Color.rgb(248, 250, 247);
    private static final int REQUEST_CAPTURE = 1001;
    private static final int REQUEST_NOTIFICATIONS = 1002;
    private static final String SIGNAL_URL = "ws://127.0.0.1:8787";

    private TextView status;
    private TextView roomCode;
    private EditText joinCode;
    private Button startButton;
    private Button joinButton;
    private SurfaceViewRenderer videoView;

    private EglBase eglBase;
    private PeerConnectionFactory peerFactory;
    private PeerConnection peerConnection;
    private WebSocket socket;
    private VideoCapturer screenCapturer;
    private VideoSource videoSource;
    private String activeRoomCode = "";
    private boolean isSender = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestNotificationPermission();
        setupWebRtc();
        setContentView(createContent());
        String room = getIntent().getStringExtra("roomCode");
        if (room != null && !room.trim().isEmpty()) {
            joinRoomFromIntent(room);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String room = intent.getStringExtra("roomCode");
        if (room != null && !room.trim().isEmpty()) {
            joinRoomFromIntent(room);
        }
    }

    @Override
    protected void onDestroy() {
        cleanup();
        super.onDestroy();
    }

    private LinearLayout createContent() {
        int padding = dp(20);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(padding, padding, padding, padding);
        root.setBackgroundColor(BG);
        root.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        TextView brand = new TextView(this);
        brand.setText("MirrorLink");
        brand.setTextColor(INK);
        brand.setTextSize(28);
        brand.setGravity(Gravity.CENTER);
        brand.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        root.addView(brand, matchWidthWrapHeight());

        status = new TextView(this);
        status.setText("Start on ZD2227H2FK, then enter the room code on 780e6887.");
        status.setTextColor(MUTED);
        status.setTextSize(15);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, dp(8), 0, dp(14));
        root.addView(status, matchWidthWrapHeight());

        roomCode = new TextView(this);
        roomCode.setText("ROOM: ------");
        roomCode.setTextColor(TEAL);
        roomCode.setTextSize(24);
        roomCode.setGravity(Gravity.CENTER);
        roomCode.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        roomCode.setPadding(0, 0, 0, dp(12));
        root.addView(roomCode, matchWidthWrapHeight());

        videoView = new SurfaceViewRenderer(this);
        videoView.init(eglBase.getEglBaseContext(), null);
        videoView.setMirror(false);
        videoView.setEnableHardwareScaler(true);
        LinearLayout.LayoutParams videoParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
        );
        videoParams.setMargins(0, 0, 0, dp(14));
        root.addView(videoView, videoParams);

        startButton = new Button(this);
        startButton.setText("Start screen share");
        startButton.setTextColor(Color.WHITE);
        startButton.setTextSize(16);
        startButton.setAllCaps(false);
        startButton.setBackgroundColor(TEAL);
        startButton.setOnClickListener((view) -> requestScreenCapture());
        root.addView(startButton, buttonLayout());

        joinCode = new EditText(this);
        joinCode.setHint("Enter room code on receiver");
        joinCode.setSingleLine(true);
        joinCode.setTextColor(INK);
        joinCode.setHintTextColor(MUTED);
        joinCode.setGravity(Gravity.CENTER);
        joinCode.setAllCaps(true);
        root.addView(joinCode, inputLayout());

        joinButton = new Button(this);
        joinButton.setText("Join as receiver");
        joinButton.setTextColor(Color.WHITE);
        joinButton.setTextSize(16);
        joinButton.setAllCaps(false);
        joinButton.setBackgroundColor(TEAL);
        joinButton.setOnClickListener((view) -> joinAsReceiver());
        root.addView(joinButton, buttonLayout());

        return root;
    }

    private void setupWebRtc() {
        eglBase = EglBase.create();
        PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(this).createInitializationOptions()
        );
        DefaultVideoEncoderFactory encoderFactory = new DefaultVideoEncoderFactory(
                eglBase.getEglBaseContext(),
                true,
                true
        );
        DefaultVideoDecoderFactory decoderFactory = new DefaultVideoDecoderFactory(eglBase.getEglBaseContext());
        peerFactory = PeerConnectionFactory.builder()
                .setVideoEncoderFactory(encoderFactory)
                .setVideoDecoderFactory(decoderFactory)
                .createPeerConnectionFactory();
    }

    private void requestScreenCapture() {
        isSender = true;
        setStatus("Requesting Android screen capture permission.");
        MediaProjectionManager manager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(manager.createScreenCaptureIntent(), REQUEST_CAPTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_CAPTURE) return;
        if (resultCode != RESULT_OK || data == null) {
            setStatus("Screen capture permission was denied.");
            return;
        }
        startService(new Intent(this, ProjectionService.class));
        connectSocket(() -> {
            createPeerConnection();
            startScreenTrack(resultCode, data);
            send(put(json("room:create"), "role", "sender"));
            setStatus("Creating sender room.");
        });
    }

    private void joinAsReceiver() {
        String code = joinCode.getText().toString().trim().toUpperCase(Locale.US);
        if (code.isEmpty()) {
            setStatus("Enter the sender room code first.");
            return;
        }
        closePeerAndSocket();
        isSender = false;
        activeRoomCode = code;
        roomCode.setText("ROOM: " + code);
        connectSocket(() -> {
            createPeerConnection();
            send(put(put(json("room:join"), "roomCode", code), "role", "receiver"));
            setStatus("Joining room " + code + ".");
        });
    }

    private void joinRoomFromIntent(String room) {
        joinCode.setText(room.trim().toUpperCase(Locale.US));
        joinAsReceiver();
    }

    private void connectSocket(Runnable onOpen) {
        setStatus("Connecting to signaling server " + SIGNAL_URL + ".");
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(SIGNAL_URL).build();
        socket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                runOnUiThread(onOpen);
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                runOnUiThread(() -> handleSignal(text));
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable throwable, Response response) {
                runOnUiThread(() -> setStatus("Signaling failed: " + throwable.getMessage()));
            }
        });
    }

    private void handleSignal(String text) {
        try {
            JSONObject message = new JSONObject(text);
            String type = message.optString("type");
            if ("room:created".equals(type)) {
                activeRoomCode = message.getString("roomCode");
                roomCode.setText("ROOM: " + activeRoomCode);
                setStatus("Room ready. Enter this code on 780e6887.");
                return;
            }
            if ("room:joined".equals(type)) {
                setStatus("Receiver joined room. Waiting for sender offer.");
                return;
            }
            if ("peer:joined".equals(type)) {
                setStatus("Receiver paired. Creating offer.");
                createOffer();
                return;
            }
            if ("webrtc:offer".equals(type)) {
                receiveOffer(message);
                return;
            }
            if ("webrtc:answer".equals(type)) {
                receiveAnswer(message);
                return;
            }
            if ("webrtc:ice-candidate".equals(type)) {
                receiveIceCandidate(message);
                return;
            }
            if ("room:error".equals(type)) {
                setStatus(message.optString("message", "Room error."));
                return;
            }
            if ("room:closed".equals(type)) {
                setStatus(message.optString("reason", "Room closed."));
            }
        } catch (JSONException error) {
            setStatus("Bad signaling message: " + error.getMessage());
        }
    }

    private void createPeerConnection() {
        if (peerConnection != null) return;
        List<PeerConnection.IceServer> iceServers = new ArrayList<>();
        iceServers.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer());
        PeerConnection.RTCConfiguration config = new PeerConnection.RTCConfiguration(iceServers);
        peerConnection = peerFactory.createPeerConnection(config, new PeerConnection.Observer() {
            @Override
            public void onIceCandidate(IceCandidate candidate) {
                JSONObject payload = put(
                        put(
                                put(
                                        put(json("webrtc:ice-candidate"), "roomCode", activeRoomCode),
                                        "candidate",
                                        candidate.sdp
                                ),
                                "sdpMid",
                                candidate.sdpMid
                        ),
                        "sdpMLineIndex",
                        candidate.sdpMLineIndex
                );
                send(payload);
            }

            @Override
            public void onAddStream(MediaStream stream) {
                if (!stream.videoTracks.isEmpty()) {
                    stream.videoTracks.get(0).addSink(videoView);
                    setStatus("Receiving screen stream.");
                }
            }

            @Override
            public void onTrack(org.webrtc.RtpTransceiver transceiver) {
                RtpReceiver receiver = transceiver.getReceiver();
                if (receiver.track() instanceof VideoTrack) {
                    ((VideoTrack) receiver.track()).addSink(videoView);
                    setStatus("Receiving screen stream.");
                }
            }

            @Override public void onSignalingChange(PeerConnection.SignalingState state) {}
            @Override public void onIceConnectionChange(PeerConnection.IceConnectionState state) {
                runOnUiThread(() -> setStatus("Connection: " + state.name()));
            }
            @Override public void onIceConnectionReceivingChange(boolean receiving) {}
            @Override public void onIceGatheringChange(PeerConnection.IceGatheringState state) {}
            @Override public void onIceCandidatesRemoved(IceCandidate[] candidates) {}
            @Override public void onAddTrack(RtpReceiver receiver, MediaStream[] streams) {}
            @Override public void onRemoveStream(MediaStream stream) {}
            @Override public void onDataChannel(org.webrtc.DataChannel dataChannel) {}
            @Override public void onRenegotiationNeeded() {}
        });
    }

    private void startScreenTrack(int resultCode, Intent data) {
        screenCapturer = new ScreenCapturerAndroid(data, new MediaProjectionCallback());
        SurfaceTextureHelper helper = SurfaceTextureHelper.create("ScreenCaptureThread", eglBase.getEglBaseContext());
        videoSource = peerFactory.createVideoSource(false);
        screenCapturer.initialize(helper, this, videoSource.getCapturerObserver());
        try {
            screenCapturer.startCapture(720, 1280, 30);
        } catch (Exception error) {
            setStatus("Could not start screen capture: " + error.getMessage());
            return;
        }
        VideoTrack videoTrack = peerFactory.createVideoTrack("screen-video", videoSource);
        videoTrack.addSink(videoView);
        peerConnection.addTrack(videoTrack, Collections.singletonList("screen"));

        AudioSource audioSource = peerFactory.createAudioSource(new MediaConstraints());
        AudioTrack audioTrack = peerFactory.createAudioTrack("screen-audio", audioSource);
        peerConnection.addTrack(audioTrack, Collections.singletonList("screen"));
        setStatus("Screen capture ready. Waiting for receiver.");
    }

    private void createOffer() {
        peerConnection.createOffer(new SimpleSdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                peerConnection.setLocalDescription(new SimpleSdpObserver(), sessionDescription);
                send(put(
                        put(
                                put(json("webrtc:offer"), "roomCode", activeRoomCode),
                                "sdp",
                                sessionDescription.description
                        ),
                        "sdpType",
                        sessionDescription.type.canonicalForm()
                ));
                setStatus("Offer sent. Waiting for answer.");
            }
        }, new MediaConstraints());
    }

    private void receiveOffer(JSONObject message) throws JSONException {
        SessionDescription offer = descriptionFrom(message);
        peerConnection.setRemoteDescription(new SimpleSdpObserver() {
            @Override
            public void onSetSuccess() {
                peerConnection.createAnswer(new SimpleSdpObserver() {
                    @Override
                    public void onCreateSuccess(SessionDescription sessionDescription) {
                        peerConnection.setLocalDescription(new SimpleSdpObserver(), sessionDescription);
                        send(put(
                                put(
                                        put(json("webrtc:answer"), "roomCode", activeRoomCode),
                                        "sdp",
                                        sessionDescription.description
                                ),
                                "sdpType",
                                sessionDescription.type.canonicalForm()
                        ));
                        setStatus("Answer sent. Connecting stream.");
                    }
                }, new MediaConstraints());
            }
        }, offer);
    }

    private void receiveAnswer(JSONObject message) throws JSONException {
        peerConnection.setRemoteDescription(new SimpleSdpObserver(), descriptionFrom(message));
        setStatus("Answer received. Connecting stream.");
    }

    private void receiveIceCandidate(JSONObject message) throws JSONException {
        IceCandidate candidate = new IceCandidate(
                message.getString("sdpMid"),
                message.getInt("sdpMLineIndex"),
                message.getString("candidate")
        );
        peerConnection.addIceCandidate(candidate);
    }

    private SessionDescription descriptionFrom(JSONObject message) throws JSONException {
        String type = message.optString("sdpType");
        String sdp = message.optString("sdp");
        if (sdp.isEmpty() && message.opt("sdp") instanceof JSONObject) {
            JSONObject nested = message.getJSONObject("sdp");
            type = nested.optString("type");
            sdp = nested.optString("sdp");
        }
        return new SessionDescription(SessionDescription.Type.fromCanonicalForm(type), sdp);
    }

    private JSONObject json(String type) {
        JSONObject object = new JSONObject();
        try {
            object.put("type", type);
        } catch (JSONException ignored) {
        }
        return object;
    }

    private JSONObject put(JSONObject object, String key, Object value) {
        try {
            object.put(key, value);
        } catch (JSONException ignored) {
        }
        return object;
    }

    private void send(JSONObject message) {
        if (socket != null) socket.send(message.toString());
    }

    private void setStatus(String text) {
        status.setText(text);
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
        }
    }

    private void cleanup() {
        if (screenCapturer != null) {
            try {
                screenCapturer.stopCapture();
            } catch (InterruptedException ignored) {
            }
            screenCapturer.dispose();
            screenCapturer = null;
        }
        if (videoSource != null) {
            videoSource.dispose();
            videoSource = null;
        }
        closePeerAndSocket();
        stopService(new Intent(this, ProjectionService.class));
    }

    private void closePeerAndSocket() {
        if (peerConnection != null) {
            peerConnection.close();
            peerConnection.dispose();
            peerConnection = null;
        }
        if (socket != null) {
            socket.close(1000, "Activity closed");
            socket = null;
        }
    }

    private LinearLayout.LayoutParams matchWidthWrapHeight() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private LinearLayout.LayoutParams buttonLayout() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(50)
        );
        params.setMargins(0, 0, 0, dp(10));
        return params;
    }

    private LinearLayout.LayoutParams inputLayout() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(54)
        );
        params.setMargins(0, 0, 0, dp(10));
        return params;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private static class MediaProjectionCallback extends android.media.projection.MediaProjection.Callback {
        @Override
        public void onStop() {
        }
    }

    private static class SimpleSdpObserver implements SdpObserver {
        @Override public void onCreateSuccess(SessionDescription sessionDescription) {}
        @Override public void onSetSuccess() {}
        @Override public void onCreateFailure(String error) {}
        @Override public void onSetFailure(String error) {}
    }
}
