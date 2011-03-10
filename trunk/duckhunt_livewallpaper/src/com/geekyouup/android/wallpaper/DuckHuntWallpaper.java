package com.geekyouup.android.wallpaper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.SystemClock;
import android.service.wallpaper.WallpaperService;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

/*
 * This animated wallpaper draws a rotating wireframe cube.
 */
public class DuckHuntWallpaper extends WallpaperService {

    private final Handler mHandler = new Handler();

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public Engine onCreateEngine() {
        return new CubeEngine(this);
    }

    class CubeEngine extends Engine {

        private final Paint mPaint = new Paint();
        private final Paint mTextPaint = new Paint();
        private float mOffset;
        private float mTouchX = 50;
        private float mTouchY = 50;
        private long mStartTime;
        private DuckObject mDuck;
        private Drawable mFireImage;
        private Bitmap mBackgroundImage;
        private Rect mFullScreenRect;
        private boolean mFiring = false;
        private Context mContext;
        private boolean mDrawFire = false;
        private int mMissed = 0;
        private int mHit = 0;
        private int mCanvasHeight = 480;
        private int mCanvasWidth = 320;
        
        private final Runnable mDrawCube = new Runnable() {
            public void run() {
            	updatePhysics();
                drawFrame();
            }
        };
        private boolean mVisible;

        public CubeEngine(Context context) {
            // Create a Paint to draw the lines for our cube
        	mContext=context;
         	mDuck = new DuckObject(context, mCanvasHeight, mCanvasWidth);
         	mBackgroundImage = BitmapFactory.decodeResource(context.getResources(),R.drawable.background2);
         	mFireImage = context.getResources().getDrawable(R.drawable.fire);
         	mFullScreenRect = new Rect(0,0,mCanvasWidth,mCanvasHeight);

            mTextPaint.setColor(0xff1010ff);
            mTextPaint.setTextSize(30);
            mTextPaint.setAntiAlias(true);
            
            mStartTime = SystemClock.elapsedRealtime();
        }

        //callback to say object has been hit or moved offscreen
        public void objectComplete(boolean objectHit)
        {
        	//object hit or not, when to schedule next one
        	if(objectHit)mHit++;
        	else mMissed++;
        }
        
        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);

            // By default we don't get touch events, so enable them.
            setTouchEventsEnabled(true);
        }
        
        @Override
        public void onDestroy() {
            super.onDestroy();
            mHandler.removeCallbacks(mDrawCube);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            mVisible = visible;
            if (visible) {
                drawFrame();
            } else {
                mHandler.removeCallbacks(mDrawCube);
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            // store the center of the surface, so we can draw the cube in the right spot
            mCanvasHeight = height;
            mCanvasWidth = width;
            mFullScreenRect = new Rect(0,0,width,height);
            mDuck = new DuckObject(mContext, height, width);
            drawFrame();
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            mVisible = false;
            mHandler.removeCallbacks(mDrawCube);
        }

        @Override
        public void onOffsetsChanged(float xOffset, float yOffset,float xStep, float yStep, int xPixels, int yPixels) {
            mOffset = xOffset;
            drawFrame();
        }

        /*
         * Store the position of the touch event so we can use it for drawing later
         */
        @Override
        public void onTouchEvent(MotionEvent event) {
        	mTouchX = event.getX();
            mTouchY = event.getY();
            mFiring=true;
            mDrawFire=true;
            super.onTouchEvent(event);
        }

        public void updatePhysics()
        {
        	mDuck.updatePhysics(this, mFiring, (int) mTouchX, (int) mTouchY);
        	if(mFiring) mFiring=false;
        }
        
        /*
         * Draw one frame of the animation. This method gets called repeatedly
         * by posting a delayed Runnable. You can do any drawing you want in
         * here. This example draws a wireframe cube.
         */
        void drawFrame() {
            final SurfaceHolder holder = getSurfaceHolder();

            Canvas c = null;
            try {
                c = holder.lockCanvas();
                if (c != null) {
                    // draw something
                	//c.drawColor(0xff222222);
                	c.drawBitmap(mBackgroundImage, null, mFullScreenRect, null);
                	//c.drawText("Hello Richard", mTouchX, mTouchY, mTextPaint);
                	
                	if(mDrawFire)
                	{
                        mFireImage.setBounds((int) mTouchX-16, (int)mTouchY-11, (int)mTouchX+16,(int) mTouchY+11);
                        mFireImage.draw(c);
                        mDrawFire=false;
                	}
                	c.drawText(mMissed+"", 10, mCanvasHeight-10, mTextPaint);
                	c.drawText(mHit+"", mCanvasWidth-10-mTextPaint.measureText(mHit+""), mCanvasHeight-10, mTextPaint);
                	mDuck.draw(c);
                }
            } finally {
                if (c != null) holder.unlockCanvasAndPost(c);
            }

            // Reschedule the next redraw
            mHandler.removeCallbacks(mDrawCube);
            if (mVisible) {
                mHandler.postDelayed(mDrawCube, 1000 / 25);
            }
        }
    }
}
