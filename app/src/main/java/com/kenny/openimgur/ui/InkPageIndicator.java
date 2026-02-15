package com.kenny.openimgur.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.kenny.openimgur.R;

/**
 * Custom InkPageIndicator implementation compatible with API 17+
 * Displays dots for each page in a ViewPager with an animated ink effect
 */
public class InkPageIndicator extends View implements ViewPager.OnPageChangeListener {
    private static final int DEFAULT_DOT_DIAMETER_DP = 8;
    private static final int DEFAULT_DOT_GAP_DP = 8;
    private static final int DEFAULT_ANIMATION_DURATION = 320;

    private ViewPager mViewPager;
    private Paint mPaint;
    private Paint mCurrentPaint;
    
    private int mDotDiameter;
    private int mDotGap;
    private int mPageIndicatorColor;
    private int mCurrentPageIndicatorColor;
    private int mAnimationDuration;
    
    private int mCurrentPage = 0;
    private int mPageCount = 0;
    
    private float mCurrentDotX;
    private float mTargetDotX;
    private ValueAnimator mAnimator;

    public InkPageIndicator(Context context) {
        this(context, null);
    }

    public InkPageIndicator(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public InkPageIndicator(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        // Default values
        float density = getResources().getDisplayMetrics().density;
        mDotDiameter = (int) (DEFAULT_DOT_DIAMETER_DP * density);
        mDotGap = (int) (DEFAULT_DOT_GAP_DP * density);
        mAnimationDuration = DEFAULT_ANIMATION_DURATION;
        
        // Get default colors from theme
        mPageIndicatorColor = ContextCompat.getColor(context, R.color.primary_dark_light);
        
        // Try to get accent color from theme
        TypedArray themeArray = context.getTheme().obtainStyledAttributes(
                new int[] { R.attr.colorAccent });
        mCurrentPageIndicatorColor = themeArray.getColor(0, 
                ContextCompat.getColor(context, R.color.primary_dark_light));
        themeArray.recycle();

        // Read custom attributes
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.InkPageIndicator);
            mDotDiameter = a.getDimensionPixelSize(R.styleable.InkPageIndicator_dotDiameter, mDotDiameter);
            mDotGap = a.getDimensionPixelSize(R.styleable.InkPageIndicator_dotGap, mDotGap);
            mPageIndicatorColor = a.getColor(R.styleable.InkPageIndicator_pageIndicatorColor, mPageIndicatorColor);
            mCurrentPageIndicatorColor = a.getColor(R.styleable.InkPageIndicator_currentPageIndicatorColor, mCurrentPageIndicatorColor);
            mAnimationDuration = a.getInt(R.styleable.InkPageIndicator_animationDuration, mAnimationDuration);
            a.recycle();
        }

        // Initialize paints
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(mPageIndicatorColor);
        mPaint.setStyle(Paint.Style.FILL);

        mCurrentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCurrentPaint.setColor(mCurrentPageIndicatorColor);
        mCurrentPaint.setStyle(Paint.Style.FILL);
    }

    public void setViewPager(ViewPager viewPager) {
        if (mViewPager != null) {
            mViewPager.removeOnPageChangeListener(this);
        }
        
        mViewPager = viewPager;
        
        if (mViewPager != null) {
            mViewPager.addOnPageChangeListener(this);
            if (mViewPager.getAdapter() != null) {
                mPageCount = mViewPager.getAdapter().getCount();
                mCurrentPage = mViewPager.getCurrentItem();
                updateDotPosition(mCurrentPage, false);
                requestLayout();
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = mPageCount > 0 
                ? (mDotDiameter * mPageCount) + (mDotGap * (mPageCount - 1)) + getPaddingLeft() + getPaddingRight()
                : 0;
        int height = mDotDiameter + getPaddingTop() + getPaddingBottom();
        
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if (mPageCount <= 0) {
            return;
        }

        float radius = mDotDiameter / 2f;
        float y = getPaddingTop() + radius;
        
        // Draw all dots
        for (int i = 0; i < mPageCount; i++) {
            float x = getPaddingLeft() + radius + (i * (mDotDiameter + mDotGap));
            canvas.drawCircle(x, y, radius, mPaint);
        }
        
        // Draw current page indicator
        canvas.drawCircle(mCurrentDotX, y, radius, mCurrentPaint);
    }

    private void updateDotPosition(int position, boolean animate) {
        if (mPageCount <= 0) {
            return;
        }
        
        float radius = mDotDiameter / 2f;
        mTargetDotX = getPaddingLeft() + radius + (position * (mDotDiameter + mDotGap));
        
        if (!animate || mCurrentDotX == 0) {
            mCurrentDotX = mTargetDotX;
            invalidate();
        } else {
            animateDot();
        }
    }

    private void animateDot() {
        if (mAnimator != null && mAnimator.isRunning()) {
            mAnimator.cancel();
        }
        
        mAnimator = ValueAnimator.ofFloat(mCurrentDotX, mTargetDotX);
        mAnimator.setDuration(mAnimationDuration);
        mAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mCurrentDotX = (Float) animation.getAnimatedValue();
                invalidate();
            }
        });
        mAnimator.start();
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        // Optional: add smooth scrolling animation based on offset
    }

    @Override
    public void onPageSelected(int position) {
        mCurrentPage = position;
        updateDotPosition(position, true);
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        // Not needed for basic implementation
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAnimator != null && mAnimator.isRunning()) {
            mAnimator.cancel();
        }
        if (mViewPager != null) {
            mViewPager.removeOnPageChangeListener(this);
        }
    }
}
