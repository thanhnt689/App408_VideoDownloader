package com.files.video.downloader.videoplayerdownloader.downloader.util.customview;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.NonNull;

public class VolumeSeekbar extends androidx.appcompat.widget.AppCompatSeekBar {
    private OnSeekBarChangeListener onChangeListener;

    public VolumeSeekbar(Context context) {
        super(context);
    }

    public VolumeSeekbar(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    public VolumeSeekbar(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public void onSizeChanged(int i, int i2, int i3, int i4) {
        super.onSizeChanged(i2, i, i4, i3);
    }

    public synchronized void onMeasure(int i, int i2) {
        super.onMeasure(i2, i);
        setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
    }

    public void onDraw(Canvas canvas) {
        canvas.rotate(-90.0f);
        canvas.translate((float) (-getHeight()), 0.0f);
        super.onDraw(canvas);
    }

    public void setOnSeekBarChangeListener(OnSeekBarChangeListener onSeekBarChangeListener) {
        this.onChangeListener = onSeekBarChangeListener;
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (!isEnabled()) {
            return false;
        }
        int action = motionEvent.getAction();
        if (action == 0) {
            this.onChangeListener.onStartTrackingTouch(this);
            setPressed(true);
            setSelected(true);
        } else if (action == 1) {
            this.onChangeListener.onStopTrackingTouch(this);
            setPressed(false);
            setSelected(false);
        } else if (action == 2) {
            super.onTouchEvent(motionEvent);
            int max = getMax() - ((int) ((((float) getMax()) * motionEvent.getY()) / ((float) getHeight())));
            if (max < 0) {
                max = 0;
            }
            if (max > getMax()) {
                max = getMax();
            }
            setProgress(max);
            onSizeChanged(getWidth(), getHeight(), 0, 0);
            this.onChangeListener.onProgressChanged(this, max, true);
            setPressed(true);
            setSelected(true);
        } else if (action == 3) {
            super.onTouchEvent(motionEvent);
            setPressed(false);
            setSelected(false);
        }
        return true;
    }

    public synchronized void setProgressAndThumb(int i) {
        setProgress(getMax() - (getMax() - i));
        onSizeChanged(getWidth(), getHeight(), 0, 0);
    }
}
