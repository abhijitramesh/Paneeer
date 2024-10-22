package org.amfoss.paneeer.editor.fragment;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.fragment.app.DialogFragment;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;

import org.amfoss.paneeer.Paneeer;
import org.amfoss.paneeer.R;
import org.amfoss.paneeer.editor.EditImageActivity;
import org.amfoss.paneeer.editor.font.FontPickerDialog;
import org.amfoss.paneeer.editor.task.StickerTask;
import org.amfoss.paneeer.editor.view.TextStickerView;
import org.amfoss.paneeer.gallery.util.ColorPalette;

import me.jfenn.colorpickerdialog.dialogs.ColorPickerDialog;

public class AddTextFragment extends BaseEditFragment
    implements TextWatcher, FontPickerDialog.FontPickerDialogListener {
  private View mainView;

  private EditText mInputText;
  private ImageView mTextColorSelector;
  private TextStickerView mTextStickerView;

  private InputMethodManager imm;
  private SaveTextStickerTask mSaveTask;
  private String text = null;

  public static AddTextFragment newInstance() {
    return new AddTextFragment();
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
    mainView = inflater.inflate(R.layout.fragment_edit_image_add_text, null);
    return mainView;
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    if (activity.mainBitmap == null) {
      return;
    }
    mTextStickerView = getActivity().findViewById(R.id.text_sticker_panel);

    View cancel = mainView.findViewById(R.id.text_cancel);
    View apply = mainView.findViewById(R.id.text_apply);
    ImageButton ibFontChoice = mainView.findViewById(R.id.text_font);

    ((ImageButton) cancel).setColorFilter(Color.BLACK);
    ((ImageButton) apply).setColorFilter(Color.BLACK);

    if (savedInstanceState != null) {
      text = savedInstanceState.getString("Edit Text");
    }

    mInputText = mainView.findViewById(R.id.text_input);
    mTextColorSelector = mainView.findViewById(R.id.text_color);
    mTextColorSelector.setImageDrawable(
        new IconicsDrawable(activity).icon(GoogleMaterial.Icon.gmd_format_color_fill).sizeDp(24));

    cancel.setOnClickListener(new BackToMenuClick());
    apply.setOnClickListener(
        new OnClickListener() {
          @Override
          public void onClick(View v) {
            applyTextImage();
          }
        });
    mTextColorSelector.setOnClickListener(new SelectColorBtnClick());
    mInputText.addTextChangedListener(this);
    if (text != null) {
      mInputText.setText(text);
    }
    boolean focus = mInputText.requestFocus();
    if (focus) {
      imm.showSoftInput(mInputText, InputMethodManager.SHOW_IMPLICIT);
    }
    mTextStickerView.setEditText(mInputText);
    onShow();

    ibFontChoice.setOnClickListener(
        new OnClickListener() {
          @Override
          public void onClick(View v) {
            showFontChoiceBox();
          }
        });
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putString("Edit Text", mInputText.getText().toString());
    // outState.putString("text", text);
  }

  private void showFontChoiceBox() {
    DialogFragment dialogFragment = FontPickerDialog.newInstance(this);
    dialogFragment.show(getFragmentManager(), "fontPicker");
  }

  @Override
  public void afterTextChanged(Editable s) {
    String text = s.toString().trim();
    mTextStickerView.setText(text);
  }

  @Override
  public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

  @Override
  public void onTextChanged(CharSequence s, int start, int before, int count) {}

  @Override
  public void onFontSelected(FontPickerDialog dialog) {
    mTextStickerView.setTextTypeFace(Typeface.createFromFile(dialog.getSelectedFont()));
  }

  private final class SelectColorBtnClick implements OnClickListener {
    @Override
    public void onClick(View v) {
      textColorDialog();
    }
  }

  private void textColorDialog() {
    final ColorPickerDialog colorPickerDialog =
        new ColorPickerDialog()
            .withPresets(ColorPalette.getAccentColors(activity.getApplicationContext()))
            .withTitle(getString(R.string.text_color_title))
            .withListener(
                (pickerView, color) -> {
                  mTextColorSelector.setColorFilter(color);
                  changeTextColor(color);
                });
    colorPickerDialog.show(getChildFragmentManager(), "ColorPicker");
  }

  private void changeTextColor(int newColor) {
    mTextStickerView.setTextColor(newColor);
  }

  public void hideInput() {
    if (getActivity() != null && getActivity().getCurrentFocus() != null && isInputMethodShow()) {
      imm.hideSoftInputFromWindow(
          getActivity().getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }
  }

  public boolean isInputMethodShow() {
    return imm.isActive();
  }

  private final class BackToMenuClick implements OnClickListener {
    @Override
    public void onClick(View v) {
      backToMain();
    }
  }

  public void backToMain() {
    hideInput();
    EditImageActivity.mode = EditImageActivity.MODE_WRITE;
    activity.writeFragment.clearSelection();
    activity.changeBottomFragment(EditImageActivity.MODE_MAIN);
    activity.mainImage.setVisibility(View.VISIBLE);
    mTextStickerView.clearTextContent();
    mTextStickerView.setVisibility(View.GONE);
  }

  @Override
  public void onShow() {
    activity.changeMode(EditImageActivity.MODE_TEXT);
    activity.mainImage.setImageBitmap(activity.mainBitmap);
    getmTextStickerView().mainImage = activity.mainImage;
    getmTextStickerView().mainBitmap = activity.mainBitmap;
    mTextStickerView.setVisibility(View.VISIBLE);
    mInputText.clearFocus();
  }

  public TextStickerView getmTextStickerView() {
    return mTextStickerView;
  }

  public void applyTextImage() {
    if (mSaveTask != null) {
      mSaveTask.cancel(true);
    }

    mSaveTask = new SaveTextStickerTask(activity, activity.mainImage.getImageViewMatrix());
    mSaveTask.execute(activity.mainBitmap);
  }

  private final class SaveTextStickerTask extends StickerTask {

    public SaveTextStickerTask(EditImageActivity activity, Matrix imageViewMatrix) {
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
      mTextStickerView.drawText(
          canvas,
          mTextStickerView.layout_x,
          mTextStickerView.layout_y,
          mTextStickerView.mScale,
          mTextStickerView.mRotateAngle);
      canvas.restore();
    }

    @Override
    public void onPostResult(Bitmap result) {
      mTextStickerView.clearTextContent();
      mTextStickerView.resetView();
      activity.changeMainBitmap(result);
      backToMain();
    }
  }

  @Override
  public void onDetach() {
    super.onDetach();
    resetTextStickerView();
  }

  private void resetTextStickerView() {
    if (null != mTextStickerView) {
      mTextStickerView.clearTextContent();
      mTextStickerView.setVisibility(View.GONE);
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (mSaveTask != null && !mSaveTask.isCancelled()) {
      mSaveTask.cancel(true);
    }
    Paneeer.getRefWatcher(getActivity()).watch(this);
  }
}
