package org.amfoss.paneeer.editor.fragment;

import android.content.DialogInterface;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

import org.amfoss.paneeer.Paneeer;
import org.amfoss.paneeer.R;
import org.amfoss.paneeer.editor.EditImageActivity;
import org.amfoss.paneeer.editor.task.StickerTask;
import org.amfoss.paneeer.editor.view.StickerItem;
import org.amfoss.paneeer.editor.view.StickerView;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static com.mikepenz.iconics.Iconics.TAG;

public class StickersFragment extends BaseEditFragment implements View.OnClickListener {

  RecyclerView recyclerView;
  View fragmentView;
  private StickerView mStickerView;
  private SaveStickersTask mSaveTask;
  List<String> pathList = new ArrayList<>();
  mRecyclerAdapter adapter;
  ImageButton cancel, apply;
  String mData;

  public StickersFragment() {}

  public static StickersFragment newInstance(ArrayList<String> list) {
    StickersFragment fragment = new StickersFragment();
    fragment.pathList = list;
    return fragment;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    RecyclerView.LayoutManager manager = null;

    int orientation = getActivity().getResources().getConfiguration().orientation;
    if (orientation == Configuration.ORIENTATION_PORTRAIT) {
      manager = new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);
    } else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
      manager = new LinearLayoutManager(getActivity());
    }
    recyclerView = fragmentView.findViewById(R.id.editor_recyclerview);
    recyclerView.setLayoutManager(manager);

    cancel = fragmentView.findViewById(R.id.sticker_cancel);
    apply = fragmentView.findViewById(R.id.sticker_apply);

    cancel.setImageResource(R.drawable.ic_close_black_24dp);
    apply.setImageResource(R.drawable.ic_done_black_24dp);

    cancel.setOnClickListener(this);
    apply.setOnClickListener(this);

    adapter = new mRecyclerAdapter();
    recyclerView.setAdapter(adapter);

    if (savedInstanceState != null) {
      mData = savedInstanceState.getString("Sticker position");
      if (mData != null) {
        selectedStickerItem(mData);
      }
    }

    this.mStickerView = activity.mStickerView;
    onShow();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    Paneeer.getRefWatcher(getActivity()).watch(this);
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    container.removeAllViews();
    if (fragmentView != null) {
      fragmentView.findViewById(R.id.sticker_cancel).setVisibility(View.GONE);
      fragmentView.findViewById(R.id.sticker_apply).setVisibility(View.GONE);
      fragmentView = null;
    }
    fragmentView = inflater.inflate(R.layout.fragment_editor_stickers, container, false);
    return fragmentView;
  }

  @Override
  public void onShow() {
    if (activity.mainBitmap == null) {
      getActivity()
          .getSupportFragmentManager()
          .beginTransaction()
          .remove(StickersFragment.this)
          .commit();
      return;
    }
    activity.changeMode(EditImageActivity.MODE_STICKERS);
    if (this.mStickerView == null) Log.d(TAG, "onShow:this.mstickerview is null ");

    getmStickerView().mainImage = activity.mainImage;
    getmStickerView().mainBitmap = activity.mainBitmap;
    getmStickerView().setVisibility(View.VISIBLE);
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    if (mData != null) {
      outState.putString("Sticker position", mData);
    }
  }

  private Bitmap getImageFromAssetsFile(String fileName) {
    Bitmap image = null;
    AssetManager am = getResources().getAssets();
    try {
      InputStream is = am.open(fileName);
      image = BitmapFactory.decodeStream(is);
      is.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return image;
  }

  public void selectedStickerItem(String path) {
    mStickerView.addBitImage(getImageFromAssetsFile(path));
  }

  public StickerView getmStickerView() {
    return mStickerView;
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.sticker_apply:
        if (!mStickerView.getBank().isEmpty()) {
          applyStickers();
        } else {
          new AlertDialog.Builder(getContext())
              .setTitle(R.string.no_stickers)
              .setMessage(R.string.exit_no_stickers)
              .setPositiveButton(
                  android.R.string.yes,
                  new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                      dialog.dismiss();
                      backToMain();
                    }
                  })
              .setNegativeButton(
                  android.R.string.cancel,
                  new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                      dialog.dismiss();
                    }
                  })
              .setIcon(R.drawable.ic_red_dialog_alert)
              .show();
        }
        break;
      case R.id.sticker_cancel:
        backToMain();
        break;
    }
  }

  public void backToMain() {
    activity.mainImage.setImageBitmap(activity.mainBitmap);
    EditImageActivity.mode = EditImageActivity.MODE_STICKER_TYPES;
    activity.stickerTypesFragment.clearCurrentSelection();
    activity.stickersFragment.getmStickerView().clear();
    activity.stickersFragment.getmStickerView().setVisibility(View.GONE);
    activity.changeBottomFragment(EditImageActivity.MODE_MAIN);
    activity.mainImage.setScaleEnabled(true);
  }

  private final class SaveStickersTask extends StickerTask {
    SaveStickersTask(EditImageActivity activity, Matrix imageViewMatrix) {
      super(activity, imageViewMatrix);
    }

    @Override
    public void handleImage(Canvas canvas, Matrix m) {
      LinkedHashMap<Integer, StickerItem> addItems = mStickerView.getBank();
      if (addItems.size() == 0) {
        this.cancel(true);
        return;
      }
      for (Integer id : addItems.keySet()) {
        if (id != 1) {
          StickerItem item = addItems.get(id);
          item.matrix.postConcat(m);
          canvas.drawBitmap(item.bitmap, item.matrix, null);
        }
      }
    }

    @Override
    public void onPostResult(Bitmap result) {
      mStickerView.clear();
      activity.changeMainBitmap(result);
      backToMain();
    }
  }

  public void applyStickers() {
    if (mStickerView.getBank().size() == 0) {
      backToMain();
    }
    if (mSaveTask != null) {
      mSaveTask.cancel(true);
    }
    mSaveTask = new SaveStickersTask(activity, activity.mainImage.getImageViewMatrix());
    mSaveTask.execute(activity.mainBitmap);
  }

  class mRecyclerAdapter extends RecyclerView.Adapter<mRecyclerAdapter.mViewHolder> {

    DisplayImageOptions imageOption =
        new DisplayImageOptions.Builder()
            .cacheInMemory(true)
            .showImageOnLoading(R.drawable.yd_image_tx)
            .build();

    class mViewHolder extends RecyclerView.ViewHolder {
      ImageView icon;
      TextView title;
      View view;

      mViewHolder(View itemView) {
        super(itemView);
        view = itemView;
        icon = itemView.findViewById(R.id.editor_item_image);
        title = itemView.findViewById(R.id.editor_item_title);
      }
    }

    mRecyclerAdapter() {}

    @Override
    public int getItemViewType(int position) {
      return 1;
    }

    @Override
    public mRecyclerAdapter.mViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      View view =
          LayoutInflater.from(parent.getContext()).inflate(R.layout.editor_iconitem, parent, false);
      return new mViewHolder(view);
    }

    @Override
    public void onBindViewHolder(mRecyclerAdapter.mViewHolder holder, final int position) {

      String path = pathList.get(position);
      ImageLoader.getInstance().displayImage("assets://" + path, holder.icon, imageOption);
      holder.itemView.setTag(path);
      holder.title.setText("");

      int size =
          (int) getActivity().getResources().getDimension(R.dimen.icon_item_image_size_sticker);
      LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(size, size);
      holder.icon.setLayoutParams(layoutParams);

      holder.itemView.setOnClickListener(
          new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              String data = (String) v.getTag();
              mData = data;
              selectedStickerItem(data);
            }
          });
    }

    @Override
    public int getItemCount() {
      return pathList.size();
    }
  }
}
