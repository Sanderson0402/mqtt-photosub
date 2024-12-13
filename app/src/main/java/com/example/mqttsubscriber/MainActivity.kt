package com.example.mqttsubscriber

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private lateinit var imageView: ImageView
    private lateinit var subscribeButton: Button
    private lateinit var mqttClient: MqttClient

    private val topics = listOf("cachorro", "boi", "cavalo", "capivara")
    private var selectedTopic: String = "cachorro"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageView = findViewById(R.id.imageView)
        subscribeButton = findViewById(R.id.subscribeButton)
        val topicSpinner: Spinner = findViewById(R.id.topicSpinner)

        // Inicializa o cliente MQTT
        mqttClient = MqttClient("tcp://broker.hivemq.com:1883", MqttClient.generateClientId(), null)
        mqttClient.connect()

        // Configurar Spinner de tópicos
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, topics)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        topicSpinner.adapter = adapter

        topicSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                selectedTopic = topics[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedTopic = "cachorro"
            }
        }

        subscribeButton.setOnClickListener {
            val fullTopic = "animal/photos/$selectedTopic" // Prefixo fixo + tópico selecionado
            subscribeToTopic(fullTopic)
        }
    }

    // Inscrição no tópico
    private fun subscribeToTopic(topic: String) {
        try {
            mqttClient.setCallback(object : org.eclipse.paho.client.mqttv3.MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    Toast.makeText(this@MainActivity, "Connection lost", Toast.LENGTH_SHORT).show()
                    clearImageView() // Limpa a ImageView se a conexão for perdida
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    message?.payload?.let { payload ->
                        val messageContent = String(payload)
                        try {
                            val jsonObject = JSONObject(messageContent)
                            // Verifica se a chave "photo" existe no JSON
                            if (jsonObject.has("photo")) {
                                val base64Image = jsonObject.getString("photo")
                                // Decodifica o base64 para Bitmap
                                val decodedBytes = Base64.decode(base64Image, Base64.DEFAULT)
                                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                                // Atualiza a UI na thread principal
                                runOnUiThread {
                                    imageView.setImageBitmap(bitmap)
                                }
                            } else {
                                // Se não houver a chave "photo", chama clearImageView
                                clearImageView()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this@MainActivity, "Error processing message: ${e.message}", Toast.LENGTH_SHORT).show()
                            clearImageView() // Chama clearImageView em caso de erro
                        }
                    } ?: run {
                        // Se a mensagem for nula, chama clearImageView
                        clearImageView()
                    }
                }

                override fun deliveryComplete(token: org.eclipse.paho.client.mqttv3.IMqttDeliveryToken?) {}
            })

            mqttClient.subscribe(topic) // Inscreve no tópico
            Toast.makeText(this, "Subscribed to topic: $topic", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Toast.makeText(this, "Error subscribing to topic", Toast.LENGTH_SHORT).show()
            clearImageView() // Limpa a ImageView se ocorrer erro na inscrição
        }
    }

    // Limpa a ImageView caso não haja mensagem ou chave "photo"
    private fun clearImageView() {
        runOnUiThread {
            imageView.setImageResource(0) // Remove a imagem da ImageView
            Toast.makeText(this, "No message found for topic", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        mqttClient.disconnect()
        super.onDestroy()
    }
}
