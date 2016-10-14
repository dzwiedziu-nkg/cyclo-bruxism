#include <CurieBLE.h>

const int ledPin = 13; // set ledPin to use on-board LED
BLEPeripheral blePeripheral; // create peripheral instance

BLEService ledService("181C"); // create service

// create switch characteristic and allow remote device to read and write
BLEUnsignedShortCharacteristic switchChar("2A19", BLERead | BLENotify);

int Trig = 3;  //pin 2 Arduino połączony z pinem Trigger czujnika
int Echo = 2;  //pin 3 Arduino połączony z pinem Echo czujnika
int CM = -1;        //odległość w cm
long CZAS;     //długość powrotnego impulsu w uS
int oldCM = -1;
int state = -1;
int oldState = -1;
int infinityCount = 0;
 
void setup() {
  Serial.begin(9600);                        //inicjalizaja monitora szeregowego
  pinMode(Trig, OUTPUT);                     //ustawienie pinu 2 w Arduino jako wyjście
  pinMode(Echo, INPUT);                      //ustawienie pinu 3 w Arduino jako wejście
  pinMode(13, OUTPUT);
   
  Serial.println("Test czujnika odleglosci");

  // set the local name peripheral advertises
  blePeripheral.setLocalName("CarDistance");
  // set the UUID for the service this peripheral advertises
  blePeripheral.setAdvertisedServiceUuid(ledService.uuid());

  // add service and characteristic
  blePeripheral.addAttribute(ledService);
  blePeripheral.addAttribute(switchChar);

  // assign event handlers for connected, disconnected to peripheral
  blePeripheral.setEventHandler(BLEConnected, blePeripheralConnectHandler);
  blePeripheral.setEventHandler(BLEDisconnected, blePeripheralDisconnectHandler);

  // set an initial value for the characteristic
  switchChar.setValue(0);

  // advertise the service
  blePeripheral.begin();
  Serial.println(("Bluetooth device active, waiting for connections..."));
}
  
void loop() {
  blePeripheral.poll();
  pomiar_odleglosci();              //pomiar odległości
  //Serial.println(int(sqrt(CM)));
  
  if (CM > 1000) {
    infinityCount++;
  } else {
    infinityCount = 0;
    if (oldCM > 1000) {
      oldCM = CM;
    }
  }

  oldState = state;
  if (infinityCount > 1) {
    state = 1;
  } else {
    state = 0;
  }
  
  if ((state != oldState) || (state == 0 && infinityCount == 0 && (int(sqrt(min(oldCM, 400))) != int(sqrt(min(CM, 400)))))) {
    Serial.print("Odleglosc: ");      //wyświetlanie wyników na ekranie w pętli co 200 ms
    Serial.print(CM);
    Serial.println(" cm");
    switchChar.setValue(CM);
  }
  digitalWrite(13, CM < 20 ? HIGH : LOW);
  //delay(200);
}
  
void pomiar_odleglosci () {
  digitalWrite(Trig, HIGH);       //ustawienie stanu wysokiego na 10 uS - impuls inicjalizujacy - patrz dokumentacja
  delayMicroseconds(10);
  digitalWrite(Trig, LOW);
  CZAS = pulseIn(Echo, HIGH);
  oldCM = CM;
  CM = CZAS / 58;                //szerokość odbitego impulsu w uS podzielone przez 58 to odleglosc w cm - patrz dokumentacja
}


void blePeripheralConnectHandler(BLECentral& central) {
  // central connected event handler
  Serial.print("Connected event, central: ");
  Serial.println(central.address());
}

void blePeripheralDisconnectHandler(BLECentral& central) {
  // central disconnected event handler
  Serial.print("Disconnected event, central: ");
  Serial.println(central.address());
}

