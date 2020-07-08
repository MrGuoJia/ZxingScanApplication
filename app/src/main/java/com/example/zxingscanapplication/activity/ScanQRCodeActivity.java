package com.example.zxingscanapplication.activity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.example.zxingscanapplication.R;
import com.uuzuche.lib_zxing.activity.CaptureFragment;
import com.uuzuche.lib_zxing.activity.CodeUtils;

/**
 * 二维码扫描界面
 */
public class ScanQRCodeActivity extends AppCompatActivity {
    private FrameLayout fl_my_container;
    public static final int SCAN_SUCCESS=1111;//扫描成功
    public static final int SCAN_FAIL=1112;//扫描失败

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_q_r_code);
        fl_my_container=findViewById(R.id.fl_my_container);
        initScan();
    }

    private void initScan() {
        try {
            /**
             * 二维码解析回调函数
             */
            CodeUtils.AnalyzeCallback analyzeCallback = new CodeUtils.AnalyzeCallback() {
                @Override
                public void onAnalyzeSuccess(Bitmap mBitmap, String result) {
                    if (!TextUtils.isEmpty(result)) {
                        setResult(SCAN_SUCCESS,getIntent().putExtra("success_result",result));
                        finish();
                    }
                }

                @Override
                public void onAnalyzeFailed() {
                    setResult(SCAN_FAIL,getIntent().putExtra("fail_result","扫描失败"));
                    finish();
                }
            };

            /**
             * 执行扫面Fragment的初始化操作
             */
            CaptureFragment captureFragment = new CaptureFragment();
            // 为二维码扫描界面设置定制化界面
            CodeUtils.setFragmentArgs(captureFragment, R.layout.my_camera);
            captureFragment.setAnalyzeCallback(analyzeCallback);
            /**
             * 替换我们的扫描控件
             */
            getSupportFragmentManager().beginTransaction().replace(R.id.fl_my_container, captureFragment).commit();
        } catch (Exception e) {

        }
    }
}
