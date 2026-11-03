package org.lyi.cha;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.Locale;

/** 带进度条、已下载大小、百分比和实时速度的 cloudflared 下载对话框 */
public class DownloadDialog {

    public interface Callback {
        void onSuccess(String version);

        void onFailure(Exception e);

        /**
         * 下载成功后无论 Activity 是否还在（例如因深色模式切换被重建）都会在主线程调用，
         * 只应放与界面无关的收尾逻辑，如设置当前版本。
         */
        default void onCompleted(String version) {
        }
    }

    /**
     * @param version 指定版本 tag；为 null 时自动获取最新版本
     */
    public static void start(Activity act, CloudflaredManager mgr, @Nullable String version, Callback cb) {
        View v = LayoutInflater.from(act).inflate(R.layout.dialog_download, null);
        TextView tvStatus = v.findViewById(R.id.tvStatus);
        TextView tvProgress = v.findViewById(R.id.tvProgress);
        TextView tvSpeed = v.findViewById(R.id.tvSpeed);
        LinearProgressIndicator bar = v.findViewById(R.id.progress);
        bar.setMax(1000);
        bar.setProgress(0);

        tvStatus.setText(version == null ? "正在获取最新版本信息…" : "准备下载 cloudflared " + version + "…");
        tvProgress.setText("0 B");
        tvSpeed.setText("");

        AlertDialog dlg = new MaterialAlertDialogBuilder(act)
                .setTitle("下载 cloudflared")
                .setView(v)
                .setCancelable(false)
                .show();

        // Activity 被重建/销毁时同步关闭对话框，避免 WindowLeaked
        if (act instanceof LifecycleOwner) {
            ((LifecycleOwner) act).getLifecycle().addObserver((androidx.lifecycle.LifecycleEventObserver) (src, event) -> {
                if (event == Lifecycle.Event.ON_DESTROY && dlg.isShowing()) dlg.dismiss();
            });
        }

        Handler main = new Handler(Looper.getMainLooper());
        Thread th = new Thread(() -> {
            try {
                final String ver = version != null ? version : mgr.fetchLatestVersion();
                main.post(() -> tvStatus.setText("正在下载 cloudflared " + ver + " (" + CloudflaredManager.archName() + ")…"));
                final long[] lastUi = {0};
                mgr.download(ver, (done, total, speed) -> {
                    long now = System.currentTimeMillis();
                    if (now - lastUi[0] < 150 && (total <= 0 || done < total)) return;
                    lastUi[0] = now;
                    main.post(() -> {
                        if (total > 0) {
                            int pct = (int) (done * 100 / total);
                            bar.setProgressCompat((int) (done * 1000 / total), true);
                            tvProgress.setText(fmt(done) + " / " + fmt(total) + "  (" + pct + "%)");
                        } else {
                            tvProgress.setText(fmt(done));
                        }
                        tvSpeed.setText(fmt((long) speed) + "/s");
                    });
                });
                main.post(() -> {
                    cb.onCompleted(ver);
                    if (gone(act)) return;
                    dismissSafely(act, dlg);
                    cb.onSuccess(ver);
                });
            } catch (Exception e) {
                main.post(() -> {
                    if (gone(act)) return;
                    dismissSafely(act, dlg);
                    cb.onFailure(e);
                });
            }
        }, "cf-download");
        th.setDaemon(true);
        th.start();
    }

    /** Activity 已销毁（如旋转屏幕）时不能再弹对话框，否则会抛 BadTokenException */
    private static boolean gone(Activity act) {
        return act.isFinishing() || act.isDestroyed();
    }

    private static void dismissSafely(Activity act, AlertDialog dlg) {
        if (!gone(act) && dlg.isShowing()) dlg.dismiss();
    }

    public static String fmt(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double k = bytes / 1024.0;
        if (k < 1024) return String.format(Locale.US, "%.1f KB", k);
        double m = k / 1024.0;
        if (m < 1024) return String.format(Locale.US, "%.2f MB", m);
        return String.format(Locale.US, "%.2f GB", m / 1024.0);
    }
}
