package nl.tblid.login;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.content.Intent;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.common.config.GservicesValue;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.*;

//import java.awt.Button;



public class MainActivity extends AppCompatActivity implements View.OnClickListener  {

    SurfaceView surfaceView;
    CameraSource cameraSource;
    TextView textView;
    TextView textView2;
    TextView textView3;
    TextView resultView;
    BarcodeDetector barcodeDetector;
    Camera.Parameters params;
    Camera camera;
    boolean isFlash = false;
    ProgressDialog pd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        surfaceView= (SurfaceView)findViewById(R.id.camerapreview);
        textView=(TextView)findViewById(R.id.textView);
        resultView = (TextView) findViewById(R.id.textView2);
        textView3=(TextView)findViewById(R.id.textView3);

        barcodeDetector = new BarcodeDetector.Builder(this).setBarcodeFormats(Barcode.QR_CODE).build();

        cameraSource = new CameraSource.Builder(this,barcodeDetector).setRequestedPreviewSize(640,480).setAutoFocusEnabled(true).build() ;

        RSA rsa = new RSA();

        rsa.initialize();

        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                int requestCode=0;
                if(ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != (PackageManager.PERMISSION_GRANTED)){
                    ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.CAMERA},requestCode);
                }
                try {
                    cameraSource.start(holder);
                    initCameraFocusListener();
                }catch (IOException e){
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                cameraSource.stop();
            }
        });

        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {

            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                final SparseArray<Barcode> qrCodes= detections.getDetectedItems();
                if(qrCodes.size()!=0){
                    textView.post(new Runnable() {
                        @Override
                        public void run() {
                            cameraSource.stop();
                            Vibrator vibrator = (Vibrator)getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                            vibrator.vibrate(500);
                            textView.setText(qrCodes.valueAt(0).displayValue);

//                            new JsonTask().execute(qrCodes.valueAt(0).displayValue);

                            DNS dns = new DNS();
                         //   String a = dns.getDnsRecord(qrCodes.valueAt(0).displayValue);
                            String a = dns.getDnsRecord("tblid.nl");
                            //textView3->setText(a);
//                            DirContext ictx = new InitialDirContext();
  //                          Attributes attrs3 = ictx.getAttributes("dns://server1.example.com/host3.example.com",
    //                                new String[] {"MX"});

                        }
                    });
                }
            }
        });


        initViews();



    }

    private void initViews() {
    }

    @Override
    public void onClick(View v) {
        int a=0;
        a++;

    }

    private void initCameraFocusListener() {
        surfaceView.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                cameraFocus(event, cameraSource, Camera.Parameters.FOCUS_MODE_AUTO);

                return false;
            }
        });
    }

    private boolean cameraFocus(MotionEvent event, @NonNull CameraSource cameraSource, @NonNull String focusMode) {
        Field[] declaredFields = CameraSource.class.getDeclaredFields();

        int pointerId = event.getPointerId(0);
        int pointerIndex = event.findPointerIndex(pointerId);
        // Get the pointer's current position
        float x = event.getX(pointerIndex);
        float y = event.getY(pointerIndex);

        float touchMajor = event.getTouchMajor();
        float touchMinor = event.getTouchMinor();

        Rect touchRect = new Rect((int)(x - touchMajor / 2), (int)(y - touchMinor / 2), (int)(x + touchMajor / 2), (int)(y + touchMinor / 2));

        Rect focusArea = new Rect();

        focusArea.set(touchRect.left * 2000 / surfaceView.getWidth() - 1000,
                touchRect.top * 2000 / surfaceView.getHeight() - 1000,
                touchRect.right * 2000 / surfaceView.getWidth() - 1000,
                touchRect.bottom * 2000 / surfaceView.getHeight() - 1000);

        // Submit focus area to camera

        ArrayList<Camera.Area> focusAreas = new ArrayList<Camera.Area>();
        focusAreas.add(new Camera.Area(focusArea, 1000));

        for (Field field : declaredFields) {
            if (field.getType() == Camera.class) {
                field.setAccessible(true);
                try {
                    camera = (Camera) field.get(cameraSource);
                    if (camera != null) {
                        params = camera.getParameters();
                        params.setFocusMode(focusMode);
                        params.setFocusAreas(focusAreas);
                        camera.setParameters(params);

                        // Start the autofocus operation

                        camera.autoFocus(new Camera.AutoFocusCallback() {
                            @Override
                            public void onAutoFocus(boolean b, Camera camera) {
                                // currently set to auto-focus on single touch
                            }
                        });
                        return true;
                    }

                    return false;
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }

                break;
            }
        }
        return false;
    }


    private class JsonTask extends AsyncTask<String, String, String> {

        protected void onPreExecute() {
            super.onPreExecute();

            pd = new ProgressDialog(MainActivity.this);
            pd.setMessage("Please wait");
            pd.setCancelable(false);
            pd.show();
        }

        protected String doInBackground(String... params) {


            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try {
                URL url = new URL(params[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();


                InputStream stream = connection.getInputStream();

                reader = new BufferedReader(new InputStreamReader(stream));

                StringBuffer buffer = new StringBuffer();
                String line = "";

                while ((line = reader.readLine()) != null) {
                    buffer.append(line+"\n");
                    Log.d("Response: ", "> " + line);   //here u ll get whole response...... :-)

                }

                return buffer.toString();


            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (pd.isShowing()){
                pd.dismiss();
            }
            resultView.setText(result);
        }
    }
}


