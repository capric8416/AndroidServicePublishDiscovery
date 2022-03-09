package com.upuphone.service.discovery.multicast;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

import com.upuphone.service.discovery.multicast.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'multicast' library on application startup.
    static {
        System.loadLibrary("multicast");
    }

    private ActivityMainBinding binding;

    private Discovery discovery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Example of a call to a native method
        TextView tv = binding.sampleText;
        tv.setText(stringFromJNI());

        discovery = new Discovery();
        discovery.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        discovery.stop();
    }

    /**
     * A native method that is implemented by the 'multicast' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}