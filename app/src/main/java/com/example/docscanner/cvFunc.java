package com.example.docscanner;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.util.ArrayList;
import java.util.List;

public class cvFunc{
    public Bitmap toBit (Mat src){
        Bitmap toReturn = Bitmap.createBitmap(src.cols(), src.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(src, toReturn);
        return toReturn;
    }
    public void warp(String file, ImageView imgView1, ImageView imgView2, ImageView imgView3){
        Bitmap toReturn;

        Mat orig = Imgcodecs.imread(file);
        Mat src = new Mat();
        Size sz = new Size(0, 0);
        double scale = (float) 500 / orig.size().width;
        Imgproc.resize(orig, src, sz, scale, scale, Imgproc.INTER_AREA);
        double h = src.size().height;
        double w = src.size().width;

        Mat grey = new Mat();
        Imgproc.cvtColor(src, grey, Imgproc.COLOR_BGR2GRAY);

        Mat blur = new Mat();
        Imgproc.GaussianBlur(grey, blur, new Size(5, 5), 0);

        // Convert and display on view2 edges
        Mat edge = new Mat();
        Imgproc.Canny(blur, edge, 75, 200);
        Log.d("CV_DETECT", "Setting Found Edges Image on 2!");
        imgView2.setImageBitmap(toBit(edge));

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(edge, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        // Display all found contours on view3
        Mat allContours = src.clone();
        Imgproc.drawContours(allContours, contours, -1, new Scalar(0, 0, 255), Imgproc.LINE_8);
        imgView3.setImageBitmap(toBit(allContours));

        Boolean found = false;
        Point[] sortedPoints = new Point[4];
        for (MatOfPoint contour: contours){
            MatOfPoint2f contourFloat = new MatOfPoint2f(contour.toArray());
            double arc = Imgproc.arcLength(contourFloat, true) * 0.02;
            MatOfPoint2f approx = new MatOfPoint2f();
            Imgproc.approxPolyDP(contourFloat, approx, arc, true );
            Log.d("CV_DETECT", "Current Area: " + Imgproc.contourArea(contour) + ", Points: " + approx.total());
            if (approx.total() == 4 && Imgproc.contourArea(contour) > 1.0){
                found = true;
                // Display Largest Found Contours on view3
                List<MatOfPoint> foundContour = new ArrayList<>();
                foundContour.add(contour);
                Mat contourMat = src.clone();
                Imgproc.drawContours(contourMat, foundContour, -1, new Scalar(0, 0, 255), Imgproc.LINE_8);
                Log.d("CV_DETECT", "Setting Found Contours Image on 2!");
                imgView3.setImageBitmap(toBit(contourMat));

                //calculate the center of mass of our contour image using moments
                Moments moment = Imgproc.moments(approx);
                int x = (int) (moment.get_m10() / moment.get_m00());
                int y = (int) (moment.get_m01() / moment.get_m00());

                //SORT POINTS RELATIVE TO CENTER OF MASS
                int count = 0;
                for(int i = 0; i < approx.total(); i++){
                    double[] data = approx.get(i, 0);
                    double datax = data[0];
                    double datay = data[1];
                    if(datax < x && datay < y){
                        sortedPoints[0] = new Point(datax,datay);
                        count++;
                    }else if(datax > x && datay < y){
                        sortedPoints[1] = new Point(datax,datay);
                        count++;
                    }else if (datax < x && datay > y){
                        sortedPoints[2] = new Point(datax,datay);
                        count++;
                    }else if (datax > x && datay > y){
                        sortedPoints[3] = new Point(datax,datay);
                        count++;
                    }
                }
            }
            break;
        }
        if (found){
            MatOfPoint2f source = new MatOfPoint2f(
                    sortedPoints[0],
                    sortedPoints[1],
                    sortedPoints[2],
                    sortedPoints[3]
            );
            MatOfPoint2f destination = new MatOfPoint2f(
                    new Point(0, 0),
                    new Point(w - 1, 0),
                    new Point(0, h - 1),
                    new Point(w - 1, h - 1)
            );

            Mat warpMat = Imgproc.getPerspectiveTransform(source, destination);
            Mat warpped = new Mat();
            Imgproc.warpPerspective(src, warpped, warpMat, src.size());

            Mat warrpedGrey = new Mat();
            Imgproc.cvtColor(warpped, warrpedGrey, Imgproc.COLOR_RGB2GRAY);

            Mat finalMat = new Mat();
            Imgproc.adaptiveThreshold(warrpedGrey, finalMat, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 11, 6);

            // Display warpeed image
            Log.d("CV_DETECT", "Setting Warrped Image on 1!");
            toReturn = toBit(finalMat);
        }
        else{
            Log.d("CV_DETECT", "Setting Original Image on 1!");
            toReturn = BitmapFactory.decodeFile(file);
        }
        // Display warpeed or orignal image on view1
        imgView1.setImageBitmap(toReturn);
    }
}
