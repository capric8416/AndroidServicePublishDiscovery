package com.upuphone.service.publish.multicast;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

import com.upuphone.service.publish.multicast.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'multicast' library on application startup.
    static {
        System.loadLibrary("multicast");
    }

    private ActivityMainBinding binding;

    private Publish publish;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Example of a call to a native method
        TextView tv = binding.sampleText;
        tv.setText(stringFromJNI());

        publish = new Publish();
        publish.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        publish.stop();
    }

    /**
     * A native method that is implemented by the 'multicast' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}