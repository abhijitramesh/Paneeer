package org.amfoss.paneeer.editor;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import org.amfoss.paneeer.R;
import org.amfoss.paneeer.editor.fragment.AddTextFragment;
import org.amfoss.paneeer.editor.fragment.CropFragment;
import org.amfoss.paneeer.editor.fragment.FrameFragment;
import org.amfoss.paneeer.editor.fragment.MainMenuFragment;
import org.amfoss.paneeer.editor.fragment.PaintFragment;
import org.amfoss.paneeer.editor.fragment.RecyclerMenuFragment;
import org.amfoss.paneeer.editor.fragment.RotateFragment;
import org.amfoss.paneeer.editor.fragment.SliderFragment;
import org.amfoss.paneeer.editor.fragment.StickersFragment;
import org.amfoss.paneeer.editor.fragment.TwoItemFragment;
import org.amfoss.paneeer.editor.utils.BitmapUtils;
import org.amfoss.paneeer.editor.utils.FileUtil;
import org.amfoss.paneeer.editor.view.CropImageView;
import org.amfoss.paneeer.editor.view.CustomPaintView;
import org.amfoss.paneeer.editor.view.RotateImageView;
import org.amfoss.paneeer.editor.view.StickerView;
import org.amfoss.paneeer.editor.view.TextStickerView;
import org.amfoss.paneeer.editor.view.imagezoom.ImageViewTouch;
import org.amfoss.paneeer.editor.view.imagezoom.ImageViewTouchBase;
import org.amfoss.paneeer.gallery.util.AlertDialogsHelper;
import org.amfoss.paneeer.gallery.util.ColorPalette;
import org.amfoss.paneeer.utilities.ActivitySwitchHelper;
import org.amfoss.paneeer.utilities.SnackBarHandler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Called from SingleMediaActivity when the user selects the 'edit' option in the toolbar overflow
 * menu.
 */
public class EditImageActivity extends EditBaseActivity
    implements View.OnClickListener, View.OnTouchListener {
  public static final String FILE_PATH = "extra_input";
  public static final String EXTRA_OUTPUT = "extra_output";
  public static final String IMAGE_IS_EDIT = "image_is_edit";

  /** Different edit modes. */
  public static final int MODE_MAIN = 0;

  public static final int MODE_SLIDER = 1;
  public static final int MODE_FILTERS = 2;
  public static final int MODE_ENHANCE = 3;
  public static final int MODE_ADJUST = 4;
  public static final int MODE_STICKER_TYPES = 5;
  public static final int MODE_WRITE = 6;

  public static final int MODE_STICKERS = 7;
  public static final int MODE_CROP = 8;

  public static final int MODE_ROTATE = 9;
  public static final int MODE_TEXT = 10;
  public static final int MODE_PAINT = 11;
  public static final int MODE_FRAME = 12;

  public String filePath;
  public String saveFilePath;
  private int imageWidth, imageHeight;

  public static int mode;
  public static int effectType;
  private int initalBottomFragment = 0;
  private int initalMiddleFragment = 2;
  /** Number of times image has been edited. Indicates whether image has been edited or not. */
  protected int mOpTimes = 0;

  protected boolean isBeenSaved = false;

  private LoadImageTask mLoadImageTask;

  private EditImageActivity mContext;
  public Bitmap mainBitmap;
  private Bitmap originalBitmap;
  public ImageViewTouch mainImage;
  View parentLayout;
  ImageButton cancel;
  ImageButton save;
  ImageButton bef_aft;
  ImageButton undo;
  ImageButton redo;
  ProgressBar progressBar;
  public StickerView mStickerView; // Texture layers View
  public CropImageView mCropPanel; // Cut operation control
  public RotateImageView mRotatePanel; // Rotation operation controls
  public TextStickerView mTextStickerView; // Text display map View
  public CustomPaintView mPaintView; // drawing paint

  private SaveImageTask mSaveImageTask;
  private int requestCode;
  private int currentShowingIndex = -1;

  public ArrayList<Bitmap> bitmapsForUndo;
  public MainMenuFragment mainMenuFragment = new MainMenuFragment();
  public RecyclerMenuFragment filterFragment, enhanceFragment, stickerTypesFragment;
  public StickersFragment stickersFragment;
  public SliderFragment sliderFragment;
  public TwoItemFragment writeFragment, adjustFragment;
  public AddTextFragment addTextFragment;
  public PaintFragment paintFragment;
  public CropFragment cropFragment;
  public RotateFragment rotateFragment;
  public FrameFragment frameFragment;
  private static String stickerType;
  private boolean exitDialog = false;
  private boolean exitWithoutChanges = false;
  private boolean modeChangesExit = false;
  private int modeCheck = -1;
  private int messageCheck = -1;
  private boolean check = false;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getSupportActionBar() != null) getSupportActionBar().hide();

    if (mainBitmap != null) {
      mainBitmap.recycle();
      mainBitmap = null;
      System.gc();
    }

    checkInitImageLoader();
    setContentView(R.layout.activity_image_edit);
    mainImage = findViewById(R.id.main_image);
    parentLayout = findViewById(R.id.parentLayout);
    cancel = findViewById(R.id.edit_cancel);
    save = findViewById(R.id.edit_save);
    bef_aft = findViewById(R.id.edit_befaft);
    undo = findViewById(R.id.edit_undo);
    redo = findViewById(R.id.edit_redo);
    progressBar = findViewById(R.id.progress_bar_edit);
    mStickerView = findViewById(R.id.sticker_panel); // Texture layers View
    mCropPanel = findViewById(R.id.crop_panel); // Cut operation control
    mRotatePanel = findViewById(R.id.rotate_panel); // Rotation operation controls
    mTextStickerView = findViewById(R.id.text_sticker_panel); // Text display map View
    mPaintView = findViewById(R.id.custom_paint_view); // drawing paint
    initView();
    if (savedInstanceState != null) {
      mode = savedInstanceState.getInt(getString(R.string.frag_prev));
      initalMiddleFragment = savedInstanceState.getInt(getString(R.string.frag_prev_mid));
      initalBottomFragment = savedInstanceState.getInt(getString(R.string.frag_prev_bottom));
      exitDialog = savedInstanceState.getBoolean("Dialogshown", false);
      exitWithoutChanges = savedInstanceState.getBoolean("exitWithoutChanges", false);
      modeChangesExit = savedInstanceState.getBoolean("modeChanges", false);
      modeCheck = savedInstanceState.getInt("checkMode", -1);
      messageCheck = savedInstanceState.getInt("checkString", -1);
      check = true;
      mainBitmap = savedInstanceState.getParcelable("Edited Bitmap");
      mOpTimes = savedInstanceState.getInt("numberOfEdits");
      Fragment restoredAddTextFragment =
          getSupportFragmentManager().getFragment(savedInstanceState, "addTextFragment");
      if (restoredAddTextFragment != null) {
        addTextFragment = (AddTextFragment) restoredAddTextFragment;
      }
    }
    if (exitDialog) {
      onSaveTaskDone();
    } else if (exitWithoutChanges) {
      noChangesExitDialog();
    } else if (modeChangesExit) {
      showDiscardChangesDialog(modeCheck, messageCheck);
    }
    getData();
  }

  /**
   * Calleter onCreate() when the activity is first started. Loads the initial default fragments.
   */
  private void setInitialFragments() {
    getSupportFragmentManager()
        .beginTransaction()
        .replace(R.id.controls_container, getFragment(initalBottomFragment))
        .commit();

    changeMiddleFragment(initalMiddleFragment);
    mode = initalMiddleFragment;
    setButtonsVisibility();
  }

  /** Gets the image to be loaded from the intent and displays this image. */
  private void getData() {
    if (null != getIntent() && null != getIntent().getExtras()) {
      Bundle bundle = getIntent().getExtras();
      filePath = bundle.getString(FILE_PATH);
      saveFilePath = bundle.getString(EXTRA_OUTPUT);
      requestCode = bundle.getInt(getString(R.string.request_code), 1);
      loadImage(filePath);
      return;
    }
    SnackBarHandler.create(parentLayout, getString(R.string.image_invalid)).show();
  }

  /** Called from onCreate(). Initializes all view objects and fragments to be used. */
  private void initView() {
    mContext = this;
    DisplayMetrics metrics = getResources().getDisplayMetrics();
    imageWidth = metrics.widthPixels / 2;
    imageHeight = metrics.heightPixels / 2;

    bitmapsForUndo = new ArrayList<>();
    progressBar
        .getIndeterminateDrawable()
        .setColorFilter(ColorPalette.getLighterColor(getPrimaryColor()), PorterDuff.Mode.SRC_ATOP);

    cancel.setOnClickListener(this);
    save.setOnClickListener(this);
    undo.setOnClickListener(this);
    redo.setOnClickListener(this);
    bef_aft.setOnTouchListener(this);

    mode = MODE_FILTERS;

    mainMenuFragment = MainMenuFragment.newInstance();
    sliderFragment = SliderFragment.newInstance();
    filterFragment = RecyclerMenuFragment.newInstance(MODE_FILTERS);
    enhanceFragment = RecyclerMenuFragment.newInstance(MODE_ENHANCE);
    stickerTypesFragment = RecyclerMenuFragment.newInstance(MODE_STICKER_TYPES);
    adjustFragment = TwoItemFragment.newInstance(MODE_ADJUST);
    writeFragment = TwoItemFragment.newInstance(MODE_WRITE);
    addTextFragment = AddTextFragment.newInstance();
    paintFragment = PaintFragment.newInstance();
    cropFragment = CropFragment.newInstance();
    rotateFragment = RotateFragment.newInstance();
  }

  /**
   * Get current editing mode.
   *
   * @return the editing mode.
   */
  public static int getMode() {
    return mode;
  }

  public void changeMode(int to_mode) {
    EditImageActivity.mode = to_mode;
    highLightSelectedOption(to_mode);
  }

  private void highLightSelectedOption(int mode) {
    switch (mode) {
      case MODE_FILTERS:
      case MODE_ENHANCE:
      case MODE_ADJUST:
      case MODE_STICKER_TYPES:
      case MODE_FRAME:
      case MODE_WRITE:
        mainMenuFragment.highLightSelectedOption(mode);
        break;
      case MODE_STICKERS:
      case MODE_TEXT:
      case MODE_PAINT:
      case MODE_CROP:
      case MODE_ROTATE:
      case MODE_SLIDER:
    }
  }

  /**
   * Get the fragment corresponding to current editing mode.
   *
   * @param index integer corresponding to editing mode.
   * @return Fragment of current editing mode.
   */
  public Fragment getFragment(int index) {
    switch (index) {
      case MODE_MAIN:
        return mainMenuFragment;
      case MODE_SLIDER:
        sliderFragment = SliderFragment.newInstance();
        return sliderFragment;
      case MODE_FILTERS:
        return filterFragment;
      case MODE_ENHANCE:
        return enhanceFragment;
      case MODE_STICKER_TYPES:
        return stickerTypesFragment;
      case MODE_STICKERS:
        stickersFragment = StickersFragment.newInstance(addStickerImages(stickerType));
        return stickersFragment;
      case MODE_WRITE:
        return writeFragment;
      case MODE_ADJUST:
        return adjustFragment;
      case MODE_TEXT:
        return addTextFragment;
      case MODE_PAINT:
        return paintFragment;
      case MODE_CROP:
        return cropFragment;
      case MODE_ROTATE:
        return rotateFragment;
      case MODE_FRAME:
        return frameFragment = FrameFragment.newInstance(mainBitmap);
    }
    return mainMenuFragment;
  }

  /**
   * Called when a particular option in the preview_container is selected. It reassigns the
   * controls_container. It displays options and tools for the selected editing mode.
   *
   * @param index integer corresponding to the current editing mode.
   */
  public void changeBottomFragment(int index) {
    initalBottomFragment = index;
    getSupportFragmentManager()
        .beginTransaction()
        .replace(R.id.controls_container, getFragment(index))
        .commit();

    setButtonsVisibility();
  }

  /** Handles the visibility of the 'save' button. */
  private void setButtonsVisibility() {
    save.setVisibility(View.VISIBLE);
    bef_aft.setVisibility(View.VISIBLE);
    if (currentShowingIndex > 0) {
      undo.setColorFilter(Color.BLACK);
      undo.setEnabled(true);
    } else {
      undo.setColorFilter(getResources().getColor(R.color.md_grey_300));
      undo.setEnabled(false);
    }
    if (currentShowingIndex + 1 < bitmapsForUndo.size()) {
      redo.setColorFilter(Color.BLACK);
      redo.setEnabled(true);
    } else {
      redo.setColorFilter(getResources().getColor(R.color.md_grey_300));
      redo.setEnabled(false);
    }

    switch (mode) {
      case MODE_STICKERS:
      case MODE_CROP:
      case MODE_ROTATE:
      case MODE_TEXT:
      case MODE_PAINT:
        save.setVisibility(View.INVISIBLE);
        bef_aft.setVisibility(View.INVISIBLE);
        break;
      case MODE_SLIDER:
        save.setVisibility(View.INVISIBLE);
        break;
    }
  }

  public void setEffectType(int type, int mode) {
    effectType = 100 * mode + type;
  }

  /**
   * Is called when an editing mode is selected in the control_container. Reassigns the
   * preview_container according to the editing mode selected.
   *
   * @param index integer representing selected editing mode
   */
  public void changeMiddleFragment(int index) {
    initalMiddleFragment = index;
    getSupportFragmentManager()
        .beginTransaction()
        .replace(R.id.preview_container, getFragment(index))
        .commit();
  }

  public void changeMainBitmap(Bitmap newBit) {
    if (mainBitmap != null) {
      if (!mainBitmap.isRecycled()) {
        mainBitmap.recycle();
      }
    }
    mainBitmap = newBit;
    mainImage.setImageBitmap(mainBitmap);
    mainImage.setDisplayType(ImageViewTouchBase.DisplayType.FIT_TO_SCREEN);
    addToUndoList();
    setButtonsVisibility();
    increaseOpTimes();
  }

  private void addToUndoList() {
    try {
      TODO: // implement a more efficient way, like storing only the difference of bitmaps or
      // steps followed to edit
      recycleBitmapList(++currentShowingIndex);
      bitmapsForUndo.add(mainBitmap.copy(mainBitmap.getConfig(), true));
    } catch (OutOfMemoryError error) {
      /**
       * When outOfMemory exception throws then to make space, remove the last edited step from list
       * and added the new operation in the end.
       */
      bitmapsForUndo.get(1).recycle();
      bitmapsForUndo.remove(1);
      bitmapsForUndo.add(mainBitmap.copy(mainBitmap.getConfig(), true));
    }
  }

  private void recycleBitmapList(int fromIndex) {
    while (fromIndex < bitmapsForUndo.size()) {
      bitmapsForUndo.get(fromIndex).recycle();
      bitmapsForUndo.remove(fromIndex);
    }
  }

  private Bitmap getUndoBitmap() {
    if (currentShowingIndex - 1 >= 0) currentShowingIndex -= 1;
    else currentShowingIndex = 0;

    return bitmapsForUndo
        .get(currentShowingIndex)
        .copy(bitmapsForUndo.get(currentShowingIndex).getConfig(), true);
  }

  private Bitmap getRedoBitmap() {
    if (currentShowingIndex + 1 <= bitmapsForUndo.size()) currentShowingIndex += 1;
    else currentShowingIndex = bitmapsForUndo.size() - 1;

    return bitmapsForUndo
        .get(currentShowingIndex)
        .copy(bitmapsForUndo.get(currentShowingIndex).getConfig(), true);
  }

  private void onUndoPressed() {
    if (mainBitmap != null) {
      if (!mainBitmap.isRecycled()) {
        mainBitmap.recycle();
      }
    }
    mainBitmap = getUndoBitmap();
    mainImage.setImageBitmap(mainBitmap);
    mainImage.setDisplayType(ImageViewTouchBase.DisplayType.FIT_TO_SCREEN);
    setButtonsVisibility();
  }

  private void onRedoPressed() {

    if (mainBitmap != null) {
      if (!mainBitmap.isRecycled()) {
        mainBitmap.recycle();
      }
    }
    mainBitmap = getRedoBitmap();
    mainImage.setImageBitmap(mainBitmap);
    mainImage.setDisplayType(ImageViewTouchBase.DisplayType.FIT_TO_SCREEN);
    setButtonsVisibility();
  }

  /**
   * Load the image from filepath into mainImage imageView.
   *
   * @param filepath The image to be loaded.
   */
  public void loadImage(String filepath) {
    if (mLoadImageTask != null) {
      mLoadImageTask.cancel(true);
    }
    if (check && mOpTimes > 0) {
      check = false;
      mainImage.setImageBitmap(mainBitmap);
      mainImage.setDisplayType(ImageViewTouchBase.DisplayType.FIT_TO_SCREEN);
      originalBitmap = mainBitmap.copy(mainBitmap.getConfig(), true);
      addToUndoList();
      setInitialFragments();
    } else {
      mLoadImageTask = new LoadImageTask();
      mLoadImageTask.execute(filepath);
    }
  }

  protected void doSaveImage() {
    if (mSaveImageTask != null) {
      mSaveImageTask.cancel(true);
    }

    mSaveImageTask = new SaveImageTask();
    mSaveImageTask.execute(mainBitmap);
  }

  // Increment no. of times the image has been edited
  public void increaseOpTimes() {
    mOpTimes++;
    isBeenSaved = false;
  }

  public void resetOpTimes() {
    isBeenSaved = true;
  }

  public void showProgressBar() {
    progressBar.setVisibility(View.VISIBLE);
  }

  public void hideProgressBar() {
    progressBar.setVisibility(View.GONE);
  }

  /**
   * Allow exit only if image has not been modified or has been modified and saved.
   *
   * @return true if can exit, false if cannot.
   */
  public boolean canAutoExit() {
    return isBeenSaved || mOpTimes == 0;
  }

  protected void onSaveTaskDone() {
    if (mOpTimes > 0) {
      FileUtil.albumUpdate(this, saveFilePath);
      finish();
    } else if (mOpTimes <= 0 && requestCode == 1) {
      finish();
    } else {
      exitDialog = true;
      final AlertDialog.Builder discardChangesDialogBuilder =
          new AlertDialog.Builder(EditImageActivity.this, getDialogStyle());
      AlertDialogsHelper.getTextDialog(
          EditImageActivity.this,
          discardChangesDialogBuilder,
          R.string.no_changes_made,
          R.string.exit_without_edit,
          null);
      discardChangesDialogBuilder.setPositiveButton(
          getString(R.string.confirm).toUpperCase(),
          new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              finish();
            }
          });
      discardChangesDialogBuilder.setNegativeButton(
          getString(R.string.cancel).toUpperCase(),
          new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              if (dialog != null) {
                exitDialog = false;
                dialog.dismiss();
              }
            }
          });

      AlertDialog alertDialog = discardChangesDialogBuilder.create();
      alertDialog.show();
      AlertDialogsHelper.setButtonTextColor(
          new int[] {DialogInterface.BUTTON_POSITIVE, DialogInterface.BUTTON_NEGATIVE},
          getAccentColor(),
          alertDialog);
    }
  }

  private ArrayList<String> addStickerImages(String folderPath) {
    ArrayList<String> pathList = new ArrayList<>();
    try {
      String[] files = getAssets().list(folderPath);

      for (String name : files) {
        pathList.add(folderPath + File.separator + name);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return pathList;
  }

  public void setStickerType(String stickerType) {
    EditImageActivity.stickerType = stickerType;
  }

  @Override
  public boolean onTouch(View v, MotionEvent event) {
    if (R.id.edit_befaft == v.getId()) {
      if (MotionEvent.ACTION_DOWN == event.getAction()) {
        switch (mode) {
          case MODE_SLIDER:
            mainImage.setImageBitmap(mainBitmap);
            break;
          default:
            mainImage.setImageBitmap(originalBitmap);
        }
      } else if (MotionEvent.ACTION_UP == event.getAction()) {
        switch (mode) {
          case MODE_SLIDER:
            mainImage.setImageBitmap(sliderFragment.filterBit);
            break;
          default:
            mainImage.setImageBitmap(mainBitmap);
        }
      }
    }
    return true;
  }

  private final class LoadImageTask extends AsyncTask<String, Void, Bitmap> {
    @Override
    protected Bitmap doInBackground(String... params) {

      return BitmapUtils.getSampledBitmap(params[0], imageWidth, imageHeight);
    }

    @Override
    protected void onPostExecute(Bitmap result) {
      super.onPostExecute(result);
      mainBitmap = result;
      mainImage.setImageBitmap(result);
      mainImage.setDisplayType(ImageViewTouchBase.DisplayType.FIT_TO_SCREEN);
      originalBitmap = mainBitmap.copy(mainBitmap.getConfig(), true);
      addToUndoList();
      setInitialFragments();
    }
  }

  private final class SaveImageTask extends AsyncTask<Bitmap, Void, Boolean> {
    private Dialog dialog;

    @Override
    protected Boolean doInBackground(Bitmap... params) {
      if (TextUtils.isEmpty(saveFilePath)) return false;

      return BitmapUtils.saveBitmap(params[0], saveFilePath);
    }

    @Override
    protected void onCancelled() {
      super.onCancelled();
      dialog.dismiss();
    }

    @Override
    protected void onCancelled(Boolean result) {
      super.onCancelled(result);
      dialog.dismiss();
    }

    @Override
    protected void onPreExecute() {
      super.onPreExecute();
      dialog = getLoadingDialog(mContext, R.string.saving_image, false);
      dialog.show();
    }

    @Override
    protected void onPostExecute(Boolean result) {
      super.onPostExecute(result);
      dialog.dismiss();

      if (result) {
        resetOpTimes();
        onSaveTaskDone();
      } else {
        final AlertDialog.Builder discardChangesDialogBuilder =
            new AlertDialog.Builder(EditImageActivity.this, getDialogStyle());
        AlertDialogsHelper.getTextDialog(
            EditImageActivity.this,
            discardChangesDialogBuilder,
            R.string.save_error,
            R.string.permissions_error,
            null);
        discardChangesDialogBuilder.setPositiveButton(
            getString(R.string.ok).toUpperCase(),
            new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
              }
            });
        discardChangesDialogBuilder.setNegativeButton(
            getString(R.string.cancel).toUpperCase(),
            new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                Toast.makeText(getBaseContext(), R.string.no_save, Toast.LENGTH_LONG).show();
              }
            });

        AlertDialog alertDialog = discardChangesDialogBuilder.create();
        alertDialog.show();
        AlertDialogsHelper.setButtonTextColor(
            new int[] {DialogInterface.BUTTON_POSITIVE, DialogInterface.BUTTON_NEGATIVE},
            getAccentColor(),
            alertDialog);
      }
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putInt(getString(R.string.frag_prev_mid), initalMiddleFragment);
    outState.putInt(getString(R.string.frag_prev_bottom), initalBottomFragment);
    outState.putInt(getString(R.string.frag_prev), mode);
    outState.putBoolean("Dialogshown", exitDialog);
    outState.putBoolean("exitWithoutChanges", exitWithoutChanges);
    outState.putBoolean("modeChanges", modeChangesExit);
    outState.putInt("checkMode", modeCheck);
    outState.putInt("checkString", messageCheck);
    outState.putParcelable("Edited Bitmap", mainBitmap);
    outState.putInt("numberOfEdits", mOpTimes);
    if (addTextFragment.isAdded()) {
      getSupportFragmentManager().putFragment(outState, "addTextFragment", addTextFragment);
    }
  }

  @Override
  public void onBackPressed() {
    switch (mode) {
        // On pressing back, ask whether the user wants to discard changes or not
      case MODE_SLIDER:
        showDiscardChangesDialog(MODE_SLIDER, R.string.discard_enhance_message);
        return;
      case MODE_STICKERS:
        showDiscardChangesDialog(MODE_STICKERS, R.string.discard_stickers_message);
        return;
      case MODE_CROP:
        showDiscardChangesDialog(MODE_CROP, R.string.discard_crop_message);
        return;
      case MODE_ROTATE:
        showDiscardChangesDialog(MODE_ROTATE, R.string.discard_rotate_message);
        return;
      case MODE_TEXT:
        showDiscardChangesDialog(MODE_TEXT, R.string.discard_text_message);
        return;
      case MODE_PAINT:
        showDiscardChangesDialog(MODE_PAINT, R.string.discard_paint_message);
      case MODE_FRAME:
        if (canAutoExit()) {
          finish();
        } else {
          showDiscardChangesDialog(MODE_FRAME, R.string.discard_frame_mode_message);
        }
        return;
    }
    // if the image has not been edited or has been edited and saved.
    if (canAutoExit()) {
      finish();
    } else {
      exitWithoutChanges = true;
      final AlertDialog.Builder discardChangesDialogBuilder =
          new AlertDialog.Builder(EditImageActivity.this, getDialogStyle());
      AlertDialogsHelper.getTextDialog(
          EditImageActivity.this,
          discardChangesDialogBuilder,
          R.string.discard_changes_header,
          R.string.exit_without_save,
          null);
      discardChangesDialogBuilder.setPositiveButton(
          getString(R.string.confirm).toUpperCase(),
          new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              finish();
            }
          });
      discardChangesDialogBuilder.setNegativeButton(
          getString(R.string.cancel).toUpperCase(),
          new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              if (dialog != null) {
                exitWithoutChanges = false;
                dialog.dismiss();
              }
            }
          });
      discardChangesDialogBuilder.setNeutralButton(
          getString(R.string.save_action).toUpperCase(),
          new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              doSaveImage();
            }
          });

      AlertDialog alertDialog = discardChangesDialogBuilder.create();
      alertDialog.show();
      AlertDialogsHelper.setButtonTextColor(
          new int[] {
            DialogInterface.BUTTON_POSITIVE,
            DialogInterface.BUTTON_NEGATIVE,
            DialogInterface.BUTTON_NEUTRAL
          },
          getAccentColor(),
          alertDialog);
    }
  }

  private void noChangesExitDialog() {
    final AlertDialog.Builder discardChangesDialogBuilder =
        new AlertDialog.Builder(EditImageActivity.this, getDialogStyle());
    AlertDialogsHelper.getTextDialog(
        EditImageActivity.this,
        discardChangesDialogBuilder,
        R.string.discard_changes_header,
        R.string.exit_without_save,
        null);
    discardChangesDialogBuilder.setPositiveButton(
        getString(R.string.confirm).toUpperCase(),
        new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            finish();
          }
        });
    discardChangesDialogBuilder.setNegativeButton(
        getString(R.string.cancel).toUpperCase(),
        new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            if (dialog != null) {
              exitWithoutChanges = false;
              dialog.dismiss();
            }
          }
        });
    discardChangesDialogBuilder.setNeutralButton(
        getString(R.string.save_action).toUpperCase(),
        new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            doSaveImage();
          }
        });

    AlertDialog alertDialog = discardChangesDialogBuilder.create();
    alertDialog.show();
    AlertDialogsHelper.setButtonTextColor(
        new int[] {
          DialogInterface.BUTTON_POSITIVE,
          DialogInterface.BUTTON_NEGATIVE,
          DialogInterface.BUTTON_NEUTRAL
        },
        getAccentColor(),
        alertDialog);
  }

  private void showDiscardChangesDialog(final int editMode, @StringRes int message) {
    modeChangesExit = true;
    modeCheck = editMode;
    messageCheck = message;
    AlertDialog.Builder discardChangesDialogBuilder =
        new AlertDialog.Builder(EditImageActivity.this, getDialogStyle());
    AlertDialogsHelper.getTextDialog(
        EditImageActivity.this,
        discardChangesDialogBuilder,
        R.string.discard_changes_header,
        message,
        null);
    discardChangesDialogBuilder.setNegativeButton(
        getString(R.string.cancel).toUpperCase(),
        new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            if (dialog != null) {
              modeChangesExit = false;
              dialog.dismiss();
            }
          }
        });
    discardChangesDialogBuilder.setPositiveButton(
        getString(R.string.confirm).toUpperCase(),
        new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            switch (editMode) {
              case MODE_SLIDER:
                sliderFragment.backToMain();
                break;
              case MODE_STICKERS:
                stickersFragment.backToMain();
                break;
              case MODE_CROP:
                cropFragment.backToMain();
                break;
              case MODE_ROTATE:
                rotateFragment.backToMain();
                break;
              case MODE_TEXT:
                addTextFragment.backToMain();
                break;
              case MODE_PAINT:
                paintFragment.backToMain();
                break;
              case MODE_FRAME:
                frameFragment.backToMain();
                break;
              default:
                break;
            }
          }
        });
    AlertDialog alertDialog = discardChangesDialogBuilder.create();
    alertDialog.show();
    AlertDialogsHelper.setButtonTextColor(
        new int[] {DialogInterface.BUTTON_POSITIVE, DialogInterface.BUTTON_NEGATIVE},
        getAccentColor(),
        alertDialog);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (originalBitmap != null && !originalBitmap.isRecycled()) {
      originalBitmap.recycle();
      originalBitmap = null;
    }
    if (mLoadImageTask != null) {
      mLoadImageTask.cancel(true);
    }

    if (mSaveImageTask != null) {
      mSaveImageTask.cancel(true);
    }

    recycleBitmapList(0);
  }

  @Override
  public void onResume() {
    super.onResume();
    ActivitySwitchHelper.setContext(this);
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.edit_save:
        if (mOpTimes != 0) doSaveImage();
        else onSaveTaskDone();
        break;
      case R.id.edit_cancel:
        onBackPressed();
        break;
      case R.id.edit_undo:
        onUndoPressed();
        break;
      case R.id.edit_redo:
        onRedoPressed();
        break;
    }
  }
}
