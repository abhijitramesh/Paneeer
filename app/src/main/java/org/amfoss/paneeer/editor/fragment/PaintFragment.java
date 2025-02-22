package org.amfoss.paneeer.editor.fragment;

import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.SeekBar;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.amfoss.paneeer.Paneeer;
import org.amfoss.paneeer.R;
import org.amfoss.paneeer.editor.EditImageActivity;
import org.amfoss.paneeer.editor.adapter.ColorListAdapter;
import org.amfoss.paneeer.editor.task.StickerTask;
import org.amfoss.paneeer.editor.ui.ColorPicker;
import org.amfoss.paneeer.editor.view.CustomPaintView;
import org.amfoss.paneeer.editor.view.PaintModeView;
import org.amfoss.paneeer.gallery.util.ColorPalette;

import me.jfenn.colorpickerdialog.dialogs.ColorPickerDialog;

public class PaintFragment extends BaseEditFragment
    implements View.OnClickListener, ColorListAdapter.IColorListAction {

  private View mainView;
  private View cancel, apply;
  private PaintModeView mPaintModeView;
  private RecyclerView mColorListView;
  private ColorListAdapter mColorAdapter;
  private View popView;

  private CustomPaintView mPaintView;

  private ColorPicker mColorPicker;

  private PopupWindow setStokenWidthWindow;
  private SeekBar mStokenWidthSeekBar;

  private ImageView mEraserView;

  public boolean isEraser = false;

  private SaveCustomPaintTask mSavePaintImageTask;

  private static int mStokeColor = Integer.MIN_VALUE;
  private static float mStokeWidth = Integer.MIN_VALUE;

  public int[] mPaintColors = {
    Color.BLACK,
    Color.DKGRAY,
    Color.GRAY,
    Color.LTGRAY,
    Color.WHITE,
    Color.RED,
    Color.GREEN,
    Color.BLUE,
    Color.YELLOW,
    Color.CYAN,
    Color.MAGENTA
  };

  public static PaintFragment newInstance() {
    PaintFragment fragment = new PaintFragment();
    return fragment;
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    mainView = inflater.inflate(R.layout.fragment_edit_paint, null);
    return mainView;
  }

  @Override
  public void onDetach() {
    resetPaintView();
    super.onDetach();
  }

  private void resetPaintView() {
    if (null != mPaintView && activity.mainBitmap != null) {
      mPaintView.reset();
      mPaintView.setVisibility(View.GONE);
    }
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    mPaintView = getActivity().findViewById(R.id.custom_paint_view);

    cancel = mainView.findViewById(R.id.paint_cancel);
    apply = mainView.findViewById(R.id.paint_apply);

    mPaintModeView = mainView.findViewById(R.id.paint_thumb);
    mColorListView = mainView.findViewById(R.id.paint_color_list);
    mEraserView = mainView.findViewById(R.id.paint_eraser);

    cancel.setOnClickListener(this);
    apply.setOnClickListener(this);

    mColorPicker = new ColorPicker(getActivity(), 255, 0, 0);
    initColorListView();
    mPaintModeView.setOnClickListener(this);

    initStokeWidthPopWindow();

    if (savedInstanceState != null) {
      mPaintView.mDrawBit = savedInstanceState.getParcelable("Draw Bit");
      CustomPaintView.mPaintCanvas = new Canvas(mPaintView.mDrawBit);
      mStokeColor = savedInstanceState.getInt("Stoke Color");
      mStokeWidth = savedInstanceState.getFloat("Stoke Width");
    }
    if (mStokeColor != Integer.MIN_VALUE && mStokeWidth != Integer.MIN_VALUE) {
      mPaintModeView.setPaintStrokeColor(mStokeColor);
      mPaintModeView.setPaintStrokeWidth(mStokeWidth);
      setPaintColor(mStokeColor);
      mPaintModeView.setPaintStrokeWidth(mStokeWidth);
    }

    mEraserView.setOnClickListener(this);
    updateEraserView();
    onShow();
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putFloat("Stoke Width", mPaintModeView.getStokenWidth());
    outState.putInt("Stoke Color", mPaintModeView.getStokenColor());
    outState.putParcelable("Draw Bit", mPaintView.mDrawBit);
  }

  private void initColorListView() {

    mColorListView.setHasFixedSize(false);

    LinearLayoutManager stickerListLayoutManager = new LinearLayoutManager(activity);
    if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
      stickerListLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
    }
    if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
      stickerListLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
    }
    mColorListView.setLayoutManager(stickerListLayoutManager);
    mColorAdapter = new ColorListAdapter(this, mPaintColors, this);
    mColorListView.setAdapter(mColorAdapter);
  }

  @Override
  public void onClick(View v) {
    if (v == cancel) { // back button click
      backToMain();
    } else if (v == mPaintModeView) {
      setStokeWidth();
    } else if (v == mEraserView) {
      toggleEraserView();
    } else if (v == apply) {
      applyPaintImage();
    }
  }

  public void backToMain() {
    EditImageActivity.mode = EditImageActivity.MODE_WRITE;
    activity.changeBottomFragment(EditImageActivity.MODE_MAIN);
    activity.writeFragment.clearSelection();
    activity.mainImage.setVisibility(View.VISIBLE);
    this.mPaintView.reset();
    this.mPaintView.setVisibility(View.GONE);
  }

  public void onShow() {
    if (activity.mainBitmap == null) {
      getActivity()
          .getSupportFragmentManager()
          .beginTransaction()
          .remove(PaintFragment.this)
          .commit();
      return;
    }
    activity.changeMode(EditImageActivity.MODE_PAINT);
    activity.mainImage.setImageBitmap(activity.mainBitmap);
    activity.mPaintView.mainBitmap = activity.mainBitmap;
    activity.mPaintView.mainImage = activity.mainImage;
    this.mPaintView.setVisibility(View.VISIBLE);
  }

  @Override
  public void onColorSelected(int position, int color) {
    setPaintColor(color);
  }

  @Override
  public void onMoreSelected(int position) {
    selectPaintColor();
  }

  private void selectPaintColor() {
    final ColorPickerDialog colorPickerDialog =
        new ColorPickerDialog()
            .withPresets(ColorPalette.getAccentColors(activity.getApplicationContext()))
            .withTitle(getString(R.string.paint_color_title))
            .withListener((pickerView, color) -> setPaintColor(color));
    colorPickerDialog.show(getChildFragmentManager(), "ColorPicker");
  }

  protected void setPaintColor(final int paintColor) {
    mPaintModeView.setPaintStrokeColor(paintColor);

    updatePaintView();
  }

  private void updatePaintView() {
    isEraser = false;
    updateEraserView();

    this.mPaintView.setColor(mPaintModeView.getStokenColor());
    this.mPaintView.setWidth(mPaintModeView.getStokenWidth());
  }

  protected void setStokeWidth() {
    if (popView.getMeasuredHeight() == 0) {
      popView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
    }

    mStokenWidthSeekBar.setMax(mPaintModeView.getMeasuredWidth() / 2);

    mStokenWidthSeekBar.setProgress((int) mPaintModeView.getStokenWidth());

    mStokenWidthSeekBar.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          @Override
          public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            mPaintModeView.setPaintStrokeWidth(progress);
            updatePaintView();
          }

          @Override
          public void onStartTrackingTouch(SeekBar seekBar) {}

          @Override
          public void onStopTrackingTouch(SeekBar seekBar) {}
        });

    setStokenWidthWindow.showAtLocation(
        this.getActivity().getWindow().getDecorView(),
        Gravity.NO_GRAVITY,
        0,
        activity.mainImage.getHeight() - popView.getMeasuredHeight() / 2);
  }

  private void initStokeWidthPopWindow() {
    popView = LayoutInflater.from(activity).inflate(R.layout.view_set_stoke_width, null);
    setStokenWidthWindow =
        new PopupWindow(
            popView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

    mStokenWidthSeekBar = popView.findViewById(R.id.stoke_width_seekbar);

    setStokenWidthWindow.setFocusable(true);
    setStokenWidthWindow.setOutsideTouchable(true);
    setStokenWidthWindow.setBackgroundDrawable(new BitmapDrawable());
    setStokenWidthWindow.setAnimationStyle(R.style.popwin_anim_style);

    mPaintModeView.setPaintStrokeColor(Color.RED);
    mPaintModeView.setPaintStrokeWidth(10);

    updatePaintView();
  }

  private void toggleEraserView() {
    isEraser = !isEraser;
    updateEraserView();
  }

  private void updateEraserView() {
    mEraserView.setImageResource(isEraser ? R.drawable.eraser_seleced : R.drawable.eraser_normal);
    mPaintView.setEraser(isEraser);
  }

  public void applyPaintImage() {
    if (mSavePaintImageTask != null && !mSavePaintImageTask.isCancelled()) {
      mSavePaintImageTask.cancel(true);
    }

    mSavePaintImageTask =
        new SaveCustomPaintTask(activity, activity.mainImage.getImageViewMatrix());
    mSavePaintImageTask.execute(activity.mainBitmap);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (mSavePaintImageTask != null && !mSavePaintImageTask.isCancelled()) {
      mSavePaintImageTask.cancel(true);
    }
    Paneeer.getRefWatcher(getActivity()).watch(this);
  }

  private final class SaveCustomPaintTask extends StickerTask {

    public SaveCustomPaintTask(EditImageActivity activity, Matrix imageViewMatrix) {
      super(activity, imageViewMatrix);
    }

    @Override
    public void handleImage(Canvas canvas, Matrix m) {
      float[] f = new float[9];
      m.getValues(f);
      int dx = (int) f[Matrix.MTRANS_X];
      int dy = (int) f[Matrix.MTRANS_Y];
      float scale_x = f[Matrix.MSCALE_X];
      float scale_y = f[Matrix.MSCALE_Y];
      canvas.save();
      canvas.translate(dx, dy);
      canvas.scale(scale_x, scale_y);

      if (mPaintView.getPaintBit() != null) {
        canvas.drawBitmap(mPaintView.getPaintBit(), 0, 0, null);
      }
      canvas.restore();
    }

    @Override
    public void onPostResult(Bitmap result) {
      mPaintView.reset();
      activity.changeMainBitmap(result);
      backToMain();
    }
  }
}
