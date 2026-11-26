package com.shizuku.taptest;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import rikka.shizuku.Shizuku;

public class MainActivity extends AppCompatActivity {
    private static final int REQ = 42;
    private ITapService svc;
    private TextView status;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);

        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        int pad = (int)(16 * getResources().getDisplayMetrics().density);
        ll.setPadding(pad, pad, pad, pad);

        status = new TextView(this);
        ll.addView(status);

        Button grant = new Button(this);
        grant.setText("grant shizuku");
        grant.setOnClickListener(v -> {
            if (Shizuku.pingBinder()) {
                if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED)
                    bind();
                else
                    Shizuku.requestPermission(REQ);
            } else {
                Toast.makeText(this, "shizuku not running", Toast.LENGTH_SHORT).show();
            }
        });
        ll.addView(grant);

        Button tap = new Button(this);
        tap.setText("TAP center of screen");
        tap.setOnClickListener(v -> {
            if (svc == null) { Toast.makeText(this, "bind first", Toast.LENGTH_SHORT).show(); return; }
            try {
                int w = getResources().getDisplayMetrics().widthPixels;
                int h = getResources().getDisplayMetrics().heightPixels;
                svc.tap(w / 2, h / 2);
                Toast.makeText(this, "tap sent", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "tap fail: " + e, Toast.LENGTH_LONG).show();
            }
        });
        ll.addView(tap);

        setContentView(ll);

        Shizuku.addRequestPermissionResultListener((req, code, result) -> {
            if (code == REQ && result == PackageManager.PERMISSION_GRANTED) bind();
        });

        if (Shizuku.pingBinder()
            && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) bind();
    }

    private void bind() {
        Shizuku.UserServiceArgs args = new Shizuku.UserServiceArgs(
            new ComponentName(this, TapUserService.class))
            .daemon(true).processNameSuffix("tap").version(1).debuggable(false);

        Shizuku.bindUserService(args, new ServiceConnection() {
            @Override public void onServiceConnected(ComponentName n, IBinder b) {
                svc = ITapService.Stub.asInterface(b);
                try { status.setText("connected: " + svc.ping()); }
                catch (Exception e) { status.setText("ping fail: " + e); }
            }
            @Override public void onServiceDisconnected(ComponentName n) {
                svc = null;
                status.setText("disconnected");
            }
        });
    }
}