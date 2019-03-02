package com.example.linkgamex;

import android.graphics.Bitmap;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.example.linkgamex.LinkGameXUtils.COL;
import static com.example.linkgamex.LinkGameXUtils.ROW;

public class LinkGameXPicAnalyse {

    /*相似阈值，两图片相似度超过该值可认为相同*/
    private static final double VALUE = 0.85;
    /*哈希感知算法中，把图片缩小到M*M尺寸*/
    private static final int M = 16;

    /*存放特征码与对应的图像，调试用*/
    private HashMap<String, Bitmap> bitmapHashMap = new HashMap<>();

    public LinkGameXPicAnalyse() {

    }

    /*传入连连看游戏的全屏截屏图片，返回相同图片数组，外层Array存放不同的图片坐标数组，内层Array存放相同图片的坐标arrayLists*/
    public ArrayList<ArrayList<LocInfo>> getSameBitmapList(Bitmap bitmap) {
        bitmap = Bitmap.createBitmap(bitmap, LinkGameXUtils.RECT_X, LinkGameXUtils.RECT_Y, LinkGameXUtils.RECT_WIDTH, LinkGameXUtils.RECT_HEIGH);
        double width = LinkGameXUtils.RECT_WIDTH * 1.0 / LinkGameXUtils.COL;
        double heigh = LinkGameXUtils.RECT_HEIGH * 1.0 / LinkGameXUtils.ROW;

        HashMap<String, String> hashMapBitmapKey = new HashMap<>();
        double k = 8;
        /*截取小图片，用感知哈希算法生成特征码，存放在hashMapBitmapKey中*/
        for (int i = 0; i < COL; i++) {
            for (int j = 0; j < ROW; j++) {
                int x = (int) (i * width + k);
                int y = (int) (j * heigh + k);

                Bitmap tmpBitmap = getBitmap(bitmap, x, y, width, heigh, k);
                Mat mat = new Mat();
                Utils.bitmapToMat(tmpBitmap, mat);
                Imgproc.resize(mat, mat, new Size(M, M));
                hashMapBitmapKey.put(i + "" + j, getBitmapKey(mat));
                bitmapHashMap.put(i + "" + j, tmpBitmap);
            }
        }

        /*搜索相同的图片，存放在listHashMap中*/
        HashMap<String, ArrayList<String>> listHashMap = new HashMap<>();
        Set<String> flagSet = new HashSet<>();
        for (int i = 0; i + 1 < ROW * COL; i++) {
            String key1 = i / ROW % COL + "" + i % ROW;
            if (flagSet.contains(key1)) continue;
            for (int j = i + 1; j < ROW * COL; j++) {
                String key2 = j / ROW % COL + "" + j % ROW;
                if (flagSet.contains(key2)) continue;
                if (isSimilarity(hashMapBitmapKey, key1, key2)) {
                    ArrayList<String> strings = listHashMap.get(key1);
                    if (strings == null || strings.isEmpty()) {
                        ArrayList<String> tmp = new ArrayList<>();
                        tmp.add(key2);
                        listHashMap.put(key1, tmp);
                    } else {
                        strings.add(key2);
                    }
                    flagSet.add(key2);
                }
            }
        }

        int cnt = 0;
        //本地输出相同的图片，调试用
        for (String key : listHashMap.keySet()) {
            ArrayList<String> strings = listHashMap.get(key);
            saveBitmap(bitmapHashMap.get(key), cnt + ":" + key + ":" + strings.size());
            for (int i = 0; i < strings.size(); i++) {
                saveBitmap(bitmapHashMap.get(strings.get(i)), cnt + ":" + strings.get(i) + ":" + getSimilarityValue(hashMapBitmapKey, key, strings.get(i)));
            }
            cnt++;
        }

        /*hashMap转ArrayList*/
        ArrayList<ArrayList<LocInfo>> result = new ArrayList<>();
        for (String key : listHashMap.keySet()) {
            ArrayList<String> strings = listHashMap.get(key);
            ArrayList<LocInfo> tmpList = new ArrayList<>();

            int[] locInfo = LinkGameXUtils.getRowColFromKey(key);
            tmpList.add(new LocInfo(locInfo[0], locInfo[1]));

            for (int i = 0; i < strings.size(); i++) {
                int[] locInfo2 = LinkGameXUtils.getRowColFromKey(strings.get(i));
                tmpList.add(new LocInfo(locInfo2[0], locInfo2[1]));
            }
            result.add(tmpList);
        }

        return result;
    }

    private Bitmap getBitmap(Bitmap originBitmap, int x, int y, double width, double heigh, double k) {
        Bitmap bitmap = Bitmap.createBitmap(originBitmap, x, y, (int) (width - k * 1.3), (int) (heigh - k * 1.65));
        Mat mat = new Mat();
        Mat grayMat = new Mat();
        Utils.bitmapToMat(bitmap, mat);
        Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_RGB2GRAY);
        Imgproc.threshold(grayMat, mat, 150, 255, Imgproc.THRESH_BINARY);

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(mat, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        Rect rect = null;
        for (int j = 0; j < hierarchy.cols(); j++) {
            if (hierarchy.get(0, j)[3] == 0) {
                rect = Imgproc.boundingRect(contours.get(j));
//                Imgproc.cvtColor(mat, mat, Imgproc.COLOR_GRAY2RGB);
//                Imgproc.rectangle(mat, new Point(rect.x, rect.y), new Point(rect.x
//                        + rect.width, rect.y + rect.height), new Scalar(255, 0, 0));
//                Utils.matToBitmap(mat, bitmap);
                break;
            }
        }

        if (rect != null) {
            Bitmap bitmap1 = Bitmap.createBitmap(bitmap, rect.x, rect.y, rect.width, rect.height);
            Mat mat1 = new Mat();
            Utils.bitmapToMat(bitmap1, mat1);
            Imgproc.cvtColor(mat1, mat1, Imgproc.COLOR_RGB2GRAY);
            Utils.matToBitmap(mat1, bitmap1);
//            saveBitmap(bitmap1, "crop " + x + ":" + y);
            return bitmap1;
        }
        return bitmap;
    }

    private void gray255togray64(Mat mat) {
        for (int i = 0; i < mat.width(); i++) {
            for (int j = 0; j < mat.height(); j++) {
                double gray = mat.get(i, j)[0];
                mat.put(i, j, new double[]{gray / 255 * 64});
            }
        }
    }

    private String getBitmapKey(Mat mat) {
        double sum = 0;
        for (int i = 0; i < mat.width(); i++) {
            for (int j = 0; j < mat.height(); j++) {
                sum += mat.get(i, j)[0];
            }
        }
        double avg = sum / (M * M);
        String result = "";
        for (int i = 0; i < mat.width(); i++) {
            for (int j = 0; j < mat.height(); j++) {
                result += (mat.get(i, j)[0] > avg) ? "1" : "0";
            }
        }
        return result;
    }

    private boolean isSimilarity(HashMap<String, String> hashMap, String key1, String key2) {
        return getSimilarityValue(hashMap, key1, key2) > VALUE;
    }

    int cnt = 0;

    private float getSimilarityValue(HashMap<String, String> hashMap, String key1, String key2) {
        String value1 = hashMap.get(key1);
        String value2 = hashMap.get(key2);

        int sum = 0;
        for (int i = 0; i < value1.length(); i++) {
            if (value1.charAt(i) == value2.charAt(i)) {
                sum++;
            }
        }
        float result = (float) (sum * 1.0 / (M * M));
        return result;
    }

    private void printContoursInfo(List<MatOfPoint> contours, Mat hierarchy) {
        for (int i = 0; i < contours.size(); i++) {
            double[] doubles = hierarchy.get(0, i);
            System.out.println("当前轮廓：" + i + " 前一轮廓：" + doubles[0] + " 后一轮廓：" + doubles[1] + " 子轮廓：" + doubles[2] + " 父轮廓：" + doubles[3]);
        }
    }

    /*保存图片，便于调试*/
    private void saveBitmap(Bitmap bitmap, String fileName) {
        File cacheDir = new File(LinkGameXUtils.APP_CACHE_DIR);
        if (!cacheDir.exists()) {
            cacheDir.mkdir();
        }
        try {
            File file = new File(LinkGameXUtils.APP_CACHE_DIR + fileName + ".jpg");
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
