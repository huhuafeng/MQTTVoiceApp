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
import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MQTTService extends Service {
    // Actions for broadcasts
    public static final String ACTION_MQTT_MESSAGE_RECEIVED = "com.example.mqttvoiceapp.MQTT_MESSAGE_RECEIVED";
    public static final String ACTION_MQTT_STATUS_UPDATE = "com.example.mqttvoiceapp.MQTT_STATUS_UPDATE";

    // Keys for extra data in intents
    public static final String EXTRA_MQTT_MESSAGE = "com.example.mqttvoiceapp.MQTT_MESSAGE";
    public static final String EXTRA_MQTT_STATUS = "com.example.mqttvoiceapp.MQTT_STATUS";

    private static final String TAG = "MQTTService";
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "MQTT_CHANNEL";

    private MqttAndroidClient mqttClient;
    private TextToSpeech textToSpeech;
    private ScheduledExecutorService scheduler;
    private String brokerIp, brokerPort, protocol, topic, clientId, username, password;
    private boolean isTtsInitialized = false;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        initTextToSpeech();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getExtras() != null) {
            brokerIp = intent.getStringExtra("BROKER_IP");
            brokerPort = intent.getStringExtra("BROKER_PORT");
            protocol = intent.getStringExtra("PROTOCOL");
            topic = intent.getStringExtra("TOPIC");
            clientId = intent.getStringExtra("CLIENT_ID");
            username = intent.getStringExtra("USERNAME");
            password = intent.getStringExtra("PASSWORD");

            // Always try to start the connection when the service is started with details
            startMQTTConnection();
        }

        startForeground(NOTIFICATION_ID, createNotification("服务已启动"));
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
            scheduler.shutdownNow();
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
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private void initTextToSpeech() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                broadcastStatus("TTS引擎初始化成功");
                int result = textToSpeech.setLanguage(Locale.CHINESE);
                if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "中文语音不支持");
                    broadcastStatus("TTS中文语言不支持或数据丢失");
                } else {
                    Log.d(TAG, "TTS引擎初始化成功，语言已设置为中文");
                    broadcastStatus("TTS中文语言设置成功");
                    isTtsInitialized = true;
                }
            } else {
                Log.e(TAG, "TTS初始化失败");
                broadcastStatus("TTS引擎初始化失败，状态码: " + status);
            }
        });
    }

    private void startMQTTConnection() {
        // Disconnect any existing client before creating a new one
        disconnectMQTT();

        String brokerUrl = protocol + brokerIp + ":" + brokerPort;
        Log.d(TAG, "连接MQTT服务器: " + brokerUrl);
        broadcastStatus("正在连接: " + brokerUrl);
        updateNotification("正在连接: " + brokerUrl);

        try {
            mqttClient = new MqttAndroidClient(this.getApplicationContext(), brokerUrl, clientId, new MemoryPersistence());

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setKeepAliveInterval(60);
            options.setConnectionTimeout(30);
            options.setAutomaticReconnect(true); // Paho a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a-library provides automatic reconnect options, which is great.

            if (username != null && !username.isEmpty()) {
                options.setUserName(username);
            }
            if (password != null && !password.isEmpty()) {
                options.setPassword(password.toCharArray());
            }

            mqttClient.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    String status = (reconnect ? "重连" : "连接") + "成功";
                    Log.d(TAG, status + "，订阅主题: " + topic);
                    broadcastStatus(status);
                    updateNotification("已连接，监听主题: " + topic);
                    try {
                        String[] topics = topic.split(",");
                        int[] qos = new int[topics.length];
                        for (int i = 0; i < topics.length; i++) {
                            qos[i] = 1;
                            topics[i] = topics[i].trim();
                        }
                        mqttClient.subscribe(topics, qos);
                    } catch (MqttException e) {
                        Log.e(TAG, "订阅主题失败", e);
                        broadcastStatus("订阅主题失败");
                    }
                }

                @Override
                public void connectionLost(Throwable cause) {
                    String causeMessage = (cause != null) ? cause.getMessage() : "未知原因";
                    Log.w(TAG, "连接断开: " + causeMessage, cause);
                    broadcastStatus("连接已断开");
                    updateNotification("连接已断开");
                    // Automatic reconnect is handled by the Paho library options
                }

                @Override
                public void messageArrived(String receivedTopic, MqttMessage message) {
                    // Step 1: Decode payload using UTF-8 to prevent garbled text
                    String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
                    Log.d(TAG, "收到消息 [" + receivedTopic + "]: " + payload);

                    // Step 2: Try to parse as JSON
                    try {
                        JSONObject json = new JSONObject(payload);
                        // Check if it's the specific dynamic TTS format and has a "txt" field
                        if (json.has("type") && "tts_dynamic".equals(json.getString("type")) && json.has("txt")) {
                            String textToSpeak = json.getString("txt");
                            // Only speak and log if the text is not empty
                            if (textToSpeak != null && !textToSpeak.trim().isEmpty()) {
                                Log.d(TAG, "解析到JSON指令，播报内容: " + textToSpeak);
                                speakMessage(textToSpeak);
                                broadcastMessage("JSON指令: " + textToSpeak);
                            } else {
                                Log.d(TAG, "收到空的JSON指令，已忽略");
                                broadcastStatus("收到空的JSON指令，已忽略");
                            }
                        } else {
                            // It's a valid JSON object, but not the format we are looking for. Treat as plain text.
                            speakMessage(payload);
                            broadcastMessage(payload);
                        }
                    } catch (JSONException e) {
                        // It's not a valid JSON string, treat as plain text.
                        Log.d(TAG, "消息不是JSON格式，按纯文本处理");
                        speakMessage(payload);
                        broadcastMessage(payload);
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // Not used
                }
            });

            mqttClient.connect(options, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "MQTT连接请求成功，等待 connectComplete 回调");
                    // The actual success logic (like subscribing) is in connectComplete
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG, "MQTT连接失败", exception);
                    broadcastStatus("MQTT连接失败: " + exception.getMessage());
                    // Since automatic reconnect is enabled, Paho will handle retries.
                    // We can also use our own scheduler as a fallback.
                    scheduleReconnect();
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "MQTT连接时发生初始异常", e);
            broadcastStatus("MQTT连接异常: " + e.getMessage());
            scheduleReconnect();
        }
    }

    private void scheduleReconnect() {
        if (scheduler == null || scheduler.isShutdown()) {
            scheduler = Executors.newSingleThreadScheduledExecutor();
        }
        scheduler.schedule(this::startMQTTConnection, 10, TimeUnit.SECONDS);
    }

    private void speakMessage(String message) {
        if (isTtsInitialized && textToSpeech != null) {
            textToSpeech.speak(message, TextToSpeech.QUEUE_ADD, null, "mqtt_message");
        }
    }

    private void broadcastMessage(String message) {
        Intent intent = new Intent(ACTION_MQTT_MESSAGE_RECEIVED);
        intent.putExtra(EXTRA_MQTT_MESSAGE, message);
        intent.setPackage(getPackageName()); // Make broadcast explicit
        sendBroadcast(intent);
    }

    private void broadcastStatus(String status) {
        Intent intent = new Intent(ACTION_MQTT_STATUS_UPDATE);
        intent.putExtra(EXTRA_MQTT_STATUS, status);
        intent.setPackage(getPackageName()); // Make broadcast explicit
        sendBroadcast(intent);
    }

    private void disconnectMQTT() {
        try {
            if (mqttClient != null) {
                if (mqttClient.isConnected()) {
                    mqttClient.disconnect();
                }
                mqttClient.close();
                mqttClient = null;
            }
        } catch (MqttException e) {
            Log.e(TAG, "断开MQTT连接失败", e);
        }
    }
    
    private void updateNotification(String contentText) {
        Notification notification = createNotification(contentText);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, notification);
    }

    private Notification createNotification(String contentText) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MQTT语音播报服务")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build();
    }
}