package luisbc92.gameoflifewatchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Message;
import android.os.Handler;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.view.SurfaceHolder;

import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Created by Luis on 2/8/2015.
 */
public class GameOfLifeFaceService extends CanvasWatchFaceService {
    private static final String TAG = "GameOfLifeFaceService";

    // Update rate in milliseconds for normal mode.
    private static final long NORMAL_UPDATE_RATE_MS = 250;

    // Update rate in milliseconds for mute mode.
    private static final long MUTE_UPDATE_RATE_MS = TimeUnit.MINUTES.toMillis(1);

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        static final int MSG_REFRESH = 0;

        // Drawing
        final int mGridSize = 32;
        final float mCellSize = 3;
        Paint mBackgroundPaint;
        Paint mGridPaint;
        Paint mCellPaint;
        Paint mDeadPaint;
        Paint mHourPaint;
        Paint mMinutePaint;
        Paint mSecondPaint;
        Paint mTickPaint;

        // Life
        Life mLife;

        // Time
        Time mTime;

        // Randomness
        Random mRandom;

        // Handler to refresh animation
        final Handler mRefreshHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_REFRESH:
                        refresh();
                        if (shouldTimerBeRunning()) {
                            long timeMs = System.currentTimeMillis();
                            long delayMs = NORMAL_UPDATE_RATE_MS - (timeMs % NORMAL_UPDATE_RATE_MS);
                            mRefreshHandler.sendEmptyMessageDelayed(MSG_REFRESH, delayMs);
                        }
                        break;
                }
            }
        };

        // TimeZone updater
        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };
        boolean mRegisteredTimeZoneReceiver = false;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            // Initialize randomness
            mRandom = new Random();

            // Configure watchface
            setWatchFaceStyle(new WatchFaceStyle.Builder(GameOfLifeFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());

            // Initialize paints
            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(Color.BLACK);

            mGridPaint = new Paint();
            mGridPaint.setColor(Color.DKGRAY);
            mGridPaint.setStyle(Paint.Style.STROKE);
            mGridPaint.setStrokeWidth(0);

            mCellPaint = new Paint();
            mCellPaint.setColor(Color.CYAN);

            mDeadPaint = new Paint();
            mDeadPaint.setColor(Color.DKGRAY);

            mHourPaint = new Paint();
            mHourPaint.setARGB(255, 200, 200, 200);
            mHourPaint.setStrokeWidth(5.f);
            mHourPaint.setAntiAlias(true);
            mHourPaint.setStrokeCap(Paint.Cap.ROUND);

            mMinutePaint = new Paint();
            mMinutePaint.setARGB(255, 200, 200, 200);
            mMinutePaint.setStrokeWidth(3.f);
            mMinutePaint.setAntiAlias(true);
            mMinutePaint.setStrokeCap(Paint.Cap.ROUND);

            mSecondPaint = new Paint();
            mSecondPaint.setARGB(255, 255, 0, 0);
            mSecondPaint.setStrokeWidth(2.f);
            mSecondPaint.setAntiAlias(true);
            mSecondPaint.setStrokeCap(Paint.Cap.ROUND);

            mTickPaint = new Paint();
            mTickPaint.setARGB(100, 255, 255, 255);
            mTickPaint.setStrokeWidth(2.f);
            mTickPaint.setAntiAlias(true);

            // Initialize GoL
            mLife = new Life(mGridSize, mGridSize);
            int center = mGridSize / 2;

            int i = 100;
            while (i-- > 0) {
                int x = mRandom.nextInt(mGridSize-1);
                int y = mRandom.nextInt(mGridSize-1);
                mLife.addCell(x, y);
            }

            mLife.updateWorld();

            // Initialize time
            mTime = new Time();
        }

        @Override
        public void onDestroy() {
            mRefreshHandler.removeMessages(MSG_REFRESH);
            super.onDestroy();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            mTime.setToNow();

            int width = bounds.width();
            int height = bounds.height();
            float gridX = width / mGridSize;
            float gridY = width / mGridSize;

            // Draw background
            canvas.drawRect(0, 0, width, height, mBackgroundPaint);

            // Draw grid
            for (float i = 1; i < mGridSize; i++) {
                canvas.drawLine(gridX*i, 0, gridX*i, height, mGridPaint);
                canvas.drawLine(0, gridY*i, width, gridY*i, mGridPaint);
            }

            // Draw cells
            for (int w = 0; w < mGridSize; w++) {
                for (int h = 0; h < mGridSize; h++) {
                    if ((mLife.getWorld()[w][h] & 0x01) == 1) {
                        float centerX = (gridX * (float) w) + (gridX / 2);
                        float centerY = (gridY * (float) h) + (gridY / 2);
                        canvas.drawRect(centerX - mCellSize,
                                centerY - mCellSize,
                                centerX + mCellSize,
                                centerY + mCellSize,
                                mCellPaint);
                    }
                    if ((mLife.getWorld()[w][h] & 0x04) == 4) {
                        float centerX = (gridX * (float) w) + (gridX / 2);
                        float centerY = (gridY * (float) h) + (gridY / 2);
                        canvas.drawRect(centerX - mCellSize,
                                centerY - mCellSize,
                                centerX + mCellSize,
                                centerY + mCellSize,
                                mDeadPaint);
                    }
                }
            }

            // Draw clock
            // Find the center. Ignore the window insets so that, on round watches with a
            // "chin", the watch face is centered on the entire screen, not just the usable
            // portion.
            float centerX = width / 2f;
            float centerY = height / 2f;

            // Draw the ticks.
            float innerTickRadius = centerX - 10;
            float outerTickRadius = centerX;
            for (int tickIndex = 0; tickIndex < 12; tickIndex++) {
                float tickRot = (float) (tickIndex * Math.PI * 2 / 12);
                float innerX = (float) Math.sin(tickRot) * innerTickRadius;
                float innerY = (float) -Math.cos(tickRot) * innerTickRadius;
                float outerX = (float) Math.sin(tickRot) * outerTickRadius;
                float outerY = (float) -Math.cos(tickRot) * outerTickRadius;
                canvas.drawLine(centerX + innerX, centerY + innerY,
                        centerX + outerX, centerY + outerY, mTickPaint);
            }

            float secRot = mTime.second / 30f * (float) Math.PI;
            int minutes = mTime.minute;
            float minRot = minutes / 30f * (float) Math.PI;
            float hrRot = ((mTime.hour + (minutes / 60f)) / 6f ) * (float) Math.PI;

            float secLength = centerX - 20;
            float minLength = centerX - 40;
            float hrLength = centerX - 80;

            if (!isInAmbientMode()) {
                float secX = (float) Math.sin(secRot) * secLength;
                float secY = (float) -Math.cos(secRot) * secLength;
                canvas.drawLine(centerX, centerY, centerX + secX, centerY + secY, mSecondPaint);
            }

            float minX = (float) Math.sin(minRot) * minLength;
            float minY = (float) -Math.cos(minRot) * minLength;
            canvas.drawLine(centerX, centerY, centerX + minX, centerY + minY, mMinutePaint);

            float hrX = (float) Math.sin(hrRot) * hrLength;
            float hrY = (float) -Math.cos(hrRot) * hrLength;
            canvas.drawLine(centerX, centerY, centerX + hrX, centerY + hrY, mHourPaint);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            } else {
                unregisterReceiver();

                mLife.resetWorld();

                int i = 200;
                while (i-- > 0) {
                    int x = mRandom.nextInt(mGridSize-1);
                    int y = mRandom.nextInt(mGridSize-1);
                    mLife.addCell(x, y);
                }

                mLife.updateWorld();

            }

            updateTimer();
        }

        private void refresh() {
            mLife.updateWorld();
            invalidate();
        }

        private void updateTimer() {
            mRefreshHandler.removeMessages(MSG_REFRESH);
            if (shouldTimerBeRunning()) {
                mRefreshHandler.sendEmptyMessage(MSG_REFRESH);
            }
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) return;
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            GameOfLifeFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) return;
            mRegisteredTimeZoneReceiver = false;
            GameOfLifeFaceService.this.unregisterReceiver(mTimeZoneReceiver);
        }

        // Timer should run only when visible and interactive mode
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }
    }

}












