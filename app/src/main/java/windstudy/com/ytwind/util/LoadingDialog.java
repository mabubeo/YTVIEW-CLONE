package windstudy.com.ytwind.util;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;

import com.github.ybq.android.spinkit.style.CubeGrid;

import windstudy.com.ytwind.R;

public class LoadingDialog {
    private static LoadingDialog loadingDialog = null;
    private static Dialog dialog;
    Context context;

    public static Dialog createDialog(Context context) {
        Dialog dialog = new Dialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_loading, null);
        dialog.setContentView(view);

        ProgressBar progressBar = view.findViewById(R.id.progress_bar);
        CubeGrid cubeGrid = new CubeGrid();
        progressBar.setIndeterminateDrawable(cubeGrid);

        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        return dialog;
    }

    public static LoadingDialog getInstance() {
        if (loadingDialog == null) {
            loadingDialog = new LoadingDialog();
        }
        return loadingDialog;
    }

    public void showLoading(Context context) {
        if (context != this.context || dialog == null) {
            dialog = createDialog(context);
        }
        this.context = context;
        dialog.show();

    }

    public void hideLoading() {
        if (dialog == null) return;
        if (dialog.isShowing()) dialog.hide();
    }
}
