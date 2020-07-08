package com.example.zxingscanapplication.activity;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.zxingscanapplication.R;
import com.example.zxingscanapplication.activity.utils.BitMapUtil;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.permissionx.guolindev.PermissionX;
import com.permissionx.guolindev.callback.ExplainReasonCallback;
import com.permissionx.guolindev.callback.ForwardToSettingsCallback;
import com.permissionx.guolindev.callback.RequestCallback;
import com.permissionx.guolindev.request.ExplainScope;
import com.permissionx.guolindev.request.ForwardScope;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button btn_scan, btn_album;
    private TextView tv_content;
    public static final String CAMERA = Manifest.permission.CAMERA;
    public static final String WRITE_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE;
    public static final int SCAN_RESULT = 1120;
    public final static int DEVICE_PHOTO_REQUEST = 1234;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        btn_scan = findViewById(R.id.btn_scan);
        btn_album = findViewById(R.id.btn_album);
        tv_content = findViewById(R.id.tv_content);
        btn_scan.setOnClickListener(this);
        btn_album.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_scan://二维码扫描
                checkCamera();
                break;

            case R.id.btn_album://打开相册
                checkStorage();
                break;

        }
    }

    private void checkStorage() {
        PermissionX.init(this).permissions(WRITE_STORAGE).onExplainRequestReason(new ExplainReasonCallback() {
            @Override
            public void onExplainReason(ExplainScope scope, List<String> deniedList) {
                scope.showRequestReasonDialog(deniedList, "读取相册需要该权限", "允许");
            }
        })
                .onForwardToSettings(new ForwardToSettingsCallback() {
                    @Override
                    public void onForwardToSettings(ForwardScope scope, List<String> deniedList) {
                        scope.showForwardToSettingsDialog(deniedList, "需要在应用程序设置中手动开启", "OK");
                    }
                })
                .request(new RequestCallback() {
                    @Override
                    public void onResult(boolean allGranted, List<String> grantedList, List<String> deniedList) {
                        if (allGranted) {
                            Intent intent = new Intent(Intent.ACTION_PICK,
                                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                            startActivityForResult(intent, DEVICE_PHOTO_REQUEST);
                        } else {
                            Toast.makeText(MainActivity.this, "权限已拒绝", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void checkCamera() {
        PermissionX.init(this).permissions(CAMERA).onExplainRequestReason(new ExplainReasonCallback() {
            @Override
            public void onExplainReason(ExplainScope scope, List<String> deniedList) {
                scope.showRequestReasonDialog(deniedList, "扫描二维码需要开启摄像头", "允许");
            }
        }).onForwardToSettings(new ForwardToSettingsCallback() {
            @Override
            public void onForwardToSettings(ForwardScope scope, List<String> deniedList) {
                scope.showForwardToSettingsDialog(deniedList, "需要在应用程序设置中手动开启", "OK");
            }
        }).request(new RequestCallback() {
            @Override
            public void onResult(boolean allGranted, List<String> grantedList, List<String> deniedList) {
                if (allGranted) {
                    startActivityForResult(
                            new Intent(MainActivity.this, ScanQRCodeActivity.class)
                            , SCAN_RESULT);
                } else {
                    Toast.makeText(MainActivity.this, "权限已拒绝", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //系统相册返回请求
        switch (requestCode) {
            case DEVICE_PHOTO_REQUEST:
                if (data != null) {
                    Uri uri = data.getData();
                    String imagePath = BitMapUtil.getPicturePathFromUri(MainActivity.this, uri);
                    //对获取到的二维码照片进行压缩
                    Bitmap bitmap = BitMapUtil.compressPicture(imagePath);
                    Result result = setZxingResult(bitmap);
                    if (result == null) {
                        tv_content.setText("识别失败，请试试其它二维码");
                    } else {
                        tv_content.setText(result.getText());
                    }
                }
                break;
        }


        //扫描二维码返回结果码
        switch (resultCode) {
            case ScanQRCodeActivity.SCAN_SUCCESS:
                tv_content.setText(data.getStringExtra("success_result"));
                break;

            case ScanQRCodeActivity.SCAN_FAIL:
                tv_content.setText(data.getStringExtra("fail_result"));
                break;
        }
    }


    private static Result setZxingResult(Bitmap bitmap) {
        if (bitmap == null) return null;
        int picWidth = bitmap.getWidth();
        int picHeight = bitmap.getHeight();
        int[] pix = new int[picWidth * picHeight];
        //Log.e(TAG, "decodeFromPicture:图片大小： " + bitmap.getByteCount() / 1024 / 1024 + "M");
        bitmap.getPixels(pix, 0, picWidth, 0, 0, picWidth, picHeight);
        //构造LuminanceSource对象
        RGBLuminanceSource rgbLuminanceSource = new RGBLuminanceSource(picWidth
                , picHeight, pix);
        BinaryBitmap bb = new BinaryBitmap(new HybridBinarizer(rgbLuminanceSource));
        //因为解析的条码类型是二维码，所以这边用QRCodeReader最合适。
        QRCodeReader qrCodeReader = new QRCodeReader();
        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.CHARACTER_SET, "utf-8");
        hints.put(DecodeHintType.TRY_HARDER, true);
        Result result;
        try {
            result = qrCodeReader.decode(bb, hints);
            return result;
        } catch (NotFoundException | ChecksumException | FormatException e) {
            e.printStackTrace();
            return null;
        }
    }

}
