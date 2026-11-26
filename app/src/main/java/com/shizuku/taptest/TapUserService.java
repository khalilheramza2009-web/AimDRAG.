package com.shizuku.taptest;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import java.lang.reflect.Method;

public class TapUserService extends Service {
    private static final String TAG = "TapUserService";
    private Object inputManager;
    private Method injectMethod;

    private final ITapService.Stub binder = new ITapService.Stub() {
        @Override public String ping() {
            return "alive uid=" + android.os.Process.myUid();
        }
        @Override public void tap(int x, int y) {
            try {
                if (inputManager == null) initInjector();
                injectTap(x, y);
            } catch (Throwable t) {
                Log.e(TAG, "tap fail", t);
            }
        }
    };

    @Override public IBinder onBind(Intent i) { return binder; }

    private void initInjector() throws Exception {
        try {
            Class<?> c = Class.forName("android.hardware.input.InputManager");
            Method g = c.getDeclaredMethod("getInstance");
            g.setAccessible(true);
            inputManager = g.invoke(null);
        } catch (NoSuchMethodException e) {
            Class<?> c = Class.forName("android.hardware.input.InputManagerGlobal");
            Method g = c.getDeclaredMethod("getInstance");
            g.setAccessible(true);
            inputManager = g.invoke(null);
        }
        injectMethod = inputManager.getClass()
            .getMethod("injectInputEvent", android.view.InputEvent.class, int.class);
        injectMethod.setAccessible(true);
    }

    private void injectTap(int x, int y) throws Exception {
        android.view.MotionEvent.PointerProperties pp =
            new android.view.MotionEvent.PointerProperties();
        pp.id = 7;
        pp.toolType = android.view.MotionEvent.TOOL_TYPE_FINGER;

        android.view.MotionEvent.PointerCoords pc =
            new android.view.MotionEvent.PointerCoords();
        pc.x = x; pc.y = y; pc.pressure = 1f; pc.size = 1f;

        long now = android.os.SystemClock.uptimeMillis();

        android.view.MotionEvent down = android.view.MotionEvent.obtain(
            now, now, android.view.MotionEvent.ACTION_DOWN,
            1, new android.view.MotionEvent.PointerProperties[]{pp},
            new android.view.MotionEvent.PointerCoords[]{pc},
            0, 0, 1f, 1f, 0, 0,
            android.view.InputDevice.SOURCE_TOUCHSCREEN, 0);
        injectMethod.invoke(inputManager, down, 0);
        down.recycle();

        try { Thread.sleep(30); } catch (InterruptedException ignored) {}

        android.view.MotionEvent up = android.view.MotionEvent.obtain(
            now, now + 30, android.view.MotionEvent.ACTION_UP,
            1, new android.view.MotionEvent.PointerProperties[]{pp},
            new android.view.MotionEvent.PointerCoords[]{pc},
            0, 0, 1f, 1f, 0, 0,
            android.view.InputDevice.SOURCE_TOUCHSCREEN, 0);
        injectMethod.invoke(inputManager, up, 0);
        up.recycle();
    }
}