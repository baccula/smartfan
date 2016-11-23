/*
 * Version 0.2 20161118
 */

#include <ESP8266WiFi.h>
#include <WiFiClient.h>
#include <ESP8266WebServer.h>
#include <ESP8266mDNS.h>
#include <ESP8266HTTPUpdateServer.h>

//Define pins used to activate relays
#define relayPin1 5
#define relayPin2 4
#define relayPin3 14
#define relayPin4 12
#define led_pin LED_BUILTIN

//WiFi Params
const char* host = "fanremote";
const char* ssid = "YOUR_SSID";
const char* password = "YOUR_PW";

MDNSResponder mdns;

ESP8266WebServer server(80);
ESP8266HTTPUpdateServer httpUpdater;

void handleRoot() {
 server.send(200, "text/html", "<html><head> <title>ESP8266 RF Remote</title></head><body><h1>ESP8266 RF Remote</h1><p><a href=\"ir?fan=lighttoggle\">LIGHT</a></p><p><a href=\"ir?fan=fanoff\">FAN OFF</a></p><p><a href=\"ir?fan=speedlo\">LO SPEED</a></p><p><a href=\"ir?fan=speedhi\">HI SPEED</a></p></body></html>");
}

void handleIr(){
  for (uint8_t i=0; i<server.args(); i++){
    if(server.argName(i) == "fan")
    {
      if (server.arg(i) == "fanoff") { // fan off
        //Fan Off Relay Active
        Serial.println("Off Toggled");
        digitalWrite(relayPin1, LOW);
        digitalWrite(led_pin, LOW);
        delay(500);
        digitalWrite(relayPin1, HIGH);
        digitalWrite(led_pin, HIGH);
      }
      if (server.arg(i) == "lighttoggle") { // light toggle
        //Light Toggle Relay Active
        Serial.println("Light Switch Toggled");
        digitalWrite(relayPin2, LOW);
        digitalWrite(led_pin, LOW);
        delay(500);
        digitalWrite(relayPin2, HIGH);
        digitalWrite(led_pin, HIGH);
      }
      if (server.arg(i) == "speedlo") { // fan speed lo
        //Speed Lo Relay Active
        Serial.println("Fan speed low");
        digitalWrite(relayPin3, LOW);
        digitalWrite(led_pin, LOW);
        delay(500);
        digitalWrite(relayPin3, HIGH);
        digitalWrite(led_pin, HIGH);
      }
      if (server.arg(i) == "speedhi") { // fan speed hi
        //Speed Hi Relay Active
        Serial.println("Fan speed high");
        digitalWrite(relayPin4, LOW);
        digitalWrite(led_pin, LOW);
        delay(500);
        digitalWrite(relayPin4, HIGH);
        digitalWrite(led_pin, HIGH);
      }
    }
  }
  handleRoot();
}

void handleNotFound(){
  String message = "File Not Found\n\n";
  message += "URI: ";
  message += server.uri();
  message += "\nMethod: ";
  message += (server.method() == HTTP_GET)?"GET":"POST";
  message += "\nArguments: ";
  message += server.args();
  message += "\n";
  for (uint8_t i=0; i<server.args(); i++){
    message += " " + server.argName(i) + ": " + server.arg(i) + "\n";
  }
  server.send(404, "text/plain", message);
}

void setup(void){

  Serial.begin(9600);
  WiFi.begin(ssid, password);
  Serial.println("");

  // Wait for connection
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("");
  Serial.print("Connected to ");
  Serial.println(ssid);
  Serial.print("IP address: ");
  Serial.println(WiFi.localIP());

  if (mdns.begin(host, WiFi.localIP())) {
    mdns.addService("http", "tcp", 80);
    Serial.println("MDNS responder started");
  }

  //Initialize pins for output, set state high
  pinMode(relayPin1, OUTPUT);
  pinMode(relayPin2, OUTPUT);
  pinMode(relayPin3, OUTPUT);
  pinMode(relayPin4, OUTPUT);
  pinMode(led_pin, OUTPUT);
  digitalWrite(led_pin, HIGH);
  digitalWrite(relayPin1, HIGH);
  digitalWrite(relayPin2, HIGH);
  digitalWrite(relayPin3, HIGH);
  digitalWrite(relayPin4, HIGH);

  server.on("/", handleRoot);
  server.on("/ir", handleIr);

  server.on("/inline", [](){
    server.send(200, "text/plain", "this works as well");
  });

  server.onNotFound(handleNotFound);

  httpUpdater.setup(&server);
  server.begin();
  Serial.println("HTTP server started");
  Serial.printf("And we're back! Open http://%s.local/update in your browser\n", host);
}

void loop(void){
  server.handleClient();
}