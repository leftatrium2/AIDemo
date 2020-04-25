package com.sina.aidemo.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.media.Image;
import android.media.ThumbnailUtils;
import android.util.Log;
import android.util.Pair;

import com.sina.aidemo.blazeface.Face;
import com.sina.aidemo.handtracking.Hand;
import com.sina.aidemo.handtracking.Hand3D;
import com.sina.aidemo.posenet.PoseBodyPart;
import com.sina.aidemo.posenet.PoseKeyPoint;
import com.sina.aidemo.posenet.PosePerson;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 图像处理部分
 */
public class AIImageUtils {
    /**
     * 图片裁剪
     *
     * @param bitmap 原图
     * @return bitmap
     */
    public static Bitmap cropPoseBitmap(Bitmap bitmap) {
        float bitmapRatio = ((float) bitmap.getHeight()) / bitmap.getWidth();
        float modelInputRatio = ((float) AIConstants.MODEL2_HEIGHT) / AIConstants.MODEL2_WIDTH;
        Bitmap croppedBitmap = bitmap;

        double maxDifference = 1e-5;
        if (Math.abs(modelInputRatio - bitmapRatio) < maxDifference) {
            return croppedBitmap;
        }
        if (modelInputRatio < bitmapRatio) {
            float widthRadio = ((float) bitmap.getWidth()) / modelInputRatio;
            float cropHeight = bitmap.getHeight() - widthRadio;
            croppedBitmap = Bitmap.createBitmap(bitmap, 0, (int) (cropHeight / 2), bitmap.getWidth(), (int) (bitmap.getHeight() - cropHeight));
        } else {
            float cropWidth = bitmap.getWidth() - ((float) bitmap.getHeight() / modelInputRatio);
            croppedBitmap = Bitmap.createBitmap(bitmap, (int) (cropWidth / 2), 0, (int) (bitmap.getWidth() - cropWidth), bitmap.getHeight());
        }
        return croppedBitmap;
    }

    public static Bitmap cropSkeletonBitmap(Bitmap bitmap, int width, int height) {
        if (bitmap == null) {
            Log.e(AIConstants.TAG, "bitmap is null!!!");
        }
        return ThumbnailUtils.extractThumbnail(bitmap, width, height);
    }


    /**
     * 在给定的canvas上面画出posenet骨架图
     *
     * @param person person
     * @param canvas canvas
     */
    public static void drawPoseNetSkeleton(PosePerson person, Canvas canvas) {
        if (person == null || person.keyPoints == null) {
            return;
        }
        Paint paint = new Paint();
        paint.setStrokeWidth(2.f);
        paint.setColor(Color.RED);
        //刷新画布
        canvas.drawColor(Color.BLACK, PorterDuff.Mode.CLEAR);
        int screenWidth = canvas.getWidth();
        int screenHeight = canvas.getHeight();
        int left = 0;
        int top = 0;
        float widthRatio = (float) screenWidth / AIConstants.MODEL2_WIDTH;
        float heightRatio = (float) screenHeight / AIConstants.MODEL2_HEIGHT;
        //画点
        for (PoseKeyPoint keyPoint : person.keyPoints) {
            if (keyPoint.score > AIConstants.MIN_CONFIDENCE) {
                Point position = keyPoint.position;
                float adjustedX = (float) position.x * widthRatio + left;
                float adjustedY = (float) position.y * heightRatio + top;
                canvas.drawCircle(adjustedX, adjustedY, AIConstants.CIRCLE_RADIUS, paint);
            }
        }
        //连线
        for (Pair<PoseBodyPart, PoseBodyPart> line : AIConstants.BODY_JOINTS) {
            if (person.keyPoints.get(line.first.ordinal()).score > AIConstants.MIN_CONFIDENCE && person.keyPoints.get(line.second.ordinal()).score > AIConstants.MIN_CONFIDENCE) {
                canvas.drawLine(
                        (float) person.keyPoints.get(line.first.ordinal()).position.x * widthRatio + left,
                        (float) person.keyPoints.get(line.first.ordinal()).position.y * heightRatio + top,
                        (float) person.keyPoints.get(line.second.ordinal()).position.x * widthRatio + left,
                        (float) person.keyPoints.get(line.second.ordinal()).position.y * heightRatio + top,
                        paint
                );
            }
        }
    }

    /**
     * 在给定的canvas上，绘制面部识别点以及识别框
     *
     * @param face   face
     * @param canvas canvas
     */
    public static void drawBlazeFacePoint(Face face, Canvas canvas) {
        if (face == null) {
            return;
        }
        Paint paint = new Paint();
        paint.setStrokeWidth(2.f);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.RED);
        //刷新画布
        canvas.drawColor(Color.BLACK, PorterDuff.Mode.CLEAR);
        int screenWidth = canvas.getWidth();
        int screenHeight = canvas.getHeight();
        int left = 0;
        int top = 0;
        float widthRatio = (float) screenWidth / AIConstants.MODEL2_WIDTH;
        float heightRatio = (float) screenHeight / AIConstants.MODEL2_HEIGHT;
        //绘制五官上的点
        if (face.keyPoints != null && face.keyPoints.size() != 0) {
            for (Face.SNFaceKeyPoint keyPoint : face.keyPoints) {
                Point position = keyPoint.position;
                float adjustedX = (float) position.x * widthRatio + left;
                float adjustedY = (float) position.y * heightRatio + top;
                canvas.drawCircle(adjustedX, adjustedY, AIConstants.CIRCLE_RADIUS, paint);
            }
        }
        //绘制识别框
        if (face.detectionRect != null) {
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawRect(face.detectionRect, paint);
        }
    }

    /**
     * 在给定的canvas上，绘制手部识别点以及划线
     *
     * @param hand   hand
     * @param canvas canvas
     */
    public static void drawHandTracking(Hand hand, Canvas canvas) {
        if (hand == null) {
            return;
        }
        Paint paint = new Paint();
        paint.setStrokeWidth(2.f);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.RED);
        canvas.drawColor(Color.BLACK, PorterDuff.Mode.CLEAR);
        int screenWidth = canvas.getWidth();
        int screenHeight = canvas.getHeight();
        int left = 0;
        int top = (screenHeight - screenWidth) / 2;
        float widthRatio = (float) screenWidth / AIConstants.HT_MODEL_WIDTH;
        float heightRatio = (float) screenHeight / AIConstants.HT_MODEL_HEIGHT;
        if (hand.points != null && hand.points.size() == AIConstants.HT_MODEL_POINT_NUM) {
            //绘制手上的关节点，一共21个
            int step = 0;
            for (PointF point : hand.points) {
                float adjustedX = point.x * widthRatio + left;
                float adjustedY = point.y * heightRatio + top;
                canvas.drawCircle(adjustedX, adjustedY, AIConstants.CIRCLE_RADIUS, paint);
                step++;
            }
            //连线
            /**
             * 使用如下数列，将关节点连接为一个手型
             * 0  1  2  3  4
             * 0  5  6  7  8
             * 0  9 10 11 12
             * 0 13 14 15 16
             * 0 17 18 19 20
             */
            for (int i = 0; i < 5; i++) {
                for (int j = 0; j < 4; j++) {
                    int beginK = j + i * 4;
                    int endK = beginK + 1;
                    if (j == 0) {
                        beginK = 0;
                    }

                    PointF beginPoint = hand.points.get(beginK);
                    PointF endPoint = hand.points.get(endK);
                    canvas.drawLine(
                            (beginPoint.x * widthRatio + left),
                            (beginPoint.y * heightRatio + top),
                            (endPoint.x * widthRatio + left),
                            (endPoint.y * heightRatio + top),
                            paint);
                }
            }
        }
    }

    /**
     * 在给定的canvas上，绘制手部识别点以及划线
     * // TODO: 2020/3/26 3D不会画，后面补上
     *
     * @param hand   hand
     * @param canvas canvas
     */
    public static void drawHandTracking3D(Hand3D hand, Canvas canvas) {
        if (hand == null) {
            return;
        }
        Paint paint = new Paint();
        paint.setStrokeWidth(2.f);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.RED);
        canvas.drawColor(Color.BLACK, PorterDuff.Mode.CLEAR);
        int screenWidth = canvas.getWidth();
        int screenHeight = canvas.getHeight();
        int left = 0;
        int top = (screenHeight - screenWidth) / 2;
        float widthRatio = (float) screenWidth / AIConstants.HT_MODEL_WIDTH;
        float heightRatio = (float) screenHeight / AIConstants.HT_MODEL_HEIGHT;
        if (hand.points != null && hand.points.size() == AIConstants.HT_MODEL_POINT_NUM) {
            //3D不会画，先画个2D
            //绘制手上的关节点，一共21个
            int step = 0;
            for (Hand3D.SNHandPoint3D point : hand.points) {
                float adjustedX = point.x * widthRatio + left;
                float adjustedY = point.y * heightRatio + top;
                canvas.drawCircle(adjustedX, adjustedY, AIConstants.CIRCLE_RADIUS, paint);
                step++;
            }
            //连线
            /**
             * 使用如下数列，将关节点连接为一个手型
             * 0  1  2  3  4
             * 0  5  6  7  8
             * 0  9 10 11 12
             * 0 13 14 15 16
             * 0 17 18 19 20
             */
            for (int i = 0; i < 5; i++) {
                for (int j = 0; j < 4; j++) {
                    int beginK = j + i * 4;
                    int endK = beginK + 1;
                    if (j == 0) {
                        beginK = 0;
                    }

                    Hand3D.SNHandPoint3D beginPoint = hand.points.get(beginK);
                    Hand3D.SNHandPoint3D endPoint = hand.points.get(endK);
                    canvas.drawLine(
                            (beginPoint.x * widthRatio + left),
                            (beginPoint.y * heightRatio + top),
                            (endPoint.x * widthRatio + left),
                            (endPoint.y * heightRatio + top),
                            paint);
                }
            }
        }
    }

    /**
     * 存bitmap到sd卡
     *
     * @param bitmap bitmap
     */
    public static void saveBitmap(Context context, Bitmap bitmap) {
        if (context == null) {
            return;
        }

        String fileName = System.currentTimeMillis() + ".png";

        String path = context.getExternalFilesDir("pose").getAbsolutePath();
        File file = new File(path + "/" + fileName);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos);
            fos.flush();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public static byte[] generateNV21Data(Image image) {
        Rect crop = image.getCropRect();
        int format = image.getFormat();
        int width = crop.width();
        int height = crop.height();
        Image.Plane[] planes = image.getPlanes();
        byte[] data = new byte[width * height * ImageFormat.getBitsPerPixel(format) / 8];
        byte[] rowData = new byte[planes[0].getRowStride()];
        int channelOffset = 0;
        int outputStride = 1;
        for (int i = 0; i < planes.length; i++) {
            switch (i) {
                case 0:
                    channelOffset = 0;
                    outputStride = 1;
                    break;
                case 1:
                    channelOffset = width * height + 1;
                    outputStride = 2;
                    break;
                case 2:
                    channelOffset = width * height;
                    outputStride = 2;
                    break;
            }
            ByteBuffer buffer = planes[i].getBuffer();
            int rowStride = planes[i].getRowStride();
            int pixelStride = planes[i].getPixelStride();
            int shift = i == 0 ? 0 : 1;
            int w = width >> shift;
            int h = height >> shift;
            buffer.position(rowStride * (crop.top >> shift) + pixelStride * (crop.left >> shift));
            for (int row = 0; row < h; row++) {
                int length;
                if (pixelStride == 1 && outputStride == 1) {
                    length = w;
                    buffer.get(data, channelOffset, length);
                    channelOffset += length;
                } else {
                    length = (w - 1) * pixelStride + 1;
                    buffer.get(rowData, 0, length);
                    for (int col = 0; col < w; col++) {
                        data[channelOffset] = rowData[col * pixelStride];
                        channelOffset += outputStride;
                    }
                }
                if (row < h - 1) {
                    buffer.position(buffer.position() + rowStride - length);
                }
            }
        }
        return data;
    }
}
