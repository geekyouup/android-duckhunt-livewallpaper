package com.geekyouup.android.wallpaper;

import java.util.Random;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.Log;

public class DuckObject {

    private float mDuckTopX=0;
    private float mDuckTopY=0;
    private static final String KEY_DUCK_X = "duck_x";
    private static final String KEY_DUCK_Y = "duck_y";
    private static final String KEY_DX = "mDX";
    private static final String KEY_DY = "mDY";
    
    private Drawable mDuckFlyLeftImage;
    private Drawable mDuckFlyRightImage;
    private Drawable mDuckDeadImage;
    private Random mRand = new Random();
    
    private int mDuckWidth;
    private int mDuckHeight;
    
    private float mDX = 0;
    private float mDY = 0;
    
    private int mState = 0;
    private int STATE_WAITING=0;
    private int STATE_ONSCREEN = 1;
    private int STATE_HIT = 2;
    private long mStateStartTime=0;
    private int mTimeBetweenTargets = 0;
    private int mCanvasHeight = 480;
    private int mCanvasWidth = 320;

    public DuckObject(Context context, int canvasHeight, int canvasWidth)
    {
        mDuckFlyLeftImage = context.getResources().getDrawable(R.drawable.flyleft);
        mDuckFlyRightImage = context.getResources().getDrawable(R.drawable.flyright);
        mDuckDeadImage = context.getResources().getDrawable(R.drawable.deadduck);
        
        mDuckWidth = mDuckFlyLeftImage.getIntrinsicWidth();
        mDuckHeight = mDuckFlyLeftImage.getIntrinsicWidth();
        
        mCanvasHeight = canvasHeight;
        mCanvasWidth = canvasWidth;
        
        mTimeBetweenTargets=5000;
    }
    
    public void saveState(SharedPreferences.Editor map) 
    {
        if (map != null) {
            map.putFloat(KEY_DUCK_X,mDuckTopX);
            map.putFloat(KEY_DUCK_Y,mDuckTopY);
            map.putFloat(KEY_DX, mDX);
            map.putFloat(KEY_DY, mDY);
        }
    }
    
    public synchronized void restoreState(SharedPreferences map) {
    	if(map!=null)
    	{
            mDuckTopX = map.getFloat(KEY_DUCK_X,0);
            mDuckTopY = map.getFloat(KEY_DUCK_Y,0);
            mDX = map.getFloat(KEY_DX,0);
            mDY = map.getFloat(KEY_DY,0);
    	}
    }

    public void draw(Canvas canvas)
    {
    	if(mState == STATE_ONSCREEN)
    	{
            if(mDX < 0) //duck flying left
            {
            	mDuckFlyLeftImage.setBounds((int) mDuckTopX, (int) mDuckTopY, (int) mDuckTopX + mDuckWidth, (int) mDuckTopY + mDuckHeight);
            	mDuckFlyLeftImage.draw(canvas);
            }else if(mDX >= 0) //duck flying right
            {
            	mDuckFlyRightImage.setBounds((int) mDuckTopX, (int) mDuckTopY, (int) mDuckTopX + mDuckWidth, (int) mDuckTopY + mDuckHeight);
            	mDuckFlyRightImage.draw(canvas);
            }
    	}else if(mState == STATE_HIT)
    	{
        	mDuckDeadImage.setBounds((int) mDuckTopX, (int) mDuckTopY, (int) mDuckTopX + 109, (int) mDuckTopY + 48);
        	mDuckDeadImage.draw(canvas);
    	}
    }
       
    public boolean isHit(int shotX, int shotY)
    {
		if(shotX>mDuckTopX 
    			&& shotX<(mDuckTopX+mDuckWidth)
    			&& shotY>mDuckTopY 
    			&& shotY<(mDuckTopY+mDuckHeight) )
		{
			return true;
		}
		return false;
    }
    
    public void moveIncrementally()
    {
    	//stop duck from escaping left or right
    	//if((mDuckTopX>mCanvasWidth-40 && mDX>0) || (mDuckTopX<0 && mDX<0)) mDX = -mDX;
    	
    	mDuckTopX += mDX;
    	mDuckTopY -= mDY;
    }
    
    public void resetLocation()
    {
    	int speed = mRand.nextInt(20)+2;
    	setState(STATE_ONSCREEN);

    	float ddx = (speed+1)*2+2;
    	float xy  = ((mRand.nextFloat()*ddx)-ddx/2);
    	if(xy<0 && xy>-1) xy=-0.5f*(speed+1);
    	if(xy>=0 && xy<1) xy=0.5f*(speed+1);
    	
    	mDX = xy;
    	mDY = speed*0.3f+0.5f;
    	
    	mDuckTopX = (mDX>0?-mDuckWidth:mCanvasWidth);
    	mDuckTopY = mCanvasHeight-mRand.nextInt(mCanvasHeight-50);
    	Log.d("DuckPaper","New Duck x:"+mDuckTopX + " y:"+mDuckTopY);
    	mTimeBetweenTargets = 2000;
    }
    
    public int updatePhysics(DuckHuntWallpaper.CubeEngine controller, boolean isFiring, int mFireX, int mFireY)
    {
    	int score = 0;
    	long now = System.currentTimeMillis();
    	
    	if(mState == STATE_WAITING  && now - mTimeBetweenTargets>=mStateStartTime)
    	{
    		resetLocation();
    	}else if(mState == STATE_ONSCREEN)
    	{
    		if(mDuckTopY<-mDuckHeight || mDuckTopX<-mDuckWidth || mDuckTopX>mCanvasWidth) //moved off screen, complete object
    		{
    			setState(STATE_WAITING);
    			controller.objectComplete(false);
    		}else //still on screen check if hit and move
    		{
	    		if(isFiring)
	    		{
	        		if(isHit(mFireX,mFireY))
	        		{
	        			score=1; //HIT!! give the user some points
	        			//view.playSound(DuckThread.SOUND_QUACK);
	        			setState(STATE_HIT);
	         		}
	        	}
	    		
	    		moveIncrementally();
    		}
    	}else if(mState == STATE_HIT)
    	{
    		if(mDuckTopY < mCanvasHeight) //while duck hit, but still on screen then drop
    		{
    			mDuckTopY+=10;
    		}else //once duck dropped off, complete the object
    		{
    			controller.objectComplete(true);
	    		setState(STATE_WAITING);
    		}
    	}
    	
    	return score;
    }
    
    private void setState(int state)
    {
		mState = state;
		mStateStartTime = System.currentTimeMillis();
    }
}
