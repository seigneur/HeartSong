package com.jwetherell.heart_rate_monitor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;



import android.os.Vibrator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.pheelicks.visualizer.*;
import com.pheelicks.visualizer.renderer.*;

/**
 * This class extends Activity to handle a picture preview, process the preview for a red values 
 * and determine a heart beat.
 * 
 */
@SuppressWarnings("unused")
public class HeartRateMonitor extends Activity {
	private static final String TAG = "HeartRateMonitor";
	private static final AtomicBoolean processing = new AtomicBoolean(false);
    private static Boolean Shuffled = false;
	private static SurfaceView preview = null;
	private static SurfaceHolder previewHolder = null;
	private static Camera camera = null;
	private static View image = null;
	private static TextView text = null;
	private static TextView text2 = null;
	private static TextView ticker_text = null;
	private static TextView song = null;
	private static WakeLock wakeLock = null;
	private ProgressBar mProgress;
	
	private static int averageIndex = 0;
	private static final int averageArraySize = 4;
	private static final int[] averageArray = new int[averageArraySize];

	public static enum TYPE { GREEN, RED };
	private static TYPE currentType = TYPE.GREEN;
	public static TYPE getCurrent() {
		return currentType;
	}

	private static int beatsIndex = 0;
	private static final int beatsArraySize = 3;
	private static final int[] beatsArray = new int[beatsArraySize];
	private static double beats = 0;
	private static long startTime = 0;
	private static double beatsAvg = 0;
	private MediaPlayer mp_file;
	private Handler mhandler;
	private static int playing = 0;
	private VisualizerView mVisualizerView;
	PopupWindow popUp;
	TextView tv;
    
	/**
     * {@inheritDoc}
     */
	public void refreshMusic(View view) {
		mp_file = null;
	    // Do something in response to button click
		
	}
	
	public void onClose() {
        finish();
        System.exit(0);
    }
	
	public void showhelp() {
	//	Log.d(TAG, "We help");
		
		AlertDialog adb = new AlertDialog.Builder(this).setTitle(R.string.HelpT).setMessage(R.string.HelpM)
				  .setPositiveButton(R.string.HelpOK,
						   new DialogInterface.OnClickListener() {
						    
						    @Override
						    public void onClick(DialogInterface dialog, int which) {
						     // TODO Auto-generated method stub
						     
						    }
						   }
						    )
						  .show();

//        Log.d(TAG, "We help over");

    }
	public void onCloseHelp(View v)
	{
     popUp.dismiss();
     //this method is no longer required
	}
	
	
	private void addLineRenderer()
	  {
	    Paint linePaint = new Paint();
	    linePaint.setStrokeWidth(1f);
	    linePaint.setAntiAlias(true);
	    linePaint.setColor(Color.argb(88, 0, 128, 255));

	    Paint lineFlashPaint = new Paint();
	    lineFlashPaint.setStrokeWidth(5f);
	    lineFlashPaint.setAntiAlias(true);
	    lineFlashPaint.setColor(Color.argb(188, 255, 255, 255));
	    LineRenderer lineRenderer = new LineRenderer(linePaint, lineFlashPaint, true);
	    mVisualizerView.addRenderer(lineRenderer);
	  }
	
	public void onShuffle(View v) {
		mp_file = null;
		//ticker_text.setVisibility(View.VISIBLE); 
		text.setVisibility(View.VISIBLE);
		text2.setVisibility(View.GONE);
		image.setVisibility(View.VISIBLE);
		
		mProgress.setVisibility(View.VISIBLE);
		if (Shuffled == true)
       {   
    		   Shuffled=false;
    		   }
      	   else
    			   Shuffled=true;
    }

	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.menu, menu);
	    return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	        case R.id.exit:
	        	onClose();
	        case R.id.help:
	             showhelp();
	        	return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		preview = (SurfaceView)findViewById(R.id.preview);
		previewHolder = preview.getHolder();
		previewHolder.addCallback(surfaceCallback);
		previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		mProgress = (ProgressBar) findViewById(R.id.progress);
		image = findViewById(R.id.image);
		text = (TextView) findViewById(R.id.text);
		text2 = (TextView) findViewById(R.id.text2);

		text2.setVisibility(View.GONE);

		
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "DoNotDimScreen");
        mhandler = new Handler();
        mhandler.postDelayed(timer, 1000);
        Button button1 = (Button)findViewById(R.id.Button03);
        button1.setOnTouchListener(new View.OnTouchListener() {         
            Vibrator vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE) ;
      
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			vibe.vibrate(50);
			return false;
		}
        });
       
        
		//help
              /*
        TextView myText = (TextView) findViewById(R.id.Helptext);
		myText.setText("Hold the phone with that ");
                Log.d(TAG, "We Found mylist and created the adapter");
    */
        //help ends
        
	}
    
    /**
     * {@inheritDoc}
     */
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}
    
    /**
     * {@inheritDoc}
     */
	@Override
	public void onResume() {
		super.onResume();

		wakeLock.acquire();
		
		camera = Camera.open();
		
		startTime = System.currentTimeMillis();
	}
    
    /**
     * {@inheritDoc}
     */
	@Override
	public void onPause() {
		super.onPause();

		wakeLock.release();
		
		camera.setPreviewCallback(null);
		camera.stopPreview();
		camera.release();
		camera = null;
	}
	
	final Runnable timer = new Runnable()
	{
	    public void run() 
	    {
	        mediaPlay();
	        mhandler.postDelayed(this, 10000);
	    }
	};

	private static PreviewCallback previewCallback = new PreviewCallback() {
	    /**
	     * {@inheritDoc}
	     */
		@Override
		public void onPreviewFrame(byte[] data, Camera cam) {
			if (data == null) throw new NullPointerException();
			Camera.Size size = cam.getParameters().getPreviewSize();
			if (size == null) throw new NullPointerException();

			if (!processing.compareAndSet(false, true)) return;
			
			int width = size.width;
			int height = size.height;

			int imgAvg = ImageProcessing.decodeYUV420SPtoRedAvg(data.clone(), height, width);
			//Log.i(TAG, "imgAvg="+imgAvg);
			if (imgAvg==0 || imgAvg==255) {
				processing.set(false);
				return;
			}
			
			int averageArrayAvg=0;
			int averageArrayCnt=0;
			for (int i=0; i<averageArray.length; i++) {
				if (averageArray[i]>0) {
					averageArrayAvg += averageArray[i];
					averageArrayCnt++;
				}
			}

			int rollingAverage = (averageArrayCnt>0)?(averageArrayAvg/averageArrayCnt):0;
			TYPE newType = currentType;
			if (imgAvg<rollingAverage) {
				newType = TYPE.RED;
				if (newType!=currentType) {
					beats++;
					//Log.d(TAG, "BEAT!! beats="+beats);
				}
			} else if (imgAvg>rollingAverage) {
				newType = TYPE.GREEN;
			}
			
			if (averageIndex==averageArraySize) averageIndex = 0;
			averageArray[averageIndex] = imgAvg;
			averageIndex++;
			
			//Transitioned from one state to another to the same
			if (newType!=currentType) {
				currentType=newType;
				image.postInvalidate();
			}
			
			long endTime = System.currentTimeMillis();
			double totalTimeInSecs = (endTime-startTime)/1000d;
			//double tickerTime = totalTimeInSecs * 1000;
			//startTicker(tickerTime);
			
			if (totalTimeInSecs>=10) {
				double bps = (beats/totalTimeInSecs);
				int dpm = (int)(bps*60d);
				if (dpm<30 || dpm>180) {
					startTime = System.currentTimeMillis();
					beats = 0;
					processing.set(false);
					return;
				}
				
				//Log.d(TAG, "totalTimeInSecs="+totalTimeInSecs+" beats="+beats);

				if (beatsIndex==beatsArraySize) beatsIndex = 0;
				beatsArray[beatsIndex] = dpm;
				beatsIndex++;
				
				int beatsArrayAvg=0;
				int beatsArrayCnt=0;
				for (int i=0; i<beatsArray.length; i++) {
					if (beatsArray[i]>0) {
						beatsArrayAvg += beatsArray[i];
						beatsArrayCnt++;
					}
				}
				beatsAvg = (beatsArrayAvg/beatsArrayCnt);
				
		
				text.setText(String.valueOf(beatsAvg));
				startTime = System.currentTimeMillis();
				beats = 0;
			}
			processing.set(false);
		}
	};

	
public void mediaPlay() { 
	
	Uri a;
	Log.d(TAG, "We are Outside beatsAvg:"+beatsAvg);
	//Log.d(TAG, "We are playing?:"+mp_file.isPlaying());
	String url= "http://www.thighswideshut.org/music/Up_The_River.mp3";
	if (beatsAvg > 100) 
	{
		
		if (mp_file == null) {
			 //int heartsong;
			Log.d(TAG, "We are Inside if 1");
			if(Shuffled == true)
			{
			url = "http://173.192.225.172:12028";//"http://usa8-vn.mixstream.net:8138";//"http://www.thighswideshut.org/music/jamesbond_Moby.mp3";
			}
			else
			{
				url = "http://91.121.72.50:9106";
			}
			mp_file = new MediaPlayer();
			mp_file.setAudioStreamType(AudioManager.STREAM_MUSIC); 
			
			try
			{
			 mp_file.setDataSource(url);
			 mp_file.prepare(); // might take long! (for buffering, etc)
			}
			catch(Exception e)
			{
				Log.e(getClass().getName(), "We are getting an Error.", e);            		
		    }
			
				mp_file.start();
				CreateVisualization(mp_file); // Calling the create visualization function
				   
		   }
				else if(mp_file.isPlaying())
				{
					Log.d(TAG, "We are letting it play");
				}		else 
		{
			Log.d(TAG, "We are in e:"+mp_file.isPlaying());
			mp_file = null;
		}
		
	}
	else if (beatsAvg > 80) 
	{
		
		if (mp_file == null) {
			 //int heartsong;
			Log.d(TAG, "We are Inside 80");
			if(Shuffled == true)
			{
			 url = "http://199.19.105.215:8130";//"http://usa8-vn.mixstream.net:8138";//"http://www.thighswideshut.org/music/disney_time.mp3";
			}
			else
			{
			url = "http://76.191.96.169:8008";
			}
			 mp_file = new MediaPlayer();
			mp_file.setAudioStreamType(AudioManager.STREAM_MUSIC); 
			
			try
			{
			 mp_file.setDataSource(url);
			 mp_file.prepare(); // might take long! (for buffering, etc)
			}
			catch(Exception e)
			{
				Log.e(getClass().getName(), "W ,Error starting to stream audio.", e);            		
		    }
			
				mp_file.start();
				CreateVisualization(mp_file); // Calling the create visualization function
				Log.d(TAG, "Music:We are in the greater than 100 part1:"+playing);		
		   }
				else if(mp_file.isPlaying())
				{
					Log.d(TAG, "We are letting it play");
				}		else 
		{
			Log.d(TAG, "We are in e:"+mp_file.isPlaying());
			mp_file = null;
		}
		
	}
	else if (beatsAvg > 60) 
	{
		
		if (mp_file == null) {
			 //int heartsong;
			Log.d(TAG, "We are Inside 60");
			if(Shuffled == true)
			{
			 url = "http://67.212.233.124:8008";//"http://usa8-vn.mixstream.net:8138";//"http://www.thighswideshut.org/music/brother3.mp3";
			}
			else
			{
				url = "http://208.43.42.27:10360";
			}
			 mp_file = new MediaPlayer();
			mp_file.setAudioStreamType(AudioManager.STREAM_MUSIC); 
			
			try
			{
			 mp_file.setDataSource(url);
			 mp_file.prepare(); // might take long! (for buffering, etc)
			}
			catch(Exception e)
			{
				Log.e(getClass().getName(), "We Error starting to stream audio.", e);            		
		    }
			
				mp_file.start();
				CreateVisualization(mp_file); // Calling the create visualization function
				Log.d(TAG, "Music:We are in the greater than 100 part1:"+playing);		
		   }
				else if(mp_file.isPlaying())
				{
					Log.d(TAG, "We are letting it play");
				}		else 
		{
			Log.d(TAG, "We are in e:"+mp_file.isPlaying());
			mp_file = null;
		}
		
	}
	
	
		
}
//function to create the visualization
private void CreateVisualization(MediaPlayer mpfile)
{
	 Log.d(TAG, "We are Adding the visualizer");
	 mVisualizerView = (VisualizerView) findViewById(R.id.visualizerView);
	 Log.d(TAG, "We found the visualizer");
	 mVisualizerView.link(mp_file);
	 Log.d(TAG, "We Added the visualizer");
	 // Start with just line renderer
	    addLineRenderer();
	   // ticker_text.setVisibility(View.GONE); 
	    mProgress.setVisibility(View.GONE);
	    
	    text.setVisibility(View.GONE);
	    text2.setText(text.getText());
	    text2.setVisibility(View.VISIBLE);
	    image.setVisibility(View.GONE);
	    
}

/*private  static void  startTicker(double tickerTime)
{
	ticker_text.setText(String.valueOf(tickerTime).substring(0,1));
	
}*/


	private static SurfaceHolder.Callback surfaceCallback=new SurfaceHolder.Callback() {    
	    /**
	     * {@inheritDoc}
	     */
		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			try {
				camera.setPreviewDisplay(previewHolder);
				camera.setPreviewCallback(previewCallback);
			} catch (Throwable t) {
				Log.e("PreviewDemo-surfaceCallback", "Exception in setPreviewDisplay()", t);
			}
		}
	    
	    /**
	     * {@inheritDoc}
	     */
		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
			Camera.Parameters parameters = camera.getParameters();
			parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
			Camera.Size size = getSmallestPreviewSize(width, height, parameters);
			if (size!=null) {
				parameters.setPreviewSize(size.width, size.height);
				Log.d(TAG, "Using width="+size.width+" height="+size.height);
			}
			camera.setParameters(parameters);
			camera.startPreview();
		}
	    
	    /**
	     * {@inheritDoc}
	     */
		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			// Ignore
		}
	};

	private static Camera.Size getSmallestPreviewSize(int width, int height, Camera.Parameters parameters) {
		Camera.Size result=null;

		for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
			if (size.width<=width && size.height<=height) {
				if (result==null) {
					result=size;
				} else {
					int resultArea=result.width*result.height;
					int newArea=size.width*size.height;

					if (newArea<resultArea) result=size;
				}
			}
		}

		return result;
	}

	
}