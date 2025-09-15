package com.example.mqttvoiceapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private EditText etBrokerIp, etBrokerPort, etProtocol, etTopic, etClientId, etUsername, etPassword;
    private Button btnStart, btnStop, btnTestConnection;
    private TextView tvStatus, tvMessageLog;
    private ScrollView svLogContainer;
    private SharedPreferences sharedPreferences;
    private MessageReceiver messageReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        loadSavedConfig();
        setClickListeners();
        setupReceiver();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messageReceiver != null) {
            unregisterReceiver(messageReceiver);
        }
    }

    private void initViews() {
        etBrokerIp = findViewById(R.id.et_broker_ip);
        etBrokerPort = findViewById(R.id.et_broker_port);
        etProtocol = findViewById(R.id.et_protocol);
        etTopic = findViewById(R.id.et_topic);
        etClientId = findViewById(R.id.et_client_id);
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnStart = findViewById(R.id.btn_start);
        btnStop = findViewById(R.id.btn_stop);
        btnTestConnection = findViewById(R.id.btn_test_connection);
        tvStatus = findViewById(R.id.tv_status);
        tvMessageLog = findViewById(R.id.tv_message_log);
        svLogContainer = findViewById(R.id.sv_log_container);

        tvMessageLog.setMovementMethod(new ScrollingMovementMethod());

        sharedPreferences = getSharedPreferences("mqtt_config", MODE_PRIVATE);
    }

    private void loadSavedConfig() {
        etBrokerIp.setText(sharedPreferences.getString("broker_ip", "broker.emqx.io"));
        etBrokerPort.setText(sharedPreferences.getString("broker_port", "1883"));
        etProtocol.setText(sharedPreferences.getString("protocol", "tcp://"));
        etTopic.setText(sharedPreferences.getString("topic", "test/voice"));
        etClientId.setText(sharedPreferences.getString("client_id", "android_client_" + System.currentTimeMillis()));
        etUsername.setText(sharedPreferences.getString("username", ""));
        etPassword.setText(sharedPreferences.getString("password", ""));
    }

    private void saveConfig() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("broker_ip", etBrokerIp.getText().toString().trim());
        editor.putString("broker_port", etBrokerPort.getText().toString().trim());
        editor.putString("protocol", etProtocol.getText().toString().trim());
        editor.putString("topic", etTopic.getText().toString().trim());
        editor.putString("client_id", etClientId.getText().toString().trim());
        editor.putString("username", etUsername.getText().toString().trim());
        editor.putString("password", etPassword.getText().toString().trim());
        editor.apply();
    }

    private void setClickListeners() {
        btnStart.setOnClickListener(v -> startMQTTService());
        btnStop.setOnClickListener(v -> stopMQTTService());
        btnTestConnection.setOnClickListener(v -> testConnection());
    }

    private void setupReceiver() {
        messageReceiver = new MessageReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MQTTService.ACTION_MQTT_MESSAGE_RECEIVED);
        intentFilter.addAction(MQTTService.ACTION_MQTT_STATUS_UPDATE);
        registerReceiver(messageReceiver, intentFilter, RECEIVER_NOT_EXPORTED);
    }

    private void startMQTTService() {
        if (!validateInput()) {
            return;
        }
        // Clear log on new start
        tvMessageLog.setText("等待消息...");
        saveConfig();

        Intent serviceIntent = new Intent(this, MQTTService.class);
        serviceIntent.putExtra("BROKER_IP", etBrokerIp.getText().toString().trim());
        serviceIntent.putExtra("BROKER_PORT", etBrokerPort.getText().toString().trim());
        serviceIntent.putExtra("PROTOCOL", etProtocol.getText().toString().trim());
        serviceIntent.putExtra("TOPIC", etTopic.getText().toString().trim());
        serviceIntent.putExtra("CLIENT_ID", etClientId.getText().toString().trim());
        serviceIntent.putExtra("USERNAME", etUsername.getText().toString().trim());
        serviceIntent.putExtra("PASSWORD", etPassword.getText().toString().trim());

        startForegroundService(serviceIntent);
        updateStatus("服务已启动");
        Toast.makeText(this, "MQTT服务已启动", Toast.LENGTH_SHORT).show();
    }

    private void stopMQTTService() {
        Intent serviceIntent = new Intent(this, MQTTService.class);
        stopService(serviceIntent);
        updateStatus("服务已停止");
        Toast.makeText(this, "MQTT服务已停止", Toast.LENGTH_SHORT).show();
    }

    private void testConnection() {
        if (!validateInput()) {
            return;
        }

        Toast.makeText(this, "连接测试功能将在服务中实现", Toast.LENGTH_SHORT).show();
    }

    private boolean validateInput() {
        String brokerIp = etBrokerIp.getText().toString().trim();
        String brokerPort = etBrokerPort.getText().toString().trim();
        String topic = etTopic.getText().toString().trim();
        String clientId = etClientId.getText().toString().trim();

        if (brokerIp.isEmpty()) {
            Toast.makeText(this, "请输入服务器IP地址", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (brokerPort.isEmpty()) {
            Toast.makeText(this, "请输入端口号", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (topic.isEmpty()) {
            Toast.makeText(this, "请输入订阅主题", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (clientId.isEmpty()) {
            Toast.makeText(this, "请输入客户端ID", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void updateStatus(String status) {
        tvStatus.setText("状态: " + status);
    }

    private void appendToLog(String text, boolean isStatus) {
        String timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        String currentLog = tvMessageLog.getText().toString();
        String newLog;
        
        String formattedText = isStatus ? "[状态] " + text : text;

        if (currentLog.equals("等待消息...")) {
            newLog = timestamp + ": " + formattedText;
        } else {
            newLog = currentLog + "\n" + timestamp + ": " + formattedText;
        }
        tvMessageLog.setText(newLog);

        // Auto-scroll to the bottom
        svLogContainer.post(() -> svLogContainer.fullScroll(View.FOCUS_DOWN));
    }

    // Inner class for receiving messages from MQTTService
    private class MessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) {
                return;
            }
            
            String action = intent.getAction();
            if (MQTTService.ACTION_MQTT_MESSAGE_RECEIVED.equals(action)) {
                String message = intent.getStringExtra(MQTTService.EXTRA_MQTT_MESSAGE);
                if (message != null) {
                    appendToLog(message, false);
                }
            } else if (MQTTService.ACTION_MQTT_STATUS_UPDATE.equals(action)) {
                String status = intent.getStringExtra(MQTTService.EXTRA_MQTT_STATUS);
                if (status != null) {
                    appendToLog(status, true);
                }
            }
        }
    }
}
