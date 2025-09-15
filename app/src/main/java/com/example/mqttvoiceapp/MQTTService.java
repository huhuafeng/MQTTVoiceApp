package com.example.mqttvoiceapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MQTTService extends Service {
    public static final String ACTION_MQTT_MESSAGE_RECEIVED = "com.example.mqttvoiceapp.MQTT_MESSAGE_RECEIVED";
    public static final String EXTRA_MQTT_MESSAGE = "com.example.mqttvoiceapp.MQTT_MESSAGE";

    private static final String TAG = "MQTTService";
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "MQTT_CHANNEL";

    private MqttClient mqttClient;
    private TextToSpeech textToSpeech;
    private ScheduledExecutorService scheduler;
    private String brokerIp, brokerPort, protocol, topic, clientId, username, password;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        initTextToSpeech();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            brokerIp = intent.getStringExtra("BROKER_IP");
            brokerPort = intent.getStringExtra("BROKER_PORT");
            protocol = intent.getStringExtra("PROTOCOL");
            topic = intent.getStringExtra("TOPIC");
            clientId = intent.getStringExtra("CLIENT_ID");
            username = intent.getStringExtra("USERNAME");
            password = intent.getStringExtra("PASSWORD");
            
            startMQTTConnection();
        }

        startForeground(NOTIFICATION_ID, createNotification());
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disconnectMQTT();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "MQTT服务",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("MQTT消息接收服务");
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MQTT语音播报服务")
            .setContentText("正在监听MQTT消息...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build();
    }

    private void initTextToSpeech() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.CHINESE);
                if (result == TextToSpeech.LANG_MISSING_DATA || 
                    result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "中文语音不支持");
                } else {
                    textToSpeech.setSpeechRate(1.0f);
                }
            } else {
                Log.e(TAG, "TTS初始化失败");
            }
        });
    }

    private void startMQTTConnection() {
        try {
            String brokerUrl = protocol + brokerIp + ":" + brokerPort;
            Log.d(TAG, "连接MQTT服务器: " + brokerUrl);

            mqttClient = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
            
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setKeepAliveInterval(60);
            options.setConnectionTimeout(30);
            options.setAutomaticReconnect(true);

            // 设置认证信息（如果提供了）
            if (username != null && !username.isEmpty()) {
                options.setUserName(username);
            }
            if (password != null && !password.isEmpty()) {
                options.setPassword(password.toCharArray());
            }

            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    Log.w(TAG, "连接断开", cause);
                    scheduleReconnect();
                }

                @Override
                public void messageArrived(String receivedTopic, MqttMessage message) {
                    String payload = new String(message.getPayload());
                    Log.d(TAG, "收到消息 [" + receivedTopic + "]: " + payload);
                    speakMessage(payload);
                    broadcastMessage(payload);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // 消息发送完成
                }
            });

            mqttClient.connect(options);
            
            // 订阅主题（支持多个主题，用逗号分隔）
            String[] topics = topic.split(",");
            int[] qos = new int[topics.length];
            for (int i = 0; i < topics.length; i++) {
                qos[i] = 1;
                topics[i] = topics[i].trim();
            }
            mqttClient.subscribe(topics, qos);
            
            Log.d(TAG, "MQTT连接成功，订阅主题: " + topic);

        } catch (Exception e) {
            Log.e(TAG, "MQTT连接失败", e);
            scheduleReconnect();
        }
    }

    private void scheduleReconnect() {
        if (scheduler == null) {
            scheduler = Executors.newScheduledThreadPool(1);
        }
        scheduler.schedule(this::startMQTTConnection, 10, TimeUnit.SECONDS);
    }

    private void speakMessage(String message) {
        if (textToSpeech != null && !textToSpeech.isSpeaking()) {
            textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, "mqtt_message");
        }
    }

    private void broadcastMessage(String message) {
        Intent intent = new Intent(ACTION_MQTT_MESSAGE_RECEIVED);
        intent.putExtra(EXTRA_MQTT_MESSAGE, message);
        sendBroadcast(intent);
    }

    private void disconnectMQTT() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
                mqttClient.close();
            }
        } catch (MqttException e) {
            Log.e(TAG, "断开MQTT连接失败", e);
        }
    }
}
