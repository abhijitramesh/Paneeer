package org.amfoss.paneeer.gallery.util;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

public class Measure {

  public static final String TAG = "Measure";

  public static int pxToDp(int px, Context c) {
    DisplayMetrics displayMetrics = c.getResources().getDisplayMetrics();
    return Math.round(px * (displayMetrics.ydpi / DisplayMetrics.DENSITY_DEFAULT));
  }

  public static int getStatusBarHeight(Resources r) {
    int resourceId = r.getIdentifier("status_bar_height", "dimen", "android");
    if (resourceId > 0) return r.getDimensionPixelSize(resourceId);

    return 0;
  }

  public static Point getNavigationBarSize(Context context) {
    Point appUsableSize = getAppUsableScreenSize(context);
    Point realScreenSize = getRealScreenSize(context);

    // navigation bar on the right
    if (appUsableSize.x < realScreenSize.x) {
      return new Point(realScreenSize.x - appUsableSize.x, appUsableSize.y);
    }

    // navigation bar at the bottom
    if (appUsableSize.y < realScreenSize.y) {
      return new Point(appUsableSize.x, realScreenSize.y - appUsableSize.y);
    }

    // navigation bar is not present
    return new Point();
  }

  private static Point getAppUsableScreenSize(Context context) {
    WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    Display display = windowManager.getDefaultDisplay();
    Point size = new Point();
    display.getSize(size);
    return size;
  }

  private static Point getRealScreenSize(Context context) {
    WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    Display display = windowManager.getDefaultDisplay();
    Point size = new Point();
    display.getRealSize(size);
    return size;
  }
}
