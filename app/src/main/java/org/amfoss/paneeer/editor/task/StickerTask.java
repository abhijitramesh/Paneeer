package org.amfoss.paneeer.editor.task;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.os.Build;

import org.amfoss.paneeer.R;
import org.amfoss.paneeer.editor.EditBaseActivity;
import org.amfoss.paneeer.editor.EditImageActivity;
import org.amfoss.paneeer.editor.utils.Matrix3;

public abstract class StickerTask extends AsyncTask<Bitmap, Void, Bitmap> {
  private Dialog dialog;

  private EditImageActivity mContext;
  private Matrix imageViewMatrix;

  public StickerTask(EditImageActivity activity, Matrix imageViewMatrix) {
    this.mContext = activity;
    this.imageViewMatrix = imageViewMatrix;
  }

  @Override
  protected void onPreExecute() {
    super.onPreExecute();

    if (mContext.isFinishing()) return;

    dialog = EditBaseActivity.getLoadingDialog(mContext, R.string.saving_image, false);
    dialog.show();
  }

  @Override
  protected Bitmap doInBackground(Bitmap... params) {
    // System.out.println("保存贴图!");
    Matrix touchMatrix = imageViewMatrix;

    Bitmap resultBit = Bitmap.createBitmap(params[0]).copy(Bitmap.Config.RGB_565, true);
    Canvas canvas = new Canvas(resultBit);

    float[] data = new float[9];
    touchMatrix.getValues(data); // 底部图片变化记录矩阵原始数据
    Matrix3 cal = new Matrix3(data); // 辅助矩阵计算类
    Matrix3 inverseMatrix = cal.inverseMatrix(); // 计算逆矩阵
    Matrix m = new Matrix();
    m.setValues(inverseMatrix.getValues());

    handleImage(canvas, m);

    // BitmapUtils.saveBitmap(resultBit, mContext.saveFilePath);
    return resultBit;
  }

  @Override
  protected void onCancelled() {
    super.onCancelled();
    dialog.dismiss();
  }

  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  @Override
  protected void onCancelled(Bitmap result) {
    super.onCancelled(result);
    dialog.dismiss();
  }

  @Override
  protected void onPostExecute(Bitmap result) {
    super.onPostExecute(result);
    onPostResult(result);
    dialog.dismiss();
  }

  public abstract void handleImage(Canvas canvas, Matrix m);

  public abstract void onPostResult(Bitmap result);
} // end class
