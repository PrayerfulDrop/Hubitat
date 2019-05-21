/*
 *  MQTT Client for NodeMCU ESP-12E on Arduino
 *  UltraSonic HC-SR04 
 *  Built to work with any standard MQTT Broker
 */

#include <ESP8266WiFi.h>
#include <WiFiClient.h>
#include <PubSubClient.h>
#include "Ultrasonic.h"                                            // Include library file for ultrasonic (HC-SR04)

//=====================Basic Setup ============================

#define wifi_ssid "xxxx"                                           // WiFi SSID
#define wifi_password "xxxxxxxxxx"                                 // WiFi Password
#define mqtt_clientname "xxxxxxxxx"                                // Arduino MQTT Client Name - example Salt_Tank
#define mqtt_server "xxxxxxxxxx"                                   // MQTT Broker Address
#define mqtt_user "xxxx"                                           // MQTT username if used
#define mqtt_password "xxxxxxxxxx"                                 // MQTT password if used 
#define distance_topic "topic_root/device/distance"                // distance topic value example: topic_root/device/distance
#define subscribe_topic "topic_root/device/delay"                  // delay in minutes topic value example: topic_root/device/delay
Ultrasonic ultrasonic(14,12);                                      // Assign Trig PIN 14(D5),Assign Echo PIN 12(D6)

//===============Do not edit below here!=======================
int GIu_Ultrasonic_Dist_CM=0;
unsigned long lastSend;
unsigned long lastConnected;
unsigned long lastLoop;

int report=1;

WiFiClient espClient;
PubSubClient client(espClient);

void setup() {
  Serial.begin(115200);
  setup_wifi();
  client.setServer(mqtt_server, 1883);
  client.setCallback(callback);
  lastSend = 0;
  lastConnected = 0;
  lastLoop = 0;
}

void setup_wifi() {
  delay(10);
  // We start by connecting to a WiFi network
  Serial.println();
  Serial.print("Connecting to ");
  Serial.println(wifi_ssid);

  WiFi.begin(wifi_ssid, wifi_password);

  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }

  Serial.println("");
  Serial.println("WiFi connected");
  Serial.println("IP address: ");
  Serial.println(WiFi.localIP());
}

void reconnect() {
  // Loop until we're reconnected
  while (!client.connected()) {
    Serial.print("Attempting MQTT connection...");
    // Attempt to connect
    // If you do not want to use a username and password, change next line to
    // if (client.connect("ESP8266Client")) {
    if (client.connect(mqtt_clientname, mqtt_user, mqtt_password)) {
      client.subscribe(subscribe_topic);
      Serial.println("connected");
      Serial.print("Subscribed to topic: ");
      Serial.println(subscribe_topic);
      GIu_Ultrasonic_Dist_CM=ultrasonic.Ranging(INC);   // Read ultrasonic distance value in CM or INCH
      client.publish(distance_topic, String(GIu_Ultrasonic_Dist_CM).c_str());              // Post Distance value in cayenne mqtt server
      Serial.print("Sonic reading is: ");
      Serial.print(GIu_Ultrasonic_Dist_CM); 
      Serial.println(" inches" );
      Serial.print("Delaying for ");
      Serial.print(report);
      Serial.println(" minutes.");
    } else {
      Serial.print("failed, rc=");
      Serial.print(client.state());
      Serial.println(" try again in 5 seconds");
      // Wait 5 seconds before retrying
      delay(5000);
    }
  }
}


void callback(char* topic, byte* payload, unsigned int length)
{
  Serial.print("Message arrived in topic: ");
  Serial.println(topic);
  Serial.print("Message:");
  String temp;
  for(int i = 0; i < length; i++) {
    //Serial.print((char)payload[i]);
    temp = temp + ((char)payload[i]);
   }
  Serial.println(temp);
  report = temp.toInt();
}

void loop()
{
  
      int delaytime = report * 60000;
      if ( millis() - lastConnected > 2000 ) {
      if ( !client.connected() ) {
          reconnect();
       }
       lastConnected = millis();
       }
       if ( millis() - lastSend > delaytime ) { // Update and send only after 1 seconds
              GIu_Ultrasonic_Dist_CM=ultrasonic.Ranging(INC);   // Read ultrasonic distance value in CM or INCH
              client.publish(distance_topic, String(GIu_Ultrasonic_Dist_CM).c_str());              // Post Distance value in cayenne mqtt server
              Serial.print("Sonic reading is: ");
              Serial.print(GIu_Ultrasonic_Dist_CM); 
              Serial.println(" inches" );
              Serial.print("Delaying for ");
              Serial.print(report);
              Serial.println(" minutes.");
              lastSend = millis();
      }
      if ( millis() - lastLoop > 2000 ) {
         client.loop();
         lastLoop = millis();
     }
      //client.loop(); 
      delay(10);
     
}

//==================================================================
