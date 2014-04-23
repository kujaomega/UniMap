package com.uni.unimap;

import java.util.List;

import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.util.DisplayMetrics;
import android.util.FloatMath;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout;
import android.widget.Toast;


public class MainActivity extends Activity implements OnTouchListener{
	private static final String TAG = "Touch";
    @SuppressWarnings("unused")
    private static final float MIN_ZOOM = 1f,MAX_ZOOM = 1f;

    // These matrices will be used to scale points of the image
    Matrix matrix = new Matrix();
    Matrix savedMatrix = new Matrix();
    Matrix savedMatrix2 = new Matrix();
    
    // The 3 states (events) which the user is trying to perform
    static final int NONE = 0;
    static final int DRAG = 1;
    static final int ZOOM = 2;
    int mode = NONE;
    public ImageView imageMap;
    RelativeLayout testLayout;

    // these PointF objects are used to record the point(s) the user is touching
    PointF start = new PointF();
    PointF mid = new PointF();
    PointF position = new PointF();
    PointF axisMovement = new PointF();
    PointF acumulatedMovement = new PointF();
    float oldDist = 1f;
    float globalNewDist = 1f;
    float totalOldDist = 0;
    float totalNewDist = 0;
    float totalScale;
    float partialScale;
    boolean makeScale;
    Bitmap scaledBitmap;
    float imageWidth;
	float imageHeight;
	LocationManager gps;
	static double[][] coordenades;
	static public double lat;
	static public double lng;
	String location_string;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.frame);
		
		ImageView uniMapView = new ImageView(getApplicationContext());
		//uniMapView.setImageDrawable(getResources().getDrawable(R.drawable.planta0_1));
		Display display =getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		int width = size.x;
		int height = size.y;
		//int width = (int) getResources().getDimension(R.dimen.image_width);
		//int height = (int) getResources().getDimension(R.dimen.image_height);
		
		scaledBitmap = decodeSampledBitmapFromResource(getResources(), R.drawable.planta0_1, width, height);
		
		Bitmap workingBitmap = Bitmap.createBitmap(scaledBitmap);
        Bitmap mutableBitmap = workingBitmap.copy(Bitmap.Config.ARGB_8888, true);
        scaledBitmap = mutableBitmap;
		uniMapView.setImageBitmap( scaledBitmap);
		
		//RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(width, height);
		//params.addRule(RelativeLayout.CENTER_IN_PARENT);

		//uniMapView.setLayoutParams(params);
		
		//uniMapView.setOnTouchListener(this);
		uniMapView.setOnTouchListener(this);
		relativeLayout.addView(uniMapView);
		acumulatedMovement.x=0;
		acumulatedMovement.y=0;
		totalScale=1;
		partialScale=1;
		mid.x=0;
		mid.y=0;
		//position.x= viewCoords[0];
		//position.y= viewCoords[1];
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId,
	        int reqWidth, int reqHeight) {

	    // First decode with inJustDecodeBounds=true to check dimensions
	    final BitmapFactory.Options options = new BitmapFactory.Options();
	    options.inJustDecodeBounds = false;
	    
	    //copied options
	    options.inDither = true;
	    options.inScaled = true;
	    options.inPreferredConfig = Bitmap.Config.ARGB_8888;// important
	    options.inPurgeable = true;
	    
	    BitmapFactory.decodeResource(res, resId, options);
	    
	   

	    // Calculate inSampleSize
	    options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

	    // Decode bitmap with inSampleSize set
	    options.inJustDecodeBounds = false;
	    return BitmapFactory.decodeResource(res, resId, options);
	}
	
	public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
	    // Raw height and width of image
	    final int height = options.outHeight;
	    final int width = options.outWidth;
	    int inSampleSize = 1;
	
	    if (height > reqHeight || width > reqWidth) {
	
	        final int halfHeight = height / 2;
	        final int halfWidth = width / 2;
	
	        // Calculate the largest inSampleSize value that is a power of 2 and keeps both
	        // height and width larger than the requested height and width.
	        while ((halfHeight / inSampleSize) > reqHeight
	                && (halfWidth / inSampleSize) > reqWidth) {
	            inSampleSize *= 2;
	        }
	    }
	
	    return inSampleSize;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		ImageView view = (ImageView) v;
		
        view.setScaleType(ImageView.ScaleType.MATRIX);
        float scale;
        
        dumpEvent(event);
        // Handle touch events here...
        imageMap = view;
        switch (event.getAction() & MotionEvent.ACTION_MASK) 
        {
            case MotionEvent.ACTION_DOWN:   // first finger down only
                                                savedMatrix.set(matrix);
                                                //Log.d("MATRIx", matrix.toString());
                                                start.set(event.getX(), event.getY());
                                                Log.d(TAG, "mode=DRAG"); // write to LogCat
                                                makeScale=false;
                                                mode = DRAG;
                                                break;

            case MotionEvent.ACTION_UP: // first finger lifted
            									if(makeScale)
            									{
            										totalScale = totalScale*partialScale;
            									}
								            	
								            	//acumulatedMovement.x= acumulatedMovement.x + (start.x - axisMovement.x)/totalScale;
								            	//acumulatedMovement.y= acumulatedMovement.y + (start.y - axisMovement.y)/totalScale;
								            	acumulatedMovement.x= acumulatedMovement.x + (start.x - axisMovement.x);
								            	acumulatedMovement.y= acumulatedMovement.y + (start.y - axisMovement.y);
            									
								            	//position.x= event.getX() - acumulatedMovement.x ;
								            	//position.y= event.getY() - acumulatedMovement.y ;
								            	//position.x= mid.x +(event.getX()-mid.x)/totalScale + acumulatedMovement.x ;
								            	//position.y=  mid.y +(event.getY()-mid.y)/totalScale + acumulatedMovement.y ;
								            	//position.x= event.getX() + acumulatedMovement.x ;
								            	//position.y= event.getY() + acumulatedMovement.y ;
								            	//position.x= mid.x +(event.getX()-mid.x)+ acumulatedMovement.x ;
								            	//position.y=  mid.y +(event.getY()-mid.y)+ acumulatedMovement.y ;
								            	position.x= event.getX();
								            	position.y= event.getY();
								            	long difftime = event.getEventTime()-event.getDownTime();
								            	if (difftime<150)
								            	{
								            		showDialog(event, difftime);
								            	}
								            	else if (difftime>5000) {
													scanDialog();
												}
								            	
								            	break;
            							
           

            case MotionEvent.ACTION_POINTER_UP: // second finger lifted
            				
                                                mode = NONE;
                                                Log.d(TAG, "mode=NONE");
                                                break;

            case MotionEvent.ACTION_POINTER_DOWN: // first and second finger down

                                                oldDist = spacing(event);
                                                Log.d(TAG, "oldDist=" + oldDist);
                                                if (oldDist > 5f) {
                                                    savedMatrix.set(matrix);
                                                   
                                                    midPoint(mid, event);
                                                    Log.d("MIDPOINT", " x: "+mid.x+" y: "+mid.y);
                                                    mode = ZOOM;
                                                    Log.d(TAG, "mode=ZOOM");
                                                }
                                                break;

            case MotionEvent.ACTION_MOVE:

                                                if (mode == DRAG) 
                                                { 
                                                	//int[] viewCoords = new int[2];
                                            		//imageMap.getLocationOnScreen(viewCoords);
                                                	axisMovement.x = event.getX();
                                            		axisMovement.y = event.getY();

                                            		
                                                
                                                    matrix.set(savedMatrix);
                                                    matrix.postTranslate(event.getX() - start.x, event.getY() - start.y); // create the transformation in the matrix  of points
                                                } 
                                                else if (mode == ZOOM) 
                                                { 
                                                    // pinch zooming
                                                    float newDist = spacing(event);
                                                    Log.d(TAG, "newDist=" + newDist);
                                                    if (newDist > 5f) 
                                                    {
                                                        matrix.set(savedMatrix);
                                                        scale = newDist / oldDist; // setting the scaling of the
                                                                                    // matrix...if scale > 1 means
                                                                                    // zoom in...if scale < 1 means
                                                                                    // zoom out
                                                        makeScale=true;
                                                        partialScale=scale;
                                                        matrix.postScale(scale, scale, mid.x, mid.y);
                                                    }
                                                }
                                                break;
        }
        
        view.setImageMatrix(matrix); // display the transformation on screen
        imageMap = view;
        return true; // indicate event was handled
	}
	
	/*
     * --------------------------------------------------------------------------
     * Method: spacing Parameters: MotionEvent Returns: float Description:
     * checks the spacing between the two fingers on touch
     * ----------------------------------------------------
     */

    private float spacing(MotionEvent event) 
    {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return FloatMath.sqrt(x * x + y * y);
    }

    /*
     * --------------------------------------------------------------------------
     * Method: midPoint Parameters: PointF object, MotionEvent Returns: void
     * Description: calculates the midpoint between the two fingers
     * ------------------------------------------------------------
     */

    private void midPoint(PointF point, MotionEvent event) 
    {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }

    /** Show an event in the LogCat view, for debugging */
    private void dumpEvent(MotionEvent event) 
    {
        String names[] = { "DOWN", "UP", "MOVE", "CANCEL", "OUTSIDE","POINTER_DOWN", "POINTER_UP", "7?", "8?", "9?" };
        StringBuilder sb = new StringBuilder();
        int action = event.getAction();
        int actionCode = action & MotionEvent.ACTION_MASK;
        sb.append("event ACTION_").append(names[actionCode]);

        if (actionCode == MotionEvent.ACTION_POINTER_DOWN || actionCode == MotionEvent.ACTION_POINTER_UP) 
        {
            sb.append("(pid ").append(action >> MotionEvent.ACTION_POINTER_ID_SHIFT);
            sb.append(")");
        }

        sb.append("[");
        for (int i = 0; i < event.getPointerCount(); i++) 
        {
            sb.append("#").append(i);
            sb.append("(pid ").append(event.getPointerId(i));
            sb.append(")=").append((int) event.getX(i));
            sb.append(",").append((int) event.getY(i));
            if (i + 1 < event.getPointerCount())
                sb.append(";");
        }

        sb.append("]");
        Log.d("Touch Events ---------", sb.toString());
    }

    /**
     * Shows a dialog with the coordinates
     * @param event
     * @param time
     */
    public void showDialog(MotionEvent event, long time)
    {
    	RectF r = new RectF(); 
    	matrix.mapRect(r);
    	Log.i(TAG, "Rect " + r.left + " " + r.top + " " + r.right + " " + r.bottom + " " +r.centerX()+" "+r.centerY() + " ");
    	/*
    	float[] values = new float[9];
    	matrix.getValues(values);
    	float globalX = values[Matrix.MTRANS_X];
    	float globalY = values[Matrix.MTRANS_Y];
    	float width = values[Matrix.MSCALE_X]*imageWidth;
    	float height = values[Matrix.MSCALE_Y]*imageHeight;
    	*/
    	//int[] viewCoords = new int[2];
		//imageMap.getLocationOnScreen(viewCoords);
    	//float xf = position.x+viewCoords[0];
    	//float yf = position.y+viewCoords[1];
		float scaledX = (position.x - r.left);
		float scaledY = (position.y - r.top);

		scaledX /= totalScale;
		scaledY /= totalScale;
		
    	drawCircle(scaledX, scaledY);
    	
    	AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle("Showed position on image");
        //alertDialog.setMessage("X: "+event.getRawX()+" Y: "+event.getRawY()+"   coord1: "+xf+" coord2: "+yf+" diferenciax: "+acumulatedMovement.x+" diferenciay: "+acumulatedMovement.y+ " scale: "+totalScale);
        alertDialog.setMessage(" X: "+scaledX+" Y: "+scaledY);
        //alertDialog.setMessage("r.left: "+r.left+" r.top: "+r.top+" position.x: "+position.x +" position.y: "+position.y);
        alertDialog.show();	
    }
    
    public void scanDialog()
    {

		
    	AlertDialog alertDialog = new AlertDialog.Builder(this).create();
    	alertDialog.setTitle("Last location");
    	//alertDialog.set
        alertDialog.setMessage(location_string);
    	alertDialog.setButton(Dialog.BUTTON_POSITIVE, "Scan", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				try
				{
		    		gps= (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		    		boolean enabled = gps.isProviderEnabled(LocationManager.GPS_PROVIDER);
		    		Criteria criteria = new Criteria();
		    		//String provider = gps.getBestProvider(criteria, false);
		    		String provider = "gps";
		    		//Location location = gps.getLastKnownLocation("network");
		    		//gps.requestLocationUpdates(location.getProvider(), 400, 1,gps);
		    		//location.
		    		//List<String> providers = gps.getAllProviders();
		    		//providers.remove(0);
		    		//providers.remove(1);
		    		//coordenades = new double [providers.size()][2];
		    		coordenades = new double [0][2];
		    		//providers.removeAll(Collections.singleton(null));
		    		//location_string = providers.toString();
		    		
		    		LocationListener locationListener = new LocationListener() {
		    		    public void onLocationChanged(Location location) {
		    		      // Called when a new location is found by the network location provider.
		    		    	 lat =  location.getLatitude();
		    		    	 lng = location.getLongitude();
		    		    	    
		    		    	    
		    		    }

		    		    public void onStatusChanged(String provider, int status, Bundle extras) {}

		    		    public void onProviderEnabled(String provider) {}

		    		    public void onProviderDisabled(String provider) {}
		    		  };
		    		


		    		//for( int i=0; i<=(providers.size()-1);i++)
		    		//{
		    			try
		    			{
		    				//Vtext2.append("H1\n");
		    				gps.requestLocationUpdates(provider, 400, 1, locationListener);
		    				
		    				Location location = gps.getLastKnownLocation(provider);
		    				//Vtext2.append("H2\n"+location.getLatitude());
		    				
		    				location_string=" \n El proveidor es: "+location.getProvider()
		    				+"\n Latitud: "+location.getLatitude()
		    				+"\n Longitud: "+location.getLongitude()
		    				+"\n Altitud: "+location.getAltitude()
		    				+"\n Precisió: "+location.getAccuracy()+ " m"
		    				
		    				//+"\n to string:"+location.toString()
		    				//+"\n \n gps activat?"+enabled
		    				+"\n ultima Lat: "+String.valueOf(lat)
		    				+"\n ultima Lng: "+String.valueOf(lng)+"\n"
		    				+"\n distancia al últim: "+getDistance(lat, lng, location.getLatitude(), location.getLongitude())+" m"
		    				;
		    				lat= location.getLatitude();
		    				lng = location.getLongitude();
		    				//coordenades[i][0]=location.getLatitude();
		    				//coordenades[i][1]=location.getLongitude();
		    				coordenades[0][0]=location.getLatitude();
		    				coordenades[0][1]=location.getLongitude();
		    				//location_string = "Proveidor "+i+" ("+providers.get(i)+") : "+location_string;
		    				location_string = "Proveidor ("+provider+") : "+location_string;
		    				Log.d("No error1", location_string);		    				
		    			}
		    			catch (Exception e)
		    			{
		    				//Toast.makeText(this, "El provider: "+providers.get(i)+" no funciona", 1).show();
		    				Log.d("error1", e.getMessage());
		    			}
		        		
		    	
		    		//}
		    	
		    		//Vtext2.setText(location_string);
		    		

		    		
		    		
		    		//WifiManager wifiManager = (WifiManager)context.getSystemService(context.WIFI_MODE_SCAN_ONLY);
		    		//int linkSpeed = wifiManager.getConnectionInfo().getRssi();
		    		Log.d("No error2", location_string);
		    
		    	}
		    	catch ( Exception e)
		    	{
		    		//Toast.makeText(this, "Possible error:"+e.getMessage(), 1).show();
		    		Log.d("error2", e.getMessage());
		    	}
				AlertDialog alertDialog2 = new AlertDialog.Builder(MainActivity.this).create();
		    	alertDialog2.setTitle("Scanned place");
		        alertDialog2.setMessage(location_string);
		        alertDialog2.show();
			}
		});
    	//alertDialog.setMessage(location_string);
        alertDialog.show();	
    }
    
    public void drawCircle (float x, float y)
    {
    	/*
    	BitmapFactory.Options myOptions = new BitmapFactory.Options();
        myOptions.inDither = true;
        myOptions.inScaled = false;
        myOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;// important
        myOptions.inPurgeable = true;
        
       
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.planta0_1,myOptions);
       


        Bitmap workingBitmap = Bitmap.createBitmap(bitmap);
        Bitmap mutableBitmap = workingBitmap.copy(Bitmap.Config.ARGB_8888, true);
		*/
    	
    	BitmapDrawable drawable = (BitmapDrawable) imageMap.getDrawable();
    	Bitmap bitmap = drawable.getBitmap();
    	imageWidth= bitmap.getWidth();
    	imageHeight = bitmap.getHeight();
    	Log.d("BITMAP", " x: "+String.valueOf(bitmap.getWidth())+" y :"+String.valueOf(bitmap.getHeight()));
		 Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.BLUE);
        Canvas canvas = new Canvas(bitmap);
       
        canvas.drawCircle(x, y, 10, paint);
        canvas.drawText("x: "+x+" y:"+y, x+10, y, paint);

        imageMap.setImageBitmap(bitmap);
    }
    
    public void drawallCircle ()
    {
    	/*
    	BitmapFactory.Options myOptions = new BitmapFactory.Options();
        myOptions.inDither = true;
        myOptions.inScaled = false;
        myOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;// important
        myOptions.inPurgeable = true;
        
       
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.planta0_1,myOptions);
       


        Bitmap workingBitmap = Bitmap.createBitmap(bitmap);
        Bitmap mutableBitmap = workingBitmap.copy(Bitmap.Config.ARGB_8888, true);
		*/
    	BitmapDrawable drawable = (BitmapDrawable) imageMap.getDrawable();
    	Bitmap bitmap = drawable.getBitmap();
		Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.BLUE);
        Canvas canvas = new Canvas(bitmap);
        
        for(int j=0; j<=1000; j=j+200)
        {
        	 for(int k=0; k<=1300; k=k+100)
             {
		        	canvas.drawCircle(j, k, 10, paint);
		            canvas.drawText("x: "+j+" y:"+k, j+10, k, paint);
		            Log.d("CircleDrown", "x: "+j+" y:"+k);
             }
        }
      canvas.setMatrix(matrix);

       
       
        imageMap.setImageBitmap(bitmap);
    }
    
    public static int getDistance(double lat_a,double lng_a, double lat_b, double lon_b){
    	  int Radius = 6371000; //Radio de la tierra
    	  double lat1 = lat_a / 1E6;
    	  double lat2 = lat_b / 1E6;
    	  double lon1 = lng_a / 1E6;
    	  double lon2 = lon_b / 1E6;
    	  double dLat = Math.toRadians(lat2-lat1);
    	  double dLon = Math.toRadians(lon2-lon1);
    	  double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon /2) * Math.sin(dLon/2);
    	  double c = 2 * Math.asin(Math.sqrt(a));
    	  return (int) (Radius * c);  

    	 }
    

}
