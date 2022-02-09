package com.sahni.rahul.fakenotedetector.ui;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;

import android.view.View;
import android.view.animation.LinearInterpolator;

import com.sahni.rahul.fakenotedetector.R;


public class ColourIndicatorView extends View implements ValueAnimator.AnimatorUpdateListener {

    private Paint mCirclePaint;
    private Paint mBlueArcPaint;
    private Paint mGreenArcPaint;
    private Path mBluePath;
    private Path mGreenPath;
    private int centerX;
    private int centerY;
    private int radius;
    private RectF mRect;

    private float sweepAngle = 180;

    private ValueAnimator valueAnimator;
    private ValueAnimator greenValueAnimator;
    private ValueAnimator blueValueAnimator;

    private boolean isBlue = false;
    private boolean isGreen = false;

    private static final int ARC_STROKE_WIDTH = 15;


    public ColourIndicatorView(Context context) {
        super(context);
        init();
    }

    public ColourIndicatorView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ColourIndicatorView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }


    private void init(){
        mCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCirclePaint.setStyle(Paint.Style.FILL);
        mCirclePaint.setColor(ContextCompat.getColor(getContext(), android.R.color.darker_gray));

        mBlueArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBlueArcPaint.setColor(ContextCompat.getColor(getContext(), R.color.noteBlue));
        mBlueArcPaint.setStyle(Paint.Style.STROKE);
        mBlueArcPaint.setStrokeWidth(ARC_STROKE_WIDTH);
//        mBluePath = new Path();

        mGreenArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mGreenArcPaint.setColor(ContextCompat.getColor(getContext(), R.color.noteGreen));
        mGreenArcPaint.setStyle(Paint.Style.STROKE);
        mGreenArcPaint.setStrokeWidth(ARC_STROKE_WIDTH);
//        mGreenPath = new Path();

        valueAnimator = ValueAnimator.ofFloat(0, 180);
        valueAnimator.setDuration(800);
        valueAnimator.setInterpolator(new LinearInterpolator());
        valueAnimator.addUpdateListener(this);

//        blueValueAnimator = ValueAnimator.ofFloat(0,180);
//        greenValueAnimator = ValueAnimator.ofFloat(0, 180);
//
//        blueValueAnimator.setDuration(800);
//        greenValueAnimator.setDuration(800);
//
//        blueValueAnimator.setInterpolator(new LinearInterpolator());
//        greenValueAnimator.setInterpolator(new LinearInterpolator());
//
//        blueValueAnimator.addUpdateListener(this);

    }



    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        centerX = getMeasuredWidth() / 2;
        centerY = getMeasuredHeight() / 2;
        radius = Math.min(centerX, centerY);
        setMeasuredDimension(2*radius+ARC_STROKE_WIDTH, 2*radius+ARC_STROKE_WIDTH);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mRect == null)
        {
            // mRect will define the drawing space for drawArc()
            // We have to take into account the STROKE_WIDTH with drawArc() as well as drawCircle():
            // circles as well as arcs are drawn 50% outside of the bounds defined by the radius (radius for arcs is calculated from the rectangle mRect).
            // So if mRect is too large, the lines will not fit into the View

            int startTop = (int) (ARC_STROKE_WIDTH/1.8);
            int startLeft = startTop;

            mRect = new RectF(startLeft, startTop, 2*radius, 2*radius);

        }

        canvas.drawCircle(centerX,centerY, radius, mCirclePaint);
        if(isGreen) {
//            mBluePath.addArc(mRect, 180,sweepAngle);
//            ValueAnimator valueAnimator = ValueAnimator.ofFloat()
            canvas.drawArc(mRect, 180, sweepAngle, false, mGreenArcPaint);
        }

        if(isBlue) {
//            mGreenPath.addArc(mRect, 0, 180);
            canvas.drawArc(mRect, 0, sweepAngle, false, mBlueArcPaint);
        }


    }

    public void showBlue(){
        isBlue = true;
        valueAnimator.start();
//        invalidate();
    }

    public void showGreen(){
        isGreen = true;
        valueAnimator.start();
        invalidate();
    }

    public void reset(){
        isGreen = false;
        isBlue = false;
        if(valueAnimator.isRunning()){
            valueAnimator.cancel();
        }
        invalidate();
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
         sweepAngle = (float) animation.getAnimatedValue();
         invalidate();

    }
}
