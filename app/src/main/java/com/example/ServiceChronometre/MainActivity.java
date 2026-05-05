package com.example.ServiceChronometre;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private TextView tvTemps;
    private Button btnStart, btnStop;
    private ChronometreService chronometreService;
    private boolean isBound = false;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ChronometreService.LocalBinder binder = (ChronometreService.LocalBinder) service;
            chronometreService = binder.getService();
            isBound = true;
            lancerMiseAJourUI(); // On commence à rafraîchir l'écran
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            chronometreService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvTemps = findViewById(R.id.tvTemps);
        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);

        btnStart.setOnClickListener(v -> startServiceAction());
        btnStop.setOnClickListener(v -> stopServiceAction());
    }

    private void startServiceAction() {
        Intent intent = new Intent(this, ChronometreService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    private void stopServiceAction() {
        uiHandler.removeCallbacksAndMessages(null); // Arrêter les updates UI

        Intent intent = new Intent(this, ChronometreService.class);
        intent.setAction("STOP");
        startService(intent); // Envoie l'action STOP au service

        if (isBound) {
            unbindService(connection);
            isBound = false;
        }
        tvTemps.setText("00:00");
    }

    private void lancerMiseAJourUI() {
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                if (isBound && chronometreService != null) {
                    tvTemps.setText(chronometreService.getTempsFormate());
                    uiHandler.postDelayed(this, 1000); // Se relance dans 1 sec
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(connection);
            isBound = false;
        }
        uiHandler.removeCallbacksAndMessages(null);
    }
}