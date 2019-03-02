package com.example.linkgamex;

import android.graphics.Bitmap;

import java.util.ArrayList;

public class LinkGameXAnalyse {

    final int flagRow = LinkGameXUtils.ROW + 2;
    final int flagCol = LinkGameXUtils.COL + 2;
    boolean[][] flag = new boolean[LinkGameXUtils.ROW + 2][LinkGameXUtils.COL + 2];

    public LinkGameXAnalyse() {
        for (int i = 0; i < flagRow; i++) {
            for (int j = 0; j < flagCol; j++) {
                if (i == 0 || i == flagRow - 1 || j == 0 || j == flagCol - 1) {
                    flag[i][j] = true;
                    continue;
                }
                flag[i][j] = false;
            }
        }
    }

    //没有拐点的情况
    private boolean canLink1(LocInfo locInfo1, LocInfo locInfo2) {
        if (locInfo1.x != locInfo2.x && locInfo1.y != locInfo2.y) return false;
        if (locInfo1.x == locInfo2.x) {
            int min = Math.min(locInfo1.y, locInfo2.y);
            int max = Math.max(locInfo1.y, locInfo2.y);
            for (int i = min + 1; i < max; i++) {
                if (!isPointDismiss(new LocInfo(locInfo1.x, i))) {
                    return false;
                }
            }
        } else {
            int min = Math.min(locInfo1.x, locInfo2.x);
            int max = Math.max(locInfo1.x, locInfo2.x);
            for (int i = min + 1; i < max; i++) {
                if (!isPointDismiss(new LocInfo(i, locInfo1.y))) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean needContinue() {
        for (int i = 0; i < LinkGameXUtils.ROW + 2; i++) {
            for (int j = 0; j < LinkGameXUtils.COL + 2; j++) {
                if (flag[i][j] == false) {
                    return true;
                }
            }
        }
        return false;
    }

    /*获得连连看的点击序列，若无法一次性完成，返回null*/
    public ArrayList<LocInfo> getTouchList(Bitmap bitmap) {
        LinkGameXPicAnalyse linkGameXPicAnalyse = new LinkGameXPicAnalyse();
        ArrayList<ArrayList<LocInfo>> arrayLists = linkGameXPicAnalyse.getSameBitmapList(bitmap);
        ArrayList<LocInfo> result = new ArrayList<>();

        boolean noSolveFlag;
        while (needContinue()) {
            noSolveFlag = true;
            for (int i = 0; i < arrayLists.size(); i++) {
                for (int j = 0; j < arrayLists.get(i).size(); j++) {
                    LocInfo locInfo1 = arrayLists.get(i).get(j);
                    if (isPointDismiss(locInfo1)) continue;
                    for (int k = j + 1; k < arrayLists.get(i).size(); k++) {
                        LocInfo locInfo2 = arrayLists.get(i).get(k);
                        if (isPointDismiss(locInfo2)) continue;
                        if (canLink1(locInfo1, locInfo2) || canLink2(locInfo1, locInfo2) || canLink3(locInfo1, locInfo2)) {
                            setPointDismiss(locInfo1);
                            setPointDismiss(locInfo2);
                            result.add(locInfo1);
                            result.add(locInfo2);
                            noSolveFlag = false;
                            break;
                        }
                    }
                }
            }
            if (noSolveFlag) {
                return null;
            }
        }
        return result;
    }

    //只有一个拐点的情况
    private boolean canLink2(LocInfo locInfo1, LocInfo locInfo2) {
        LocInfo crossPont1 = new LocInfo(locInfo1.x, locInfo2.y);
        if (isPointDismiss(crossPont1)) {
            return (canLink1(locInfo1, crossPont1) && canLink1(crossPont1, locInfo2));
        }
        LocInfo crossPont2 = new LocInfo(locInfo2.x, locInfo1.y);
        if (isPointDismiss(crossPont2)) {
            return (canLink1(locInfo1, crossPont2) && canLink1(crossPont2, locInfo2));
        }
        return false;
    }

    //有两个拐点的情况
    private boolean canLink3(LocInfo locInfo1, LocInfo locInfo2) {
        for (int i = 0; i < flagRow; i++) {
            LocInfo locInfo = new LocInfo(i, locInfo1.y);
            if (!isPointDismiss(locInfo) || !canLink1(locInfo1, locInfo)) continue;
            if (canLink2(locInfo, locInfo2)) {
                return true;
            }
        }
        for (int i = 0; i < flagCol; i++) {
            LocInfo locInfo = new LocInfo(locInfo1.x, i);
            if (!isPointDismiss(locInfo) || !canLink1(locInfo1, locInfo)) continue;
            if (canLink2(locInfo, locInfo2)) {
                return true;
            }
        }
        return false;
    }

    private boolean isPointDismiss(LocInfo locInfo) {
        return flag[locInfo.x][locInfo.y];
    }

    private void setPointDismiss(LocInfo locInfo) {
        flag[locInfo.x][locInfo.y] = true;
    }
}
