/*
 *  MQTT Client for Wemo D1 on Arduino
 *  UltraSonic HC-SR04 
 *  Built to work with any standard MQTT Broker
 */

#include <ESP8266WiFi.h>
#include <WiFiClient.h>
#include <PubSubClient.h>

//=====================Basic Setup ============================

#define wifi_ssid "xxxx"                                           // WiFi SSID
#define wifi_password "xxxxxxxxxx"                                 // WiFi Password
#define mqtt_clientname "xxxxxxxxx"                                // Arduino MQTT Client Name - example Salt_Tank
#define mqtt_server "xxxxxxxxxx"                                   // MQTT Broker Address
#define mqtt_user "xxxx"                                           // MQTT username if used
#define mqtt_password "xxxxxxxxxx"                                 // MQTT password if used 
#define distance_topic "topic_root/device/distance"                // distance topic value example: topic_root/device/distance
#define subscribe_topic "topic_root/device/delay"                  // delay in hours topic value example: topic_root/device/delay

//===============Do not edit below here!=======================
#define echoPin D7 // Echo Pin
#define trigPin D6 // Trigger Pin
unsigned long lastSend;
unsigned long lastConnected;
unsigned long lastLoop;
long duration, distance; // Duration used to calculate distance

int report=1;

WiFiClient espClient;
PubSubClient client(espClient);

void setup() {
  Serial.begin(115200);
  pinMode(trigPin, OUTPUT);
  pinMode(echoPin, INPUT);
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
      client.subscribe("wardhome/salt-tank/delay");
      Serial.println("connected");
      Serial.print("Subscribed to topic: ");
      Serial.println(subscribe_topic);
      digitalWrite(trigPin, LOW);
      delayMicroseconds(2);
      digitalWrite(trigPin, HIGH);
      delayMicroseconds(10);
      digitalWrite(trigPin, LOW);
      duration = pulseIn(echoPin, HIGH);
          //Calculate the distance (in cm) based on the speed of sound.
      distance = (duration/58.2) * 0.3937;
      client.publish(distance_topic, String(distance).c_str());              // Post Distance value to mqtt server
      Serial.print("Sonic reading is: ");
      Serial.print(distance); 
      Serial.println(" inches" );
      Serial.print("Delaying for ");
      Serial.print(report);
      Serial.println(" hours.");
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
  if(strcmp( topic, subscribe_topic ) == 0) {
  Serial.print("Message arrived in topic: ");
  Serial.println(topic);
  Serial.print("Message:");
  String temp;
  for(int i = 0; i < length; i++) {
    //Serial.print((char)payload[i]);
    temp = temp + ((char)payload[i]);
   }
  Serial.println(temp);
  if(temp.toInt() != 0) {
    report = temp.toInt();
    Serial.print("Setting hours to delay to: ");
    Serial.println(report);
  }
  }
}

void loop()
{
  
      int delaytime = report * (60000 * 60);
      if ( millis() - lastConnected > 2000 ) {
      if ( !client.connected() ) {
          reconnect();
       }
       lastConnected = millis();
       }
       if ( millis() - lastSend > delaytime ) { // Update and send only after 1 seconds
          digitalWrite(trigPin, LOW);
          delayMicroseconds(2);
          digitalWrite(trigPin, HIGH);
          delayMicroseconds(10);
          digitalWrite(trigPin, LOW);
          duration = pulseIn(echoPin, HIGH);
          //Calculate the distance (in cm) based on the speed of sound.
          distance = (duration/58.2) * 0.3937;
          client.publish(distance_topic, String(distance).c_str());              // Post Distance value to mqtt server
          Serial.print("Sonic reading is: ");
          Serial.print(distance); 
          Serial.println(" inches" );
          Serial.print("Delaying for ");
          Serial.print(report);
          Serial.println(" hours.");
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
