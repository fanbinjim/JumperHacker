package com.example.fanbin.led;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private Camera mCamera;
    private SurfaceHolder mHolder;
    private ToggleButton btn_toggle;
    private ImageView imageView_P1;
    private ImageView[] ivp = new ImageView[6];
    private boolean isPreview = false;
    private boolean isProcess = false;
    private ToggleButton tb_isProcess;
    private TextView tv_height, tv_width;
    private Bitmap srcBitmap;
    private TextView tv_jumpDistance;
    private TextView tv_nextStatus;
    private int nextStatus;

    private int pwm_value = 1000;

    private Timer servoTimer;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {

            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.i(TAG, "Trying to load OpenCV library");
        if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback)) {
            Log.e(TAG, "Cannot connect to OpenCV Manager");
        }

        Log.d(TAG, "onCreate!");

        Button btn_open = findViewById(R.id.bt_open);
        Button btn_close = findViewById(R.id.bt_close);
        Button btn_capture = findViewById(R.id.bt_capture);
        Button btn_P1 = findViewById(R.id.bt_P1);
        Button btn_testServo = findViewById(R.id.bt_TestServo);
        SeekBar seekBar = findViewById(R.id.seekBar2);
        seekBar.setMax(1000);

        tb_isProcess = findViewById(R.id.tb_isProcess);
        final TextView tv_isProcess = findViewById(R.id.tv_isProcess);
        final TextView tv_pwmvalue = findViewById(R.id.tv_servoValue);

        btn_toggle = findViewById(R.id.bt_toggle);


        SurfaceView mView = findViewById(R.id.surfaceView);
        mHolder = mView.getHolder();
        mHolder.setKeepScreenOn(true);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mHolder.addCallback(new TakePictureSurfaceCallback());

        imageView_P1 = findViewById(R.id.iv_P1);

        ivp[0] = findViewById(R.id.ivp_0);
        ivp[1] = findViewById(R.id.ivp_1);
        ivp[2] = findViewById(R.id.ivp_2);
        ivp[3] = findViewById(R.id.ivp_3);
        ivp[4] = findViewById(R.id.ivp_4);
        ivp[5] = findViewById(R.id.ivp_5);

        tv_height = findViewById(R.id.showHeight);
        tv_width = findViewById(R.id.showWidth);

        tv_jumpDistance = findViewById(R.id.tv_jumpDistance);
        tv_nextStatus = findViewById(R.id.tv_nextStatus);

        btn_open.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    open();
                    Log.d(TAG, "click open button!");
                    btn_toggle.setChecked(true);
                } catch (Exception e) {
                }
            }
        });
        btn_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    close();
                    Log.d(TAG, "click close button!");
                    btn_toggle.setChecked(false);
                } catch (Exception e) {
                }
            }
        });

        btn_capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCamera != null) {
                    mCamera.takePicture(null, null, new TakePictureCallback());
                }
            }
        });

        btn_P1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nextStatus = 0;
                isJumping = false;
            }
        });

        btn_testServo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    Log.d(TAG, "onClick: write pwm servo");
                    writeServo(pwm_value);
                } catch (Exception e) {
                    Log.d(TAG, "onClick: " + e.getMessage());
                }
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                pwm_value = i + 1000;
                tv_pwmvalue.setText(Integer.toString(pwm_value));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                try {
                    Log.d(TAG, "onClick: write pwm servo");
                    writeServo(pwm_value);
                } catch (Exception e) {
                    Log.d(TAG, "onClick: " + e.getMessage());
                }
            }
        });

        tb_isProcess.setChecked(false);
        tb_isProcess.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                isProcess = b;
                if (b) {
                    tv_isProcess.setText("Process Image ON!");
                } else {
                    tv_isProcess.setText("Process Image OFF!");
                }
            }
        });

        btn_toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    try {
                        open();
                        Log.d(TAG, "led open!");
                    } catch (Exception e) {
                    }
                } else {
                    try {
                        close();
                        Log.d(TAG, "led close!");
                    } catch (Exception e) {
                    }
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mCamera == null) {
            mCamera = getCamera();
            if (mHolder != null) {
                setStartPreview(mCamera, mHolder);
            }
        }
    }


    static int runCnt = 0;
    class RequestTimerTask extends TimerTask {
        public void run() {
            Log.d(TAG,"timer on schedule");

            runCnt++;
            if (runCnt == 1){
                try {
                    Log.d(TAG, "onClick: write pwm servo");
                    writeServo(1200);
                } catch (Exception e) {
                    Log.d(TAG, "onClick: " + e.getMessage());
                }
            }else if (runCnt == 2){
                runCnt = 0;
                servoTimer.cancel();
                try {
                    Log.d(TAG, "onClick: write pwm servo");
                    writeServo(1000);
                } catch (Exception e) {
                    Log.d(TAG, "onClick: " + e.getMessage());
                }
                nextStatus = 0;
                isJumping = false;
            }

            //nextStatus = 0;

        }
    }

    static int imageIndex = 0;
    static int processResult = 0;
    static boolean isReadyToProcess = false;
    static Vector<Point> real_vertexs = new Vector<>();
    static int vertex_counter = 0;
    static int nextJumpDistanceSave = 0;
    static boolean isJumping = false;
    private void processRun(Mat img) {

        Mat srcImage = new Mat();
        img.copyTo(srcImage);
        tv_height.setText("Height:" + srcImage.rows());
        tv_width.setText("Width:" + srcImage.cols());
        Mat rotation_matrix = Imgproc.getRotationMatrix2D(new Point(srcImage.rows() - (srcImage.cols() - srcImage.rows()), srcImage.rows() - (srcImage.cols() - srcImage.rows())), 90, 1);
        Imgproc.warpAffine(srcImage, srcImage, rotation_matrix, new Size(srcImage.rows(), srcImage.cols()));
        //showMat(srcImage);

        // find phone screen
        if(isReadyToProcess){
            if (real_vertexs.size() < 4)    {isReadyToProcess = false; return; }
            if (nextStatus > 5) {
                // 已经检测到下一次跳跃的距离，跳完后清空标志

                tv_nextStatus.setText("Ready to Jump!");
                if (!isJumping){
                    servoTimer = new Timer();

                    try {
                        Log.d(TAG, "onClick: write pwm servo");
                        writeServo(1200);
                    } catch (Exception e) {
                        Log.d(TAG, "onClick: " + e.getMessage());
                    }

                    servoTimer.schedule(new RequestTimerTask(),0, (int)(nextJumpDistanceSave * 5.3));
                    isJumping = true;
                }


                return;
            }
            tv_nextStatus.setText("Seeking next Spot");

            MatOfPoint2f targetPoint = new MatOfPoint2f(
                    new Point(0, 0),
                    new Point(360, 0),
                    new Point(360, 640),
                    new Point(0, 640)
            );
            MatOfPoint2f srcPoint = new MatOfPoint2f(
                    real_vertexs.get(0),
                    real_vertexs.get(1),
                    real_vertexs.get(2),
                    real_vertexs.get(3)
            );
            Mat perspectiveTransform = Imgproc.getPerspectiveTransform(srcPoint, targetPoint);
            Mat new_img = new Mat();
            Imgproc.warpPerspective(srcImage, new_img, perspectiveTransform, new Size(360, 640));
            debugShowImage(new_img, 1);

            //saveMatToJPEG(new_img);

            org.opencv.core.Rect rect = new org.opencv.core.Rect(10, 210, 340, 220); // 设置矩形ROI的位置
            Mat imgRectROI = new Mat(new_img, rect);

            // find the chess
            Point chess = findChess(imgRectROI);
            if (chess.x < 0 || chess.y < 0){
                Log.d(TAG, "processRun: wrong chess location");
                return;
            }
            Mat showChess = new Mat();
            imgRectROI.copyTo(showChess);
            Imgproc.circle(showChess, chess, 5, new Scalar(255, 0,  255), 3);
            Imgproc.line(showChess, new Point(chess.x, 0), new Point(chess.x, imgRectROI.rows()), new Scalar(255,0,0));
            Imgproc.line(showChess, new Point(0, chess.y - chess.x / Math.sqrt(3)), new Point((220 - chess.y) * Math.sqrt(3) + chess.x , 220), new Scalar(255, 0, 0));

            debugShowImage(showChess, 2);
            Point nextJump = findNextJump(imgRectROI, chess);
            int nextJumpDistance = (int)(Math.abs(nextJump.x - chess.x) );
            tv_jumpDistance.setText(String.format("Distance: %d",nextJumpDistance));
            if(nextStatus == 0){
                nextJumpDistanceSave = nextJumpDistance;
            }
            if (Math.abs(nextJumpDistance - nextJumpDistanceSave) > 10){
                nextStatus = 1;
                nextJumpDistanceSave = nextJumpDistance;
            }else {
                nextStatus ++;
                nextJumpDistanceSave = (int)(0.5 * nextJumpDistanceSave + 0.5 * nextJumpDistance);
            }

        }else{
            Mat grayImg = new Mat();
            Imgproc.cvtColor(srcImage, grayImg, Imgproc.COLOR_BGR2GRAY);
            Imgproc.blur(grayImg, grayImg, new Size(5, 5)) ;

            Vector<Point> distancePoint = new Vector<>();
            distancePoint.add(new Point(100, 80));
            distancePoint.add(new Point(400, 80));
            distancePoint.add(new Point(400, 560));
            distancePoint.add(new Point(100, 560));

            for(int i = 0; i < 4;i ++){
                Imgproc.circle(grayImg, distancePoint.get(i), 80, new Scalar(255, 255,  255), 3);
            }
            showMat(grayImg);

            Mat edges = new Mat();
            Imgproc.Canny(grayImg, edges, 80, 140);

            Mat lines = new Mat();
            Imgproc.HoughLinesP(edges, lines, 1, Math.PI /180, 50,200, 30);
            for (int y=0;y<lines.rows();y++)
            {
                double[] vec = lines.get(y, 0);
                double  x1 = vec[0],
                        y1 = vec[1],
                        x2 = vec[2],
                        y2 = vec[3];

                Point start = new Point(x1, y1);
                Point end = new Point(x2, y2);
                Imgproc.line(srcImage, start, end, new Scalar(255,0,0), 4);
            }
            Vector<Point> points = new Vector<>();
            for (int i = 0; i < lines.rows() - 1; i++)
            {
                for(int j = i + 1; j < lines.rows(); j++)
                {
                    double[] line1 = lines.get(i, 0);
                    double[] line2 = lines.get(j, 0);
                    Point point = compute_intersection(line1, line2);
                    if( point.x > 0 && point.x <srcImage.cols() && point.y > 0 && point.y < srcImage.rows()){
                        points.add(point);
                    }
                }
            }
            Vector<Point> vertexs = compute_vertex(points);

            for (int i = 0; i < vertexs.size(); i++) {
                Imgproc.circle(srcImage, vertexs.get(i), 5, new Scalar(0, 255,  0), 3);
                Imgproc.putText(srcImage, Integer.toString(i), vertexs.get(i), Core.FONT_HERSHEY_PLAIN, 3, new Scalar(255, 0, 255), 4);
            }

            if(vertexs.size() != 4 ){
                Log.d(TAG, "processRun: the number of vertexs is not 4");
                return;
            }else{
                Log.d(TAG, "processRun: the number of vertexs is 4");
            }



            vertexs = sort_vertex(vertexs);
            for(int i = 0; i < vertexs.size(); i++){
                double distance = get_distance(vertexs.get(i), distancePoint.get(i));
                if(distance > 80){
                    Log.d(TAG, "processRun: the vertexs are wrong!!!!");
                    return;
                }
            }

            vertex_counter ++;
            for (int i = 0; i < vertexs.size(); i++){
                if (real_vertexs.size() < 4) {
                    real_vertexs.add(vertexs.get(i));
                }
                double distance = get_distance(vertexs.get(i), real_vertexs.get(i));
                if (distance > 8){
                    vertex_counter = 0;
                    real_vertexs.set(i, vertexs.get(i));
                } else {
                    Point newPoint = new Point(vertexs.get(i).x * 0.5 + real_vertexs.get(i).x * 0.5, vertexs.get(i).y * 0.5 + real_vertexs.get(i).y * 0.5);
                    real_vertexs.set(i, newPoint);
                }

            }
            if(vertex_counter > 0){
                Log.d(TAG, "processRun: sucess init the vertexs!!!!!!");
                isReadyToProcess = true;
            }

        }
        debugShowImage(srcImage, 0);
    }

    private Point findNextJump(Mat imgRectROI, Point chess) {
        // chess 27 x 71
        Mat roi = new Mat();
        imgRectROI.copyTo(roi);
        org.opencv.core.Rect rect = new org.opencv.core.Rect(0, 0, 340, (int)chess.y - 30);
        Mat processRoi = new Mat(roi, rect);

        Imgproc.GaussianBlur(processRoi, processRoi, new Size(7, 7), 0);
        Mat cannyImg = new Mat();
        Imgproc.Canny(processRoi, cannyImg, 25, 50);

        for (int i = (int)chess.x - 14; i < chess.x + 14; i++){
            for(int j = 0; j < cannyImg.rows(); j++){
                cannyImg.put(j, i, 0);
            }
        }

        int center_x = 0, center_y = 0, max_x = 0;
        for (int y = 0; y < cannyImg.rows(); y++){

            for (int x = 0; x < cannyImg.cols(); x++){
                double [] temp = cannyImg.get(y, x);
                if (temp[0] == 255){
                    //Log.d(TAG, "findNextJump: 255");
                    if (center_x == 0) {
                        center_x = x;
                    }
                }
            }
        }


        debugShowImage(cannyImg, 4);
        Imgproc.circle(roi, new Point(center_x, center_y), 5, new Scalar(255, 0, 0), 4);
        Imgproc.line(roi, new Point(center_x, 0), new Point(center_x, roi.rows()), new Scalar(255, 0, 0), 1);
        Imgproc.line(roi, new Point(chess.x, 0), new Point(chess.x, roi.rows()), new Scalar(255, 0, 0), 1);
        Log.d(TAG, "findNextJump: " + Integer.toString(center_x) + " " + Integer.toString(center_y));
        Log.d(TAG, "rows " + Integer.toString(cannyImg.rows()) + " cols " + Integer.toString(cannyImg.cols()));

        debugShowImage(roi, 3);
        return new Point( center_x, 0);
    }

    private Point findChess(Mat roi_img){
        String loc = "/storage/emulated/0/DCIM/template.png";
        Mat roi = new Mat();
        roi_img.copyTo(roi);
        Mat template = Imgcodecs.imread(loc, 0);
        Mat gray = new Mat();
        Imgproc.cvtColor(roi, gray, Imgproc.COLOR_BGR2GRAY);
        Mat result = new Mat();
        Imgproc.matchTemplate(gray, template, result, Imgproc.TM_CCOEFF_NORMED);
        Core.MinMaxLocResult mmr = Core.minMaxLoc(result);
        if(mmr.maxVal > 0.65){
            return new Point(mmr.maxLoc.x + template.cols() * 0.5, mmr.maxLoc.y + template.rows());
        }else {
            return new Point(-1,-1);
        }
    }

    private Vector<Point> sort_vertex(Vector<Point> vertexs){

        int sum_x = 0, sum_y = 0, center_y;
        for(int i = 0; i < vertexs.size(); i++){
            sum_x += vertexs.get(i).x;
            sum_y += vertexs.get(i).y;
        }
        center_y = sum_y / 4;
        Vector<Point> top = new Vector<>();
        Vector<Point> bottom = new Vector<>();
        Point tl, tr, bl, br;
        for (int i = 0; i < vertexs.size(); i++){
            if (vertexs.get(i).y < center_y){
                top.add(vertexs.get(i));
            } else {
                bottom.add(vertexs.get(i));
            }
        }
        if (top.size() != 2 || bottom.size() != 2){
            return vertexs;
        }
        tl = top.get(0).x < top.get(1).x ? top.get(0) : top.get(1);
        tr = top.get(0).x >= top.get(1).x ? top.get(0) : top.get(1);
        bl = bottom.get(0).x < bottom.get(1).x ? bottom.get(0) : bottom.get(1);
        br = bottom.get(0).x >= bottom.get(1).x ? bottom.get(0) : bottom.get(1);

        vertexs.clear();
        vertexs.add(tl);
        vertexs.add(tr);
        vertexs.add(br);
        vertexs.add(bl);
        return vertexs;
    }
    private double get_distance(Point p1, Point p2){
        return Math.sqrt( (p1.x - p2.x)*(p1.x - p2.x) + (p1.y - p2.y)*(p1.y - p2.y) );
    }
    private boolean if_close(Vector<Point> vertex, Point point){
        for(int i = 0; i < vertex.size(); i++){
            if (get_distance(point, vertex.get(i)) < 10){
                return true;
            }
        }
        return false;
    }
    private Vector<Point> compute_vertex(Vector<Point> points){
        Vector<Point> vertex = new Vector<>();
        for(int i = 0; i < points.size(); i++){
            if(vertex.isEmpty()){
                vertex.add(points.get(i));
            }else{
                if( if_close(vertex, points.get(i))){
                    continue;
                }else{
                    vertex.add(points.get(i));
                }
            }
        }
        return vertex;
    }

    private Point compute_intersection(double[] line1, double[] line2){
        Point intersection = new Point(-1,-1);

        double x1,x2,x3,x4,y1,y2,y3,y4;
        x1 = line1[0];
        y1 = line1[1];
        x2 = line1[2];
        y2 = line1[3];

        x3 = line2[0];
        y3 = line2[1];
        x4 = line2[2];
        y4 = line2[3];

        double k1, k2;
        k1 = (y2 - y1) / (x2 - x1);
        k2 = (y4 - y3) / (x4 - x3);

        if( Math.abs((k1 - k2) / (1 + k1 * k2)) < 0.5){
            return new Point(-1, -1);
        }
        double d = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
        double x = -1, y = -1;
        if(d > 0.001){
            x = ((x1 * y2 - y1 * x2) * (x3 - x4) - (x1 - x2) * (x3 * y4 - y3 * x4)) / d;
            y = ((x1 * y2 - y1 * x2) * (y3 - y4) - (y1 - y2) * (x3 * y4 - y3 * x4)) / d;
        }
        return new Point(x, y);
    }

    private void showMat(Mat image)
    {
        Bitmap bitmap = Bitmap.createBitmap(image.width(), image.height(), Bitmap.Config.RGB_565);
        Utils.matToBitmap(image, bitmap);
        imageView_P1.setImageBitmap(bitmap);
    }


    private void debugShowImage(Mat image, int which) {
        Bitmap bitmap = Bitmap.createBitmap(image.width(), image.height(), Bitmap.Config.RGB_565);
        Utils.matToBitmap(image, bitmap);
        ivp[which].setImageBitmap(bitmap);
    }

    private void saveMatToJPEG(Mat image) {
        String loc = "/storage/emulated/0/DCIM/Process/";

        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        Date curDate = new Date(System.currentTimeMillis());//获取当前时间
        String strDate = formatter.format(curDate);

        String path = loc + "IMG-" + strDate + ".png";

        FileOutputStream fos ;

        Bitmap bmp = Bitmap.createBitmap(image.width(), image.height(), Bitmap.Config.RGB_565);
        Utils.matToBitmap(image, bmp);
        try {
            File f = new File(path);
            if(!f.exists()){
                f.getParentFile().mkdir();
                f.createNewFile();
            }
            fos = new FileOutputStream(f);
            if (fos != null) {
                bmp.compress(Bitmap.CompressFormat.PNG, 90, fos);
                fos.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final class TakePictureSurfaceCallback implements SurfaceHolder.Callback{
        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            setStartPreview(mCamera, mHolder);
        }
        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
            setStartPreview(mCamera, mHolder);
        }
        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            releaseCamera();
        }
    }



    private final class TakePictureCallback implements Camera.PictureCallback{
        @Override
        public void onPictureTaken(byte[] data, Camera camera){
            camera.stopPreview();
            isPreview = false;

            Bitmap bmp=BitmapFactory.decodeByteArray(data, 0, data.length);

            srcBitmap = Bitmap.createBitmap(bmp);

//            Bitmap new_bmp = zoomImage(bmp, 240, 180);

            Log.d(TAG, "onPictureTaken: " + bmp.getHeight());
            Log.d(TAG, "onPictureTaken: " + bmp.getWidth());

            String loc = "/storage/emulated/0/DCIM/Camera/";

            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            Date curDate = new Date(System.currentTimeMillis());//获取当前时间
            String strDate = formatter.format(curDate);

            String path = loc + "IMG-" + strDate + ".png";
            FileOutputStream fos;
            try {
                fos = new FileOutputStream(path);
                if (fos != null) {
                    bmp.compress(Bitmap.CompressFormat.PNG, 90, fos);
                    fos.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            setStartPreview(mCamera, mHolder);
        }
    }
    ByteArrayOutputStream baos;
    byte[] rawImage;
    Bitmap bitmap;
    private final class StreamIt implements Camera.PreviewCallback{
        @Override
        public void onPreviewFrame(byte[] bytes, Camera camera) {
            //Log.d(TAG, "preview frame");
            if(tb_isProcess.isChecked()){
                //Log.d(TAG, "preview frame");
                Camera.Size previewSize = camera.getParameters().getPreviewSize();
                BitmapFactory.Options newOpts = new BitmapFactory.Options();
                newOpts.inJustDecodeBounds = true;
                YuvImage yuvimage = new YuvImage(
                        bytes,
                        ImageFormat.NV21,
                        previewSize.width,
                        previewSize.height,
                        null);
                baos = new ByteArrayOutputStream();
                yuvimage.compressToJpeg(new Rect(0, 0, previewSize.width, previewSize.height), 100, baos);// 80--JPG图片的质量[0-100],100最高
                rawImage = baos.toByteArray();
                //将rawImage转换成bitmap
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.RGB_565;
                bitmap = BitmapFactory.decodeByteArray(rawImage, 0, rawImage.length, options);
                //imageView_P1.setImageBitmap(bitmap);
                Mat img = new Mat();
                Utils.bitmapToMat(bitmap, img);
                processRun(img);

            }
        }
    }

    /*得到摄像头*/
    private  Camera getCamera(){
        Camera camera;
        try {
            camera = Camera.open();
            if(camera != null && !isPreview){
                try{
                    Camera.Parameters  parameters = camera.getParameters();
                    //parameters.setPreviewSize(1280, 720); // 设置预览照片的大小
                    parameters.setPreviewFpsRange(20, 30); // 每秒显示20~30帧
                    parameters.setPictureFormat(ImageFormat.NV21); // 设置图片格式
                    //parameters.setPictureSize(1280, 720); // 设置照片的大小
                    parameters.setPreviewFrameRate(3);// 每秒3帧 每秒从摄像头里面获得3个画面,
                    camera.setParameters(parameters); // android2.3.3以后不需要此行代码
                    camera.setPreviewDisplay(mHolder); // 通过SurfaceView显示取景画面
                    camera.startPreview(); // 开始预览
                }catch(Exception e){
                    e.printStackTrace();
                }
                isPreview = true;
            }
        } catch (Exception e) {
            camera = null;
            e.printStackTrace();
            Toast.makeText(this, "无法获取摄像头", Toast.LENGTH_LONG).show();
        }
        return camera;
    }

    private void setStartPreview(Camera camera, SurfaceHolder holder) {
        try {
            camera.setPreviewDisplay(holder);
            camera.setPreviewCallback(new StreamIt()); // 设置回调的类
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void releaseCamera(){
        if (mCamera != null){
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            isPreview = false;
            mCamera.release();
            mCamera = null;
        }
    }

    private int getDisplayOrientation() {
        WindowManager windowManager = getWindowManager();
        Display display = windowManager.getDefaultDisplay();
        int rotation = display.getRotation();
        int degrees = 0;
        switch (rotation){
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        android.hardware.Camera.CameraInfo camInfo =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, camInfo);

        // 这里其实还是不太懂：为什么要获取camInfo的方向呢？相当于相机标定？？
        int result = (camInfo.orientation - degrees + 360) % 360;

        return result;
    }

    private void testServo() throws Exception {
        byte[] value = new byte[4];
        String str = "1500";
        File f = new File("/dev/pwm-servo0");
        FileInputStream fileIS = new FileInputStream(f);
        fileIS.read(value, 0, 4);
        fileIS.close();

        Log.d(TAG, "testServo: " + bytesToInt(value));
    }
    private void writeServo(int value) throws Exception{
        File f = new File("/dev/pwm-servo0");
        FileOutputStream fileOS = new FileOutputStream(f);
        fileOS.write(intToByteArray(value));
        fileOS.close();

    }
    public static byte[] intToByteArray(int a) {
        return new byte[] {
                (byte) (a & 0xFF),
                (byte) ((a >> 8) & 0xFF),
                (byte) ((a >> 16) & 0xFF),
                (byte) ((a >> 24) & 0xFF)
        };
    }
    public static int bytesToInt(byte[] bytes) {
        int i;
        i = (int) ((bytes[0] & 0xff) | ((bytes[1] & 0xff) << 8)
                | ((bytes[2] & 0xff) << 16) | ((bytes[3] & 0xff) << 24));
        return i;
    }

    private void open() throws Exception {
        String str = "1";
        File f = new File("/sys/class/leds/firefly:yellow:user/brightness");
        FileOutputStream fileOS = new FileOutputStream(f);
        fileOS.write(str.getBytes());
        fileOS.close();
    }

    private void close() throws Exception {
        String str = "0";
        File f = new File("/sys/class/leds/firefly:yellow:user/brightness");
        FileOutputStream fileOS = new FileOutputStream(f);
        fileOS.write(str.getBytes());
        fileOS.close();
    }

}
