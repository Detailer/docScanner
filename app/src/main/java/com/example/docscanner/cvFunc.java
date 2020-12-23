package com.example.docscanner;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.util.ArrayList;
import java.util.List;

public class cvFunc{
    public Bitmap warp(String file){
        Bitmap toReturn;

        Mat orig = Imgcodecs.imread(file);
        Mat src =new Mat();
        Size sz = new Size(500, 500);
        Imgproc.resize(orig, src, sz);

        Mat grey = new Mat();
        Imgproc.cvtColor(src, grey, Imgproc.COLOR_BayerBG2GRAY);

        Mat blur = new Mat();
        Imgproc.GaussianBlur(grey, blur, new Size(5, 5), 1);

        Mat edge = new Mat();
        Imgproc.Canny(blur, edge, 75, 200);

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(edge, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        Boolean found = false;
        Point[] sortedPoints = new Point[4];
        for (MatOfPoint contour: contours){
            MatOfPoint2f contourFloat = new MatOfPoint2f(contour.toArray());
            double arc = Imgproc.arcLength(contourFloat, true) * 0.02;
            MatOfPoint2f approx = new MatOfPoint2f();
            Imgproc.approxPolyDP(contourFloat, approx, arc, true );
            if (approx.total() == 4){
                found = true;
                //calculate the center of mass of our contour image using moments
                Moments moment = Imgproc.moments(approx);
                int x = (int) (moment.get_m10() / moment.get_m00());
                int y = (int) (moment.get_m01() / moment.get_m00());

                //SORT POINTS RELATIVE TO CENTER OF MASS
                double[] data;
                int count = 0;
                for(int i = 0; i < approx.total(); i++){
                    data = approx.get(i, 0);
                    double datax = data[0];
                    double datay = data[1];
                    if(datax < x && datay < y){
                        sortedPoints[0] = new Point(datax,datay);
                        count++;
                    }else if(datax > x && datay < y){
                        sortedPoints[1] = new org.opencv.core.Point(datax,datay);
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
                    new Point(500 - 1, 0),
                    new Point(0, 500 - 1),
                    new Point(500 - 1, 500 - 1)
            );

            Mat warpMat = Imgproc.getPerspectiveTransform(source, destination);
            Mat warpped = new Mat();
            Imgproc.warpPerspective(src, warpped, warpMat, src.size());

            Mat warrpedGrey = new Mat();
            Imgproc.cvtColor(warpped, warrpedGrey, Imgproc.COLOR_RGB2GRAY);

            Mat finalMat = new Mat();
            Imgproc.adaptiveThreshold(warrpedGrey, finalMat, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 11, 6);
            toReturn = Bitmap.createBitmap(finalMat.rows(), finalMat.cols(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(finalMat, toReturn);
        }
        else{
            toReturn = BitmapFactory.decodeFile(file);
        }
        return toReturn;
    }
}
