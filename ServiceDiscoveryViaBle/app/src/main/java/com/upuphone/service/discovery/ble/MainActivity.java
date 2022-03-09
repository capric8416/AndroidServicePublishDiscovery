package com.upuphone.service.discovery.ble;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.upuphone.service.discovery.ble.databinding.ActivityMainBinding;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'ble' library on application startup.
    static {
        System.loadLibrary("ble");
    }

    BluetoothManager mBluetoothManager;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothDevice mBluetoothDevice;
    private ActivityMainBinding binding;
    private Handler mHandler;
    private HandlerThread mHandlerThread;

    private BluetoothGatt mBluetoothGatt;

    private BluetoothGattCallback mGattCallback;

    private boolean discovered = false;



    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Example of a call to a native method
        TextView tv = binding.sampleText;
        tv.setText(stringFromJNI());

        mHandlerThread = new HandlerThread("BleDiscovery");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());

        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            ActivityCompat.requestPermissions(this
                    , new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                    }
                    , 2);
        }

        discover();
    }

    /**
     * A native method that is implemented by the 'ble' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    private void discover() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this
                    , new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                    }
                    , 2);
        }

        List<ScanFilter> filters = new ArrayList<ScanFilter>();

        ScanFilter filter = new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")))
//                .setServiceSolicitationUuid(new ParcelUuid(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")), new ParcelUuid(UUID.fromString("00000000-0000-0000-0000-111111111111")))
                .build();
        filters.add(filter);

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner().startScan(filters, settings, new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);

                if (result == null) {
                    return;
                }

                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                    // TODO
                }

                StringBuilder builder = new StringBuilder();

                String name = result.getDevice().getName();
                if (name != null) {
                    builder.append("name: " + name + "; ");
                }

                String address = result.getDevice().getAddress();
                if (address != null) {
                    builder.append("address: " + address + "; ");
                }

                List<ParcelUuid> uuids = result.getScanRecord().getServiceUuids();
                if (uuids != null && !uuids.isEmpty()) {
                    builder.append("uuid: " + uuids.get(0));
                }

                Log.e("BLE Discovery", "onScanResult: " + builder);

                synchronized (this) {
                    if (!discovered) {
                        discovered = true;
                        transfer(result.getDevice());
                    }
                }
            }


            private void transfer(BluetoothDevice device) {
                mGattCallback = new MyBluetoothGattCallback();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                    }
                    mBluetoothGatt = device.connectGatt(getApplicationContext()
                            , false
                            , mGattCallback
                            , BluetoothDevice.TRANSPORT_LE);
                }


            }

            @Override
            public void onScanFailed(int errorCode) {
                Log.e("BLE Discovery", "onScanFailed: " + errorCode);
                super.onScanFailed(errorCode);
            }
        });
    }


    private class MyBluetoothGattCallback extends BluetoothGattCallback {
        //定义重连次数
        private int reConnectionNum = 0;
        //最多重连次数
        private int maxConnectionNum = 3;

        //连接状态回调
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            // status 用于返回操作是否成功,会返回异常码。
            //操作成功的情况下
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //判断是否连接码
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    //可延迟发现服务，也可不延迟
                    mHandler.post(() -> {
                                //发现服务
                                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                                    // TODO: Consider calling
                                    //    ActivityCompat#requestPermissions
                                }
                                mBluetoothGatt.discoverServices();
                            }
                    );
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    //判断是否断开连接码

                }
            } else {
                //重连次数不大于最大重连次数
                if (reConnectionNum < maxConnectionNum) {
                    //重连次数自增
                    reConnectionNum++;
                    //连接设备
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        mBluetoothGatt = mBluetoothDevice.connectGatt(getApplicationContext(),
                                false, mGattCallback, BluetoothDevice.TRANSPORT_LE);
                    }
                } else {
                    //断开连接，返回连接失败回调

                }
            }

        }

        //服务发现回调
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mHandler.post(() -> {
                            //获取指定uuid的service
                            BluetoothGattService gattService = mBluetoothGatt.getService(UUID.fromString("2D8D66E1-1F8E-4AB2-81AA-C446E3D6674F"));
                            //获取到特定的服务不为空
                            if (gattService != null) {
                                //获取指定uuid的Characteristic
                                BluetoothGattCharacteristic gattCharacteristic = gattService.getCharacteristic(UUID.fromString("316C2BE6-D8C2-4209-9AC7-1F53E47385C4"));
                                //获取特定特征成功
                                if (gattCharacteristic != null) {

                                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                                        // TODO: Consider calling
                                        //    ActivityCompat#requestPermissions
                                    }
                                    mBluetoothGatt.setCharacteristicNotification(gattCharacteristic, true);
                                    BluetoothGattDescriptor descriptor = gattCharacteristic.getDescriptor(UUID.fromString("AB94A07E-1648-41F7-A775-D00B3BBCEE45"));
                                    if (descriptor != null) {
                                        //设置通知值
                                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                                        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                                            // TODO: Consider calling
                                            //    ActivityCompat#requestPermissions
                                        }
                                        boolean descriptorResult = mBluetoothGatt.writeDescriptor(descriptor);
                                    }

                                    //写入你需要传递给外设的特征值（即传递给外设的信息）
                                    gattCharacteristic.setValue("hello".getBytes(StandardCharsets.UTF_8));
                                    //通过GATt实体类将，特征值写入到外设中。
                                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                                        // TODO: Consider calling
                                        //    ActivityCompat#requestPermissions
                                    }
                                    mBluetoothGatt.writeCharacteristic(gattCharacteristic);

                                    //如果只是需要读取外设的特征值：
                                    //通过Gatt对象读取特定特征（Characteristic）的特征值
                                    mBluetoothGatt.readCharacteristic(gattCharacteristic);
                                }
                            } else {
                                //获取特定服务失败

                            }
                        }
                );


            }

        }

        //特征写入回调
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //获取写入到外设的特征值
                characteristic.getValue();
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //获取读取到的特征值
                characteristic.getValue();
            }
        }

        //外设特征值改变回调
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);

            //获取外设修改的特征值
            String value = String.valueOf(characteristic.getValue());
            //对特征值进行解析

        }

        //描述写入回调
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
        }

    }

}


