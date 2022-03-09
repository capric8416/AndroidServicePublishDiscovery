package com.upuphone.service.publish.ble;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.upuphone.service.publish.ble.databinding.ActivityMainBinding;

import java.nio.charset.Charset;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'ble' library on application startup.
    static {
        System.loadLibrary("ble");
    }

    BluetoothLeAdvertiser mAdvertiser;
    BluetoothGattService mGattService;
    BluetoothGattCharacteristic mGattCharacteristic;
    BluetoothGattDescriptor mGattDescriptor;
    BluetoothGattServer mBluetoothGattServer;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Example of a call to a native method
        TextView tv = binding.sampleText;
        tv.setText(stringFromJNI());

        startAdvertise();

        startGattService();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        stopAdvertise();
    }

    /**
     * A native method that is implemented by the 'ble' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();


    private void startAdvertise() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this
                    , new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                    }
                    , 2);
        }

        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        mAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setTimeout(0)
                .setConnectable(true)
                .build();

        ParcelUuid uuid1 = new ParcelUuid(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
        ParcelUuid uuid2 = new ParcelUuid(UUID.fromString("0000950D-0000-1000-8000-00805F9B34FB"));

        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(true)
                .addServiceUuid(uuid1)
                .addServiceData(uuid2, "Data".getBytes(Charset.forName("UTF-8")))
                .build();

        mAdvertiser.startAdvertising(settings, data, new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                super.onStartSuccess(settingsInEffect);

                Log.e("BLE Publish", "onStartSuccess");
            }

            @Override
            public void onStartFailure(int errorCode) {
                super.onStartFailure(errorCode);

                Log.e("BLE Publish", "onStartFailure: " + errorCode);
            }
        });
    }

    private void stopAdvertise() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //  ActivityCompat#requestPermissions
        }
        mAdvertiser.stopAdvertising(new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                super.onStartSuccess(settingsInEffect);
            }

            @Override
            public void onStartFailure(int errorCode) {
                super.onStartFailure(errorCode);
            }
        });
    }

    private void startGattService() {
        mGattService = new BluetoothGattService(UUID.fromString("2D8D66E1-1F8E-4AB2-81AA-C446E3D6674F")
                , BluetoothGattService.SERVICE_TYPE_PRIMARY);

        mGattCharacteristic = new BluetoothGattCharacteristic(UUID.fromString("316C2BE6-D8C2-4209-9AC7-1F53E47385C4")
                , BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_NOTIFY | BluetoothGattCharacteristic.PROPERTY_READ
                , BluetoothGattCharacteristic.PERMISSION_WRITE | BluetoothGattCharacteristic.PERMISSION_READ);

        mGattDescriptor = new BluetoothGattDescriptor(UUID.fromString("AB94A07E-1648-41F7-A775-D00B3BBCEE45")
                , BluetoothGattDescriptor.PERMISSION_WRITE);

        mGattService.addCharacteristic(mGattCharacteristic);
        mGattCharacteristic.addDescriptor(mGattDescriptor);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //  ActivityCompat#requestPermissions
        }
        mBluetoothGattServer = mBluetoothManager.openGattServer(this, new BluetoothGattServerCallback() {
            //设备连接/断开连接回调
            @Override
            public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
                super.onConnectionStateChange(device, status, newState);
            }

            //添加本地服务回调
            @Override
            public void onServiceAdded(int status, BluetoothGattService service) {
                super.onServiceAdded(status, service);
            }

            //特征值读取回调
            @Override
            public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            }

            //特征值写入回调
            @Override
            public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
                super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            }

            //描述读取回调
            @Override
            public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
                super.onDescriptorReadRequest(device, requestId, offset, descriptor);
            }

            //描述写入回调
            @Override
            public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
                super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
            }
        });

        boolean result = mBluetoothGattServer.addService(mGattService);

        Log.e("BLE Publish", "startGattService: " + result);
    }

}