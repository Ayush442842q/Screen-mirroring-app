package dev.mirrorlink.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final int TEAL = Color.rgb(12, 102, 88);
    private static final int INK = Color.rgb(24, 32, 31);
    private static final int MUTED = Color.rgb(93, 111, 107);
    private static final int BG = Color.rgb(248, 250, 247);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(createContent());
    }

    private LinearLayout createContent() {
        int padding = dp(24);

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

        TextView status = new TextView(this);
        status.setText("Android sender alpha");
        status.setTextColor(TEAL);
        status.setTextSize(16);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, dp(10), 0, dp(28));
        status.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        root.addView(status, matchWidthWrapHeight());

        TextView title = new TextView(this);
        title.setText("Share this Android screen to another device.");
        title.setTextColor(INK);
        title.setTextSize(31);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setLineSpacing(0, 0.96f);
        root.addView(title, matchWidthWrapHeight());

        TextView body = new TextView(this);
        body.setText("This first release installs the native Android app shell. MediaProjection screen capture and WebRTC sender streaming are next.");
        body.setTextColor(MUTED);
        body.setTextSize(16);
        body.setGravity(Gravity.CENTER);
        body.setLineSpacing(dp(3), 1.0f);
        body.setPadding(0, dp(18), 0, dp(28));
        root.addView(body, matchWidthWrapHeight());

        Button primary = new Button(this);
        primary.setText("Start screen share");
        primary.setTextColor(Color.WHITE);
        primary.setTextSize(16);
        primary.setAllCaps(false);
        primary.setEnabled(false);
        primary.setBackgroundColor(TEAL);
        root.addView(primary, buttonLayout());

        TextView note = new TextView(this);
        note.setText("Coming next: Android MediaProjection permission flow, signaling connection, and WebRTC video track sender.");
        note.setTextColor(MUTED);
        note.setTextSize(13);
        note.setGravity(Gravity.CENTER);
        note.setPadding(0, dp(18), 0, 0);
        root.addView(note, matchWidthWrapHeight());

        return root;
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
                dp(52)
        );
        params.setMargins(0, 0, 0, 0);
        return params;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
