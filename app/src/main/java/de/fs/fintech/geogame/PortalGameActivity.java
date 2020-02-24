package de.fs.fintech.geogame;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.graphics.BitmapFactory.Options;
import android.os.PowerManager.WakeLock;
import android.widget.TextView;

import com.j256.ormlite.logger.Logger;
import com.j256.ormlite.logger.LoggerFactory;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class PortalGameActivity extends AppCompatActivity {
    private static Logger log = LoggerFactory.getLogger(PortalGameActivity.class);
    public static final int REQUEST_GAME_RESULT = 111;
    public static final String KEY_RESULT_SCORE = "%&%&%(/&§$KEY_RESULT_SCORE)/§/§$";

    private SimulationView mSimulationView;
    private Display mDisplay;
    private WakeLock mWakeLock;
    private DisplayMetrics mDisplayMetrics = new DisplayMetrics();
    private final int BOXES_MORE_IN_HEIGHT_THAN_WIDTH = 2;
    private Button mButtonCaptureEarly;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDisplay = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();

        mWakeLock = ((PowerManager) getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, getClass().getName());

        mSimulationView = new SimulationView(this);
        mSimulationView.setBackgroundResource(android.R.color.black);
        setContentView(R.layout.activity_portal_game);

        mDisplay.getMetrics(mDisplayMetrics);

        float displayWidthPx = Orientation.HORIZONTAL.getPixels(mDisplayMetrics);
        float displayHeightPx = Orientation.VERTICAL.getPixels(mDisplayMetrics);
        float boxWidth = displayWidthPx / SimulationView.ParticleSystem.MAX_BOXES_IN_ONE_ROW;

        float viewHeight = boxWidth * (SimulationView.ParticleSystem.MAX_BOXES_IN_ONE_ROW + BOXES_MORE_IN_HEIGHT_THAN_WIDTH);

//        log.info("boxWidth = " + boxWidth + ", viewHeight = " + viewHeight);
//        log.info("displayHeightInch = " + displayHeightInch + ", displayWidthInch = " + displayWidthInch);
        int marginTopBot = (int) ((displayHeightPx - viewHeight)/2.0f);
        log.info("marginTopBot = " + marginTopBot);

        FrameLayout frameLayout = (FrameLayout) findViewById(android.R.id.content);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        params.setMargins(0, marginTopBot, 0, marginTopBot);
        mSimulationView.setLayoutParams(params);
        frameLayout.addView(mSimulationView);

        mButtonCaptureEarly = (Button) findViewById(R.id.button_capture_early);
        mButtonCaptureEarly.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                log.info("Ending because of Early Capute/Button press!");
                Intent i = new Intent();
                i.putExtra(KEY_RESULT_SCORE, mSimulationView.mParticleSystem.mBoxesDestroyed);
                setResult(RESULT_OK, i);
                finish();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();

        mWakeLock.release();
    }

    @Override
    protected void onResume() {
        super.onResume();

        mWakeLock.acquire();
    }

    class SimulationView extends FrameLayout  {
        //diameter of the balls in meters
        private static final float sBallDiameter = 0.002f;

        //Maximum Metrics (?)
        private final int mDstWidth;
        private final int mDstHeight;

        private final int mBoxWidth;

        private long mLastT;

        private float mXDpi;
        private float mYDpi;
        private float mMetersToPixelsX; //transforming ratio
        private float mMetersToPixelsY;
        private float mXOrigin;
        private float mYOrigin;
        private float mHorizontalBound;
        private float mVerticalBound;
        private final ParticleSystem mParticleSystem;

        private boolean mDrawn = false;   //Flag to control if the onDraw Method has already been run. (need this for mVertical- and mHorizontal Bounds)

        class Particle extends View {
            protected float mPosX;
            protected float mPosY;

            public Particle(Context context) { super(context); }

            public Particle(Context context, float aPosX, float aPosY) {
                super(context);
                mPosX = aPosX;
                mPosY = aPosY;
            }

            /*
             * Following constructors are in original Code but are never used
             */

            /*public Particle(Context context, AttributeSet attrs) { super(context, attrs); }

            public Particle(Context context, AttributeSet attrs, int defStyleAttr) {
                super(context, attrs, defStyleAttr);
            }

            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            public Particle(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
                super(context, attrs, defStyleAttr, defStyleRes);
            }*/


        }

        class ShootingParticle extends Particle {
            private boolean mSticksToYBound = true;
            private float mVelX;
            private float mVelY;
            private int mOldIndexHash;
            private static final float VEL_ADJUST = .01f;
            BoxParticle mBoxToBeCheckedAgainst;


            public ShootingParticle(Context context) {
                super(context);
                mPosX = 0;
                mPosY = -mVerticalBound;
                mVelX = 0.1f;
                mVelY = 0.1f;
            }

            public ShootingParticle(Context context, float aPosX, float aPosY) {
                super(context, aPosX, aPosY);
                mVelX = 0;
                mVelY = 0;
            }

            /*
             *  Constructor for creating new Balls that stick to the bottom border with a certain x-position
             */
            public ShootingParticle(Context context, float aPosX) {
                super(context, aPosX, -mVerticalBound);
                mVelX = 0;
                mVelY = 0;
            }

            public void computeNewPosition(float dT) {
                if(mSticksToYBound) return;

                mPosX += mVelX * dT;
                mPosY += mVelY * dT;
            }

            public boolean resolveCollisionWithBounds() {
                final float xmax = mHorizontalBound;
                final float ymax = mVerticalBound;
                final float x = mPosX;
                final float y = mPosY;

                if(mSticksToYBound) return false;

                /*
                 * In Case Particle hits a border revert Velocity in that Direction to
                 * simulate "Einfallswinkel = Ausfallswinkel"
                 * Added a mirroring effect to the borders to stabilize the track of the balls
                 */
                //log.info("posx = " + x + ", posy = " + y + ", XBound = " + xmax + ", YBound = " + ymax);
                if(x >= xmax) {
                    mPosX = mHorizontalBound - (mPosX - mHorizontalBound);
//                    log.info("------------------------------------");
//                    log.info("posx = " + x + "XBound = " + xmax);
                    mVelX = -Math.abs(mVelX);
                }

                if(x <= -xmax) {
                    mPosX = -mHorizontalBound + (mPosX + mHorizontalBound);
//                    log.info("------------------------------------");
//                    log.info("posx = " + x + "XBound = " + -xmax);
                    mVelX = Math.abs(mVelX);
                }

                if(y >= ymax) {
                    mPosY = mVerticalBound - (mPosY - mVerticalBound);
//                    log.info("------------------------------------");
//                    log.info("posy = " + y + "YBound = " + ymax);
                    mVelY = -Math.abs(mVelY);
                }

                if(y <= -ymax && mVelY < 0) {
                    mVelY = 0;
                    mVelX = 0;
                    mPosY = -mVerticalBound;
                    mSticksToYBound = true;
                    mParticleSystem.mParticlesReturned++;

                    //If this is not the first particle returned
                    if(mParticleSystem.mPosFirstReturnX != 0) {
                        /*if(Math.abs(mPosX - mParticleSystem.mPosFirstReturnX) < 0.001f) {
                            mVelX = 0;
                            mPosX = mParticleSystem.mPosFirstReturnX;
                        }
                        else {
                            if(mPosX > mParticleSystem.mPosFirstReturnX) mVelX = -ShootingParticle.VEL_ADJUST;
                            else if(mPosX < mParticleSystem.mPosFirstReturnX) mVelX = ShootingParticle.VEL_ADJUST;
                        }*/

                        mPosX = mParticleSystem.mPosFirstReturnX;
                    }
                    //If this is the first Particle returned
                    else {
                        while(Math.abs(mPosX) >= mHorizontalBound) mPosX+=(mPosX > 0)?0.001f:-0.001f;        //So the particles aren't shot exactly from the x-border
                        mParticleSystem.mPosFirstReturnX = mPosX;

                        ShootingParticle newParticle = new ShootingParticle(getContext(), mParticleSystem.mPosFirstReturnX);
                        newParticle.setBackgroundResource(R.drawable.ball);
                        newParticle.setLayerType(LAYER_TYPE_HARDWARE, null);
                        mParticleSystem.mBalls.add(newParticle);
                        addView(newParticle, new ViewGroup.LayoutParams(mDstWidth, mDstHeight));
                        mParticleSystem.mNumShootingParticles++;
                    }
                    log.info("Particle returned");
                }


                //This Part should make the Balls move slowly to the return position and stay there as soon as they arrive

                /*if(mSticksToYBound && mPosX != mParticleSystem.mPosFirstReturnX) {
                    if(Math.abs(mPosX - mParticleSystem.mPosFirstReturnX) < 0.001f) {
                        mVelX = 0;
                        mPosX = mParticleSystem.mPosFirstReturnX;
                    }
                    else {
                        if(mPosX > mParticleSystem.mPosFirstReturnX) mVelX = -ShootingParticle.VEL_ADJUST;
                        else if(mPosX < mParticleSystem.mPosFirstReturnX) mVelX = ShootingParticle.VEL_ADJUST;
                    }
                }*/


                if(mParticleSystem.mParticlesReturned == mParticleSystem.mNumShootingParticles - 1 && mParticleSystem.mParticlesReturned != 0) { log.info("All returned"); return true; }
                return false;
            }

            public void resolveCollisionWithBoxes() {
                int indexX = Orientation.HORIZONTAL.posToIndex(mPosX, mSimulationView);
                int indexY = Orientation.VERTICAL.posToIndex(mPosY, mSimulationView);
                int newIndexHash = indexX + indexY;

                if(newIndexHash != mOldIndexHash) {
                    mBoxToBeCheckedAgainst = mParticleSystem.onABox(this);
                    log.info("New Index Hash = " + newIndexHash);

                    /*ShootingParticle test = new ShootingParticle(getContext(), mPosX, mPosY);
                    test.setBackgroundResource(R.drawable.ball);
                    test.setLayerType(LAYER_TYPE_HARDWARE, null);
                    addView(test, new ViewGroup.LayoutParams(mDstWidth, mDstHeight));
                    mParticleSystem.mTest.add(test);*/

                    mOldIndexHash = newIndexHash;
                }

                if(mBoxToBeCheckedAgainst != null) {
                    float difX = mPosX - Orientation.HORIZONTAL.indexToPos(indexX, mSimulationView);
                    float difY = mPosY - Orientation.VERTICAL.indexToPos(indexY, mSimulationView);

                    int changeInVelocity[] = new int[2];

                    changeInVelocity[0] = signum(mVelX);
                    changeInVelocity[1] = signum(mVelY);

                    if(Math.abs(difX) > Math.abs(difY)) {
                        changeInVelocity[0] *= -1;
                        float boxWidth = Orientation.HORIZONTAL.pxToMeters(mBoxWidth, mSimulationView);
                        float boxBound = Orientation.HORIZONTAL.indexToPos(indexX, mSimulationView) + signum(difX) * boxWidth / 2;
                        mPosX = boxBound + signum(difX) * (boxWidth / 2 - Math.abs(difX));
                    }
                    else if(Math.abs(difX) < Math.abs(difY)) {
                        changeInVelocity[1] *= -1;
                        float boxWidth = Orientation.VERTICAL.pxToMeters(mBoxWidth, mSimulationView);
                        float boxBound = Orientation.VERTICAL.indexToPos(indexY, mSimulationView) + signum(difY) * boxWidth / 2;
                        mPosY = boxBound + signum(difY) * (boxWidth / 2 - Math.abs(difY));
                    }
                    else {
                        changeInVelocity[0] = -1;
                        changeInVelocity[1] = -1;

                        float boxWidthX = Orientation.HORIZONTAL.pxToMeters(mBoxWidth, mSimulationView);
                        float boxBoundX = Orientation.HORIZONTAL.indexToPos(indexY, mSimulationView) + signum(difX) * boxWidthX / 2;
                        float boxWidthY = Orientation.VERTICAL.pxToMeters(mBoxWidth, mSimulationView);
                        float boxBoundY = Orientation.VERTICAL.indexToPos(indexY, mSimulationView) + signum(difY) * boxWidthY / 2;

                        mPosX = boxBoundX + signum(difX) * (boxWidthX / 2 - Math.abs(difX));
                        mPosY = boxBoundY + signum(difY) * (boxWidthY / 2 - Math.abs(difY));
                    }

                    mVelX = Math.abs(mVelX) * changeInVelocity[0];
                    mVelY = Math.abs(mVelY) * changeInVelocity[1];


                    mBoxToBeCheckedAgainst.mHitsNeeded--;
                    mBoxToBeCheckedAgainst.mHitCounter.setText(Integer.toString(mBoxToBeCheckedAgainst.mHitsNeeded));
                    if(mBoxToBeCheckedAgainst.mHitsNeeded == 0) {
                        log.info("Destroyed Box, in total: " + ++mParticleSystem.mBoxesDestroyed);
                        mButtonCaptureEarly.setText(mParticleSystem.mBoxesDestroyed + " " + getContext().getString(((mParticleSystem.mBoxesDestroyed >= mParticleSystem.DESTROY_BOXES_TO_CAPTURE)?R.string.boxes_destroyed_capture:R.string.boxes_destroyed)));
                        if(mParticleSystem.mBoxesDestroyed == mParticleSystem.DESTROY_BOXES_TO_CAPTURE) mButtonCaptureEarly.setEnabled(true);
                        mParticleSystem.removeBox(mBoxToBeCheckedAgainst);
                    }
                }


            }

            public void shoot(float aVelX, float aVelY) {
                mVelX = aVelX;
                mVelY = aVelY;
                mSticksToYBound = false;
            }
        }

        class BoxParticle extends Particle {
            private int mIndexX;
            private int mIndexY;

            public TextView mHitCounter;

            private int mHitsNeeded;

            BoxParticle(Context context) {
                super(context);
                this.mHitsNeeded = 1;
                this.mPosX = 0;
                this.mPosY = 0;
                setIndexX(0);
                setIndexYOfFirstBoxTo0();

                setBackgroundResource(R.drawable.box);
                setLayerType(LAYER_TYPE_HARDWARE, null);
                mParticleSystem.mBoxes.get(0)[0] = this;
                addView(mParticleSystem.mBoxes.get(0)[0], new ViewGroup.LayoutParams(mBoxWidth, mBoxWidth));

                mHitCounter = new TextView(getContext());
                mHitCounter.setLayerType(LAYER_TYPE_HARDWARE, null);
                mHitCounter.setText(Integer.toString(mHitsNeeded));
                mHitCounter.setGravity(Gravity.CENTER);
                mHitCounter.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
                addView(mHitCounter, new ViewGroup.LayoutParams(mBoxWidth, mBoxWidth));
            }

            BoxParticle(Context context, int aHitsNeeded, int aIndexX) {
                super(context);
                this.mHitsNeeded = aHitsNeeded;
                this.mPosX = Orientation.HORIZONTAL.indexToPos(aIndexX, mSimulationView);
                this.mIndexX = aIndexX;
                this.mIndexY = 0;
                this.mPosY = Orientation.VERTICAL.indexToPos(mIndexY, mSimulationView);

                setBackgroundResource(R.drawable.box);
                setLayerType(LAYER_TYPE_HARDWARE, null);
                mParticleSystem.mBoxes.get(0)[aIndexX] = this;
                addView(mParticleSystem.mBoxes.get(0)[aIndexX], new ViewGroup.LayoutParams(mBoxWidth, mBoxWidth));

                mHitCounter = new TextView(getContext());
                mHitCounter.setLayerType(LAYER_TYPE_HARDWARE, null);
                mHitCounter.setText(Integer.toString(mHitsNeeded));
                mHitCounter.setGravity(Gravity.CENTER);
                mHitCounter.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
                addView(mHitCounter, new ViewGroup.LayoutParams(mBoxWidth, mBoxWidth));

            }

            public BoxParticle setIndexX(int aIndex) {
                mIndexX = aIndex;
                mPosX = Orientation.HORIZONTAL.indexToPos(mIndexX, mSimulationView);

                return this;
            }

            /*
             * Moves the Box one row down
             * returns false if the game is over
             * returns true if the game goes on
             */
            public boolean moveOneRowDown() {
                int newIndex = mIndexY + 1;
                if(newIndex == BOXES_MORE_IN_HEIGHT_THAN_WIDTH + ParticleSystem.MAX_BOXES_IN_ONE_ROW - 2)
                    return false;
                else
                    mParticleSystem.mBoxes.get(mIndexY)[mIndexX] = null;     //Set the Array Value to 0 so there is no Value left in that field


                mIndexY = newIndex;
                mPosY = Orientation.VERTICAL.indexToPos(mIndexY, mSimulationView);

                mParticleSystem.mBoxes.get(mIndexY)[mIndexX] = this;

                return true;
            }

            public void setIndexYOfFirstBoxTo0() {
                mIndexY = 0;
                mPosY = Orientation.VERTICAL.indexToPos(mIndexY, mSimulationView);

                mParticleSystem.mBoxes.get(0)[0] = this;
            }


        }

        class ParticleSystem {
            public static final int MAX_BOXES_IN_ONE_ROW = 5;

            private float mPosFirstReturnX;
            private int mNumShootingParticles = 1;
            private int mParticlesReturned = 0;
            private int mBoxesDestroyed = 0;
            private final int DESTROY_BOXES_TO_CAPTURE = 20;
            private ArrayList<ShootingParticle> mBalls = new ArrayList<>();

            //private ArrayList<ShootingParticle> mTest = new ArrayList<>();

            private ArrayList<BoxParticle[]> mBoxes = new ArrayList<>();


            private boolean mShootable = true;
            private boolean mAfterRoundRoutineDone = true;

            private ShootingParticle mAim;

            ParticleSystem(){
                ShootingParticle newParticle = new ShootingParticle(getContext(), 0);
                newParticle.setBackgroundResource(R.drawable.ball);
                newParticle.setLayerType(LAYER_TYPE_HARDWARE, null);
                mBalls.add(newParticle);
                addView(mBalls.get(0),  new ViewGroup.LayoutParams(mDstWidth, mDstHeight));

                //mBoxHits = new int[MAX_BOXES_IN_ONE_ROW][MAX_BOXES_IN_ONE_ROW + BOXES_MORE_IN_HEIGHT_THAN_WIDTH];
                mBoxes = new ArrayList<BoxParticle[]>();
                for(int i = 0; i < MAX_BOXES_IN_ONE_ROW + BOXES_MORE_IN_HEIGHT_THAN_WIDTH; i++) mBoxes.add(new BoxParticle[MAX_BOXES_IN_ONE_ROW]);
            }

            private void updatePositions(long timestamp) {
                final long t = timestamp;
                if(mLastT != 0) {       //Nothing happens at the first time
                    final float dT = (float) (t - mLastT) / 1000.f;     //(t-mLastT) is the difference in time since the last position update
                    for (int i = 0; i < mBalls.size(); i++)
                        mBalls.get(i).computeNewPosition(dT);
                }
                mLastT = t;
            }

            public void update(long now) {
                updatePositions(now);

                for(int i = 0; i < mBalls.size(); i++) {
                    if(mBalls.get(i).resolveCollisionWithBounds() && !mAfterRoundRoutineDone) {
                        afterRoundRoutine();
                    }
                    mBalls.get(i).resolveCollisionWithBoxes();
                }
            }

            private void afterRoundRoutine() {
                if(!moveAllBoxesDown()) {
                    log.info("GAME OVER! Lost because at least one Box touched the ground");
                    Intent i = new Intent();
                    i.putExtra(KEY_RESULT_SCORE, mBoxesDestroyed);
                    setResult((mBoxesDestroyed >= DESTROY_BOXES_TO_CAPTURE)?RESULT_OK:RESULT_CANCELED, i);
                    finish();
                }

                for(int i = 0; i < MAX_BOXES_IN_ONE_ROW; i++) {
                    if(Math.random() < 0.8)
                        new BoxParticle(getContext(), mNumShootingParticles, i);
                }

                mShootable = true;
                mAfterRoundRoutineDone = true;
            }


            /*
             * Moves all boxes one row down
             * returns false if the game is over
             * returns true if the game can go on
             */
            private boolean moveAllBoxesDown() {
                for(int i = mBoxes.size() - 1; i >= 0; i--)
                    for(int j = 0; j < mBoxes.get(i).length; j++)
                        if(mBoxes.get(i)[j] != null)                                //if there is a box on this field
                            if(!mBoxes.get(i)[j].moveOneRowDown()) return false;    //move the box down and in case any box returns false, also returns false
                return true;
            }

            public void shootParticles(float aVelX, float aVelY) {
                new ShootingThread(mBalls, aVelX, aVelY).start();
            }

            /*
             *  Returns the box (in case there is one) the ball is on
             */
            public BoxParticle onABox(ShootingParticle aBall) {

                int indexX = Orientation.HORIZONTAL.posToIndex(aBall.mPosX, mSimulationView);
                int indexY = Orientation.VERTICAL.posToIndex(aBall.mPosY, mSimulationView);

                if(getBox(indexX, indexY) != null) return getBox(indexX, indexY); else return null;

            }

            /*
            public ArrayList<BoxParticle> getBoxesToCheckAgainst(int aBallIndex) {
                return null;
            }*/

            public int getParticleCount() { return mBalls.size(); }
            public float getPosX(int i) { return mBalls.get(i).mPosX; }
            public float getPosY(int i) { return mBalls.get(i).mPosY; }
            public BoxParticle getBox(int aIndexX, int aIndexY) {
                try {
                    return mBoxes.get(aIndexY)[aIndexX];
                } catch(ArrayIndexOutOfBoundsException aioobe) {
                    return null;
                }

            }
            public void removeBox(BoxParticle aBox) {
                removeView(aBox.mHitCounter);
                removeView(aBox);
                mBoxes.get(aBox.mIndexY)[aBox.mIndexX] = null;
            }

            /*
             *  This thread´s task is to shoot all the Particles with a
             *  certain timedifference
             */
            class ShootingThread extends Thread {
                private final ArrayList<ShootingParticle> mBalls;
                private final long TIME_DIFFERENCE = 40;
                private final float VEL_X;
                private final float VEL_Y;

                ShootingThread(ArrayList<ShootingParticle> aBalls, float aVelX, float aVelY) {
                    VEL_X = aVelX;
                    VEL_Y = aVelY;
                    mBalls = aBalls;
                }

                @Override
                public void run() {
                    for(int i = 0; i < mBalls.size(); i++) {
                        mBalls.get(i).shoot(VEL_X, VEL_Y);
                        try {
                            TimeUnit.MILLISECONDS.sleep(TIME_DIFFERENCE);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    mParticleSystem.mAfterRoundRoutineDone = false;
                }
            }
        }

        public SimulationView(Context context) {
            super(context);

            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            mXDpi = metrics.xdpi;
            mYDpi = metrics.ydpi;
            mMetersToPixelsX = mXDpi / 0.0254f;
            mMetersToPixelsY = mYDpi / 0.0254f;

            mDstWidth = (int) (sBallDiameter * mMetersToPixelsX + 0.5f);
            mDstHeight = (int) (sBallDiameter * mMetersToPixelsY + 0.5f);

            mBoxWidth = (int) Orientation.HORIZONTAL.getPixels(metrics) / ParticleSystem.MAX_BOXES_IN_ONE_ROW;

            mParticleSystem = new ParticleSystem();

            Options opts = new Options();
            opts.inDither = true;
            opts.inPreferredConfig = Bitmap.Config.RGB_565;
        }

        public void shootParticles(float aVelX, float aVelY) {
            mParticleSystem.mShootable = false;
            mParticleSystem.mParticlesReturned = 0;
            mParticleSystem.mPosFirstReturnX = 0;
            mParticleSystem.shootParticles(aVelX, aVelY);
        }

        public float[] calculateVelocity(ShootingParticle aTouch) {
            final float startX = mParticleSystem.getPosX(0);
            final float startY = mParticleSystem.getPosY(0);

            final float touchX = aTouch.mPosX;
            final float touchY = aTouch.mPosY;

            final float difX = touchX - startX;
            final float difY = touchY - startY;

            return calculateVelocity(difX, difY);
        }

        public float[] calculateVelocity(float aDifX, float aDifY) {
            float[] velocity = new float[2];

            double ratio = aDifX/aDifY;
            log.info("Velocity-/positionratio DifX / DifY = "+ ratio);

            velocity[1] = (float) Math.sqrt((1.0 / 50.0) / (1 + Math.pow(ratio,2)));
            velocity[0] = (float) ratio * velocity[1];

            return velocity;
        }

        private void createAim(float posx, float posy) {
            mParticleSystem.mAim = new ShootingParticle(getContext(), posx, posy);
            mParticleSystem.mAim.setBackgroundResource(R.drawable.ball);
            mParticleSystem.mAim.setLayerType(LAYER_TYPE_HARDWARE, null);
            addView(mParticleSystem.mAim, new ViewGroup.LayoutParams(mDstWidth, mDstHeight));
        }

        /*
         * Inherited (Control-) Methods
         */
        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {

            //log.info("onSizeChanged called from Game with w = " + w + " and h = " + h);

            // compute the origin of the screen relative to the origin of
            // the bitmap

            mXOrigin = w * 0.5f;
            mYOrigin = h * 0.5f;
            mHorizontalBound = ((w / mMetersToPixelsX - sBallDiameter) * 0.5f);
            mVerticalBound = ((h / mMetersToPixelsY - sBallDiameter) * 0.5f);

            log.info("onSizeChanged; mHorBound = " + mHorizontalBound + ", mVerBound = " + mVerticalBound);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            final long now = System.currentTimeMillis();

            mParticleSystem.update(now);

            if(!mDrawn) {
                new BoxParticle(getContext());
                mParticleSystem.mBalls.get(0).mPosY = -mVerticalBound;      //set Initial Ball to the middle of the bottom Border and Box to the upper left corner
                                                                            //needed because mVerticalBound is set after the Ball is created
                                                                            //onSizeChanged called after the Constructor of SimulationView


                mDrawn = true;
            }

            for(int i = 0; i < mParticleSystem.getParticleCount(); i++) {
                final float x = Orientation.HORIZONTAL.posToPxForBallView(mParticleSystem.getPosX(i), this);
                final float y = Orientation.VERTICAL.posToPxForBallView(mParticleSystem.getPosY(i), this);
                mParticleSystem.mBalls.get(i).setX(x);
                mParticleSystem.mBalls.get(i).setY(y);
            }

            if(mParticleSystem.mAim != null) {
                final float x = Orientation.HORIZONTAL.posToPxForBallView(mParticleSystem.mAim.mPosX, this);
                final float y = Orientation.VERTICAL.posToPxForBallView(mParticleSystem.mAim.mPosY, this);
                mParticleSystem.mAim.setX(x);
                mParticleSystem.mAim.setY(y);
            }

            if(mParticleSystem.mBoxes == null);
            else
                for(int i = 0; i < mParticleSystem.mBoxes.size(); i++)
                    for(int j = 0; j < mParticleSystem.mBoxes.get(i).length; j++) {
                        try {
                            final BoxParticle box = mParticleSystem.mBoxes.get(i)[j];

                            final float x = Orientation.HORIZONTAL.posToPxForBoxView(box.mPosX, this);
                            final float y = Orientation.VERTICAL.posToPxForBoxView(box.mPosY, this);
                            //log.info("x/y = " + x + " / " + y + "PosX/PosY = " + box.mPosX + " / " + box.mPosY);
                            box.setX(x);
                            box.setY(y);
                            box.mHitCounter.setX(x);
                            box.mHitCounter.setY(y);
                        } catch(NullPointerException npe) {

                        }


                    }


            //For Index Testing
            /*for(int i = 0; i < mParticleSystem.mTest.size(); i++) {
                float x = Orientation.HORIZONTAL.posToPxForBallView(mParticleSystem.mTest.get(i).mPosX, this);
                float y = Orientation.VERTICAL.posToPxForBallView(mParticleSystem.mTest.get(i).mPosY, this);

                mParticleSystem.mTest.get(i).setX(x);
                mParticleSystem.mTest.get(i).setY(y);
            }*/


            invalidate();
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {

            final int action_masked = event.getActionMasked();

            float x, y, posx, posy;

            if(mParticleSystem.mShootable)
                switch(action_masked) {
                    case MotionEvent.ACTION_DOWN:
                        //log.info("Down: x: " + Float.toString(event.getX(0)/mMetersToPixelsX) + ", y: " + Float.toString(event.getY(0)/mMetersToPixelsY) + "/x: " + event.getX(0) + ", y: " + event.getY(0));

                        x = event.getX(0);
                        y = event.getY(0);

                        posx = Orientation.HORIZONTAL.pxToPos(x, this);
                        posy = Orientation.VERTICAL.pxToPos(y, this);

                        createAim(posx, posy);
                        break;

                    case MotionEvent.ACTION_UP:

                        if(mParticleSystem.mAim != null) {
                            float[] velocity = calculateVelocity(mParticleSystem.mAim);
                            shootParticles(velocity[0], velocity[1]);

                            removeView(mParticleSystem.mAim);
                            mParticleSystem.mAim = null;
                        }

                        break;

                    default:
                        //log.info("Move: x: " + Float.toString(event.getX(0)/mMetersToPixelsX) + ", y: " + Float.toString(event.getY(0)/mMetersToPixelsY) + "/x: " + event.getX(0) + ", y: " + event.getY(0));

                        if(mParticleSystem.mAim != null) {
                            x = event.getX(0);
                            y = event.getY(0);

                            posx = Orientation.HORIZONTAL.pxToPos(x, this);
                            posy = Orientation.VERTICAL.pxToPos(y, this);

                            mParticleSystem.mAim.mPosX = posx;
                            mParticleSystem.mAim.mPosY = posy;


                            //log.info("posX = " + posx + ", posY = " + posy);
                        }
                }


            return true;
        }
    }

    public int signum(float f) {
        if(f == 0) return 0;
        else if(f > 0) return 1;
        else return -1;
    }

    /*
     *  Some Methods for conversion between px and pos etc
     *  seperated by Horizontal and Vertical
     */
    enum Orientation {
        HORIZONTAL {
            public float getDPI(DisplayMetrics metrics) { return metrics.xdpi; }
            public float getPixels(DisplayMetrics metrics) { return metrics.widthPixels; }
            public float pxToMeters(float px, SimulationView simulationView) {
                return px / simulationView.mMetersToPixelsX;
            }
            public float metersToPx(float inch, SimulationView simulationView) {
                return inch * simulationView.mMetersToPixelsX;
            }
            public float pxToInch(float px, DisplayMetrics metrics) {
                return px / getDPI(metrics);
            }
            public float inchToPx(float inch, DisplayMetrics metrics) {
                return inch * getDPI(metrics);
            }
            public float posToPx(float aPos, SimulationView simulationView) {
                final float xc = simulationView.mXOrigin;
                final float xs = simulationView.mMetersToPixelsX;

                return xc + aPos * xs;
            }
            public float posToPxForBallView(float aPos, SimulationView simulationView) {
                return posToPx(aPos, simulationView) - (SimulationView.sBallDiameter * simulationView.mMetersToPixelsX) / 2;
            }
            public float posToPxForBoxView(float aPos, SimulationView simulationView) {
                return posToPx(aPos, simulationView) - simulationView.mBoxWidth / 2;
            }
            public float pxToPos(float aPx, SimulationView simulationView) {
                final float xc = simulationView.mXOrigin;
                final float xs = simulationView.mMetersToPixelsX;

                return (aPx - xc) / xs;
            }
            public float indexToPos(int aIndexX, SimulationView simulationView) {
                float px = aIndexX * simulationView.mBoxWidth + simulationView.mBoxWidth / 2;

                return pxToPos(px, simulationView);
            }
            public int posToIndex(float aPos, SimulationView simulationView) {
                float pos = aPos;
                float px = posToPx(pos, simulationView);

                //log.info("PX = " + px + ", mBoxWidth = " + mBoxWidth + ", Pos - Width/2 = " + (px - mBoxWidth / 2) + ", index = " + ((px - mBoxWidth / 2) / mBoxWidth));

                return (int) px / simulationView.mBoxWidth;
            }
        },
        VERTICAL {
            public float getDPI(DisplayMetrics metrics) { return metrics.ydpi; }
            public float getPixels(DisplayMetrics metrics) { return metrics.heightPixels; }
            public float pxToMeters(float px, SimulationView simulationView) {
                return px / simulationView.mMetersToPixelsY;
            }
            public float metersToPx(float inch, SimulationView simulationView) {
                return inch * simulationView.mMetersToPixelsY;
            }
            public float pxToInch(float px, DisplayMetrics metrics) {
                return px / getDPI(metrics);
            }
            public float inchToPx(float inch, DisplayMetrics metrics) {
                return inch * getDPI(metrics);
            }
            public float posToPx(float aPos, SimulationView simulationView) {
                final float yc = simulationView.mYOrigin;
                final float ys = simulationView.mMetersToPixelsY;

                return yc - aPos * ys;
            }
            public float posToPxForBallView(float aPos, SimulationView simulationView) {
                return posToPx(aPos, simulationView)- (SimulationView.sBallDiameter * simulationView.mMetersToPixelsX) / 2;
            }
            public float posToPxForBoxView(float aPos, SimulationView simulationView) {
                return posToPx(aPos, simulationView) - simulationView.mBoxWidth / 2;
            }
            public float pxToPos(float aPx, SimulationView simulationView) {
                final float yc = simulationView.mYOrigin;
                final float ys = simulationView.mMetersToPixelsY;
                return (- (aPx - yc) / ys);
            }
            public float indexToPos(int aIndexX, SimulationView simulationView) {
                float px = aIndexX * simulationView.mBoxWidth + simulationView.mBoxWidth / 2;

                return pxToPos(px, simulationView);
            }
            public int posToIndex(float aPos, SimulationView simulationView) {
                float pos = aPos;
                float px = posToPx(pos, simulationView);

                //log.info("PX = " + px + ", mBoxWidth = " + mBoxWidth + ", Pos - Width/2 = " + (px - mBoxWidth / 2) + ", index = " + ((px - mBoxWidth / 2) / mBoxWidth));

                return (int) px / simulationView.mBoxWidth;
            }
        };

        public float getDPI(DisplayMetrics metrics) { throw new AbstractMethodError(); }
        public float getPixels(DisplayMetrics metrics) { throw new AbstractMethodError(); }
        public float pxToMeters(float px, SimulationView simulationView) { throw new AbstractMethodError(); }
        public float metersToPx(float meters, SimulationView simulationView) { throw new AbstractMethodError(); }
        public float pxToInch(float px, DisplayMetrics metrics) { throw new AbstractMethodError(); }
        public float inchToPx(float inch, DisplayMetrics metrics) { throw new AbstractMethodError(); }
        public float posToPx(float aPos, SimulationView simulationView) { throw new AbstractMethodError(); }
        public float pxToPos(float aPx, SimulationView simulationView) { throw new AbstractMethodError(); }
        public float posToPxForBallView(float aPos, SimulationView simulationView) { throw new AbstractMethodError(); }
        public float posToPxForBoxView(float aPos, SimulationView simulationView) { throw new AbstractMethodError(); }
        public float indexToPos(int aIndexX, SimulationView simulationView) { throw new AbstractMethodError(); }
        public int posToIndex(float aPos, SimulationView simulationView) { throw new AbstractMethodError(); }
    }
}
