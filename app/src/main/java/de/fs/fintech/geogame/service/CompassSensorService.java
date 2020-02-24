package de.fs.fintech.geogame.service;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Not yet a service. Refactoring from MapsActivity CompassSensorEventListener
 */

public class CompassSensorService {

    public interface OnCompassChangeListener {
        public void onAzimuthChange(float degrees);
    }

    public static class CompassSensorEventListener implements SensorEventListener {

        float[] gData = new float[3]; // gravity or accelerometer
        float[] mData = new float[3]; // magnetometer
        float[] rMat = new float[9];
        float[] iMat = new float[9];
        float[] orientation = new float[3];

        private SensorManager mSensorManager;
        private Sensor accelerometer;
        private Sensor magnetometer;
        private Sensor gravity;
        private boolean haveAccelerometer = false;
        private boolean haveMagnetometer = false;
        private boolean haveGravity = false;

        OnCompassChangeListener lstnr;

        public CompassSensorEventListener(SensorManager sensorManager,OnCompassChangeListener listener) {
            lstnr=listener;

            // initialize your android device sensor capabilities
            mSensorManager = sensorManager;
            accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            gravity = this.mSensorManager.getDefaultSensor( Sensor.TYPE_GRAVITY );

        }

        public void unregister() {
            // to stop the listener and save battery
            mSensorManager.unregisterListener(this);
        }

        public void register() {
            int delay = SensorManager.SENSOR_DELAY_UI;
            haveAccelerometer = mSensorManager.registerListener(this, accelerometer, delay);
            haveMagnetometer = mSensorManager.registerListener(this, magnetometer, delay);
            haveGravity = mSensorManager.registerListener(this, gravity, delay);
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy ) {}

        @Override
        public void onSensorChanged( SensorEvent event ) {
            float[] data;
            // thank you http://www.codingforandroid.com/2011/01/using-orientation-sensors-simple.html
            switch ( event.sensor.getType() ) {
                case Sensor.TYPE_GRAVITY:
                    gData = lowPass( event.values.clone(), gData );
                    break;
                case Sensor.TYPE_ACCELEROMETER:
                    gData = lowPass( event.values.clone(), gData );
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    mData = lowPass( event.values.clone(), mData );
                    break;
                default: return;
            }
            
            if ( SensorManager.getRotationMatrix( rMat, iMat, gData, mData ) ) {
                float azimuth2 = (float) (Math.toDegrees(SensorManager.getOrientation(rMat, orientation)[0]) + 360) % 360;
                //if(BuildConfig.DEBUG) log.debug("azi="+azimuth2);
                lstnr.onAzimuthChange(azimuth2);
            }
        }
        
        
    }
    
    // http://blog.thomnichols.org/2012/06/smoothing-sensor-data-part-2
    // http://blog.thomnichols.org/2011/08/smoothing-sensor-data-with-a-low-pass-filter
    
    /*
     * time smoothing constant for low-pass filter
     * 0 <= alpha < 1 ; a smaller value basically means more smoothing
     * See: http://en.wikipedia.org/wiki/Low-pass_filter#Discrete-time_realization
     */
    static final float ALPHA = 0.15f;
     
    /**
     * @see http://en.wikipedia.org/wiki/Low-pass_filter#Algorithmic_implementation
     * @see http://developer.android.com/reference/android/hardware/SensorEvent.html#values
     */
    public static float[] lowPass( float[] newValues, float[] oldValues ) {
        if ( oldValues == null ) return newValues;
         
        for ( int i=0; i<newValues.length; i++ ) {
            oldValues[i] = oldValues[i] + ALPHA * (newValues[i] - oldValues[i]);
        }
        return oldValues;
    }

}
