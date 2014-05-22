package com.uni.unimap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.List;


import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
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
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ListView;
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
	static double testlat;
	static double testlong;
	float imageX;
	float imageY;
	boolean makePoint;
	String place="";
	List<ScanResult> results;
	String scanResults;
	private static final String SAMPLE_DB_NAME = "WifiScans";

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
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	        case R.id.export_db:
	            exportDB();
	            return true;
	        case R.id.delete_db:
	            deleteDB();
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
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
		imageX=scaledX;
		imageY=scaledY;
		setLatLong(scaledX, scaledY);
    	
    	
    	final AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle("Showed position on image");
        
        //alertDialog.setMessage(" X: "+scaledX+" Y: "+scaledY);
        alertDialog.setMessage(" Latitud: "+testlat+" Longitud: "+testlong);
        //alertDialog.setMessage("r.left: "+r.left+" r.top: "+r.top+" position.x: "+position.x +" position.y: "+position.y);
		alertDialog.setButton(Dialog.BUTTON_NEUTRAL, "Test", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						String label = "Test point";
						String uriBegin = "geo:" + testlat + "," + testlong;
						String query = testlat + "," + testlong + "(" + label + ")";
						String encodedQuery = Uri.encode(query);
						String uriString = uriBegin + "?q=" + encodedQuery + "&z=22";
						Uri uri = Uri.parse(uriString);
						Intent intent = new Intent(android.content.Intent.ACTION_VIEW, uri);
						startActivity(intent);
					}
		});
		alertDialog.setButton(Dialog.BUTTON_POSITIVE, "Scan", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
	    		try
	    		{    		
	        		String connectivity_context = Context.WIFI_SERVICE;
	        		WifiManager wifi = (WifiManager)getSystemService(connectivity_context);
	        		//WifiInfo text_estatic = wifi.getConnectionInfo();
	        		String text="";
	        		wifi.startScan();
	        		results= wifi.getScanResults();
	        		scanResults="";
	        		for (ScanResult result : results)
	        		{
	        					scanResults = scanResults+"SSID: "+result.SSID
	        					//+" \n Capabilities: "+result.capabilities
	        					+" \n Frequencia: "+result.frequency
	        					+" \n Potencia: "+result.level
	        					+" \n BSSID: "+result.BSSID
	        					//+" \n Timestamp: "
	        					//+" \n Contents: "+result.describeContents()
	        					//+" \n Codi hash: "+result.hashCode()
	        					//+" \n To String: "+result.toString()
	        					+"\n\n";
	        		}   
	        		
	    		}
	    		catch(Exception e)
	    		{
	    			Log.d("WIFISCAN", e.getMessage());
	    		}
        		final AlertDialog.Builder alertDialog2 = new AlertDialog.Builder(MainActivity.this);
			    	alertDialog2.setTitle(place);
			        alertDialog2.setMessage(scanResults);
			        alertDialog2.setPositiveButton("GUARDAR", new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							
							createDB(testlat, testlong, results, place);
							drawCircle(imageX, imageY);
							
						}
					});
			        alertDialog2.setNeutralButton("Set Place", new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							
							final CharSequence[] items = {" Aula 101 "," Aula 102 "," Aula 103 "," Aula 104 "};
					           
			                // Creating and Building the Dialog
			                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
			                builder.setTitle("Selecciona Lugar");
			                builder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
			                public void onClick(DialogInterface dialog, int item) {
			                  
			                   
			                    switch(item)
			                    {
			                        case 0:
			                                // Your code when first option seletced
			                        		place=String.valueOf(items[0]);
			                                 break;
			                        case 1:
			                                // Your code when 2nd  option seletced
			                        	place=String.valueOf(items[1]);
			                                break;
			                        case 2:
			                               // Your code when 3rd option seletced
			                        	place=String.valueOf(items[2]);
			                                break;
			                        case 3:
			                                 // Your code when 4th  option seletced  
			                        	place=String.valueOf(items[3]);
			                                break;
			                       
			                    }
			                       
			                    }
			                });
			                builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
								
								@Override
								public void onClick(DialogInterface dialog, int which) {
									// TODO Auto-generated method stub
									//ListView lw = ((AlertDialog)dialog).getListView();
									//Object checkedItem = lw.getAdapter().getItem(lw.getCheckedItemPosition());
									alertDialog2.setTitle(place);
									alertDialog2.show();
									
								}
							});
			              builder.create();
			              builder.show();
						}
			        });
			        alertDialog2.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							Toast.makeText(MainActivity.this, "Cancelado", Toast.LENGTH_SHORT).show();
							
						}
					});
			        alertDialog2.create();
			        alertDialog2.show();	
			}
});
		alertDialog.setButton(Dialog.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				Toast.makeText(MainActivity.this, "Cancelado", Toast.LENGTH_SHORT).show();
				
			}
		});
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
		    				+"\n Precisi�: "+location.getAccuracy()+ " m"
		    				
		    				//+"\n to string:"+location.toString()
		    				//+"\n \n gps activat?"+enabled
		    				+"\n ultima Lat: "+String.valueOf(lat)
		    				+"\n ultima Lng: "+String.valueOf(lng)+"\n"
		    				+"\n distancia al �ltim: "+getDistance(lat, lng, location.getLatitude(), location.getLongitude())+" m"
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
    
	public static double getDistance(double lat_a,double lon_a, double lat_b, double lon_b){
	    	  double Radius = (double) 6371.0; //Radio de la tierra
	    	  
	    	  double lat1 = lat_a;
	    	  double lat2 = lat_b;
	    	  double lon1 = lon_a;
	    	  double lon2 = lon_b;
	    	  double dLat = Math.toRadians(lat2-lat1);
	    	  double dLon = Math.toRadians(lon2-lon1);
	    	  double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon /2) * Math.sin(dLon/2);
	    	  double c = 2 * Math.asin(Math.sqrt(a));
	    	  return (double) (Radius * c)*1000;

	    	 }
    
    public void setLatLong(double pointx, double pointy)
    {
    	/*
    	double latx=41.403525131186775;
    	double longy=2.193743782592934;
    	double posx=344.041;
    	double posy=25.522142;
    	double x11=3.9708213398457424438835793492790875820565961926048041621*1E-7;
    	double x21=5.3337852255565321029706524878788673859822851423211338249*1E-7;
    	double x12=3.8463383347614717403469501958589815332960268606603245663*1E-7;
    	double x22=5.3929007815571954859168065659391904495429957097556426039*1E-7;
    	pointx=pointx-posx;
    	pointy=pointy-posy;
    	pointx=pointx*x11+pointy*x21;
    	pointy=pointx*x12+pointy*x22;
    	testlat=latx+pointx;
    	testlong=longy+pointy;
    	*/
    	double x1=41.403525131186775;
    	double y1=2.193743782592934;
    	double x2=41.40418497822078;
    	double y2=2.1946689455078117;
    	double x3=41.404028558630785;
    	double y3=2.194457050995636;
    	double x4=41.404229238358326;
    	double y4=2.1941874889896384;
    	double x21=(Math.abs(x1-x2))/1715.529158;
		double x22=(Math.abs(y1-y2))/1715.529158;
		double x11=(Math.abs(x3-x4))/505.3859399999;
		double x12=(Math.abs(y3-y4))/505.3859399999;
		double basex=344.041;
		double basey=25.522142;
		//849.42694, 1337.6772
		//double basex=344.041;
		//double basey=1337.6772;
		pointx=pointx-basex;
		pointy=pointy-basey;
		Log.d("punts", "x: "+pointx+" y: "+pointy);
    	testlat=pointx*x11+pointy*x21;
    	testlong=-pointx*x12+pointy*x22;
    	testlat=x1+testlat;
    	testlong=y1+testlong;
    	
    }
    
    private void createDB(double latitude, double longitude, List<ScanResult> scanList, String location) {
    	int floor=1;
    	SQLiteDatabase sampleDB =  this.openOrCreateDatabase(SAMPLE_DB_NAME, MODE_PRIVATE, null);
    	for (ScanResult result : results)
		{
    		String tableName=result.BSSID.replace(":","");
    		
    		sampleDB.execSQL(
    				"CREATE TABLE IF NOT EXISTS A" +tableName 
                    + " (ID INTEGER PRIMARY KEY   AUTOINCREMENT"
                    + ", Latitude DOUBLE"
                    + ", Longitude DOUBLE"
                    + ", Frequency INTEGER "
                    + ", MapaX DOUBLE"
                    + ", MapaY DOUBLE"
                    + ", Power INTEGER"
                    + ", Location VARCHAR"
                    + ", Floor INTEGER"
                    +");");
            sampleDB.execSQL(
            		"INSERT INTO A" + tableName +"(Latitude, Longitude, Frequency, MapaX, MapaY, Power, Location, Floor) "
                    + " Values ('"+ latitude +"','"+ longitude +"','" + result.frequency +"','"+ imageX+"','"+ imageY+"','"+ result.level+"','"+location+"', '"+floor+"' "
                    +");");
            Toast.makeText(this, "Table: "+tableName+" Created", Toast.LENGTH_LONG).show(); 
            
		}
    	sampleDB.close();
        sampleDB.getPath();
        Toast.makeText(this, "DB Created @ "+sampleDB.getPath(), Toast.LENGTH_LONG).show(); 
    	
	}
    
    private void exportDB(){
		File sd = Environment.getExternalStorageDirectory();
        File data = Environment.getDataDirectory();
        FileChannel source=null;
        FileChannel destination=null;
        String currentDBPath = "/data/"+ "com.uni.unimap" +"/databases/"+SAMPLE_DB_NAME;
        String backupDBPath = "/Backup/"+SAMPLE_DB_NAME;
        File currentDB = new File(data, currentDBPath);
        File backupDB = new File(sd, backupDBPath);
        try {
            source = new FileInputStream(currentDB).getChannel();
            destination = new FileOutputStream(backupDB).getChannel();
            destination.transferFrom(source, 0, source.size());
            source.close();
            destination.close();
            Toast.makeText(this, "DB Exported!", Toast.LENGTH_LONG).show();
        } catch(IOException e) {
        	e.printStackTrace();
        }
	}
    private void deleteDB(){
		boolean result = this.deleteDatabase(SAMPLE_DB_NAME);
		if (result==true) {
			 Toast.makeText(this, "DB Deleted!", Toast.LENGTH_LONG).show();
		} 
	}
}
