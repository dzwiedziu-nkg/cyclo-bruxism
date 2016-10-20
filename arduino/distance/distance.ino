//#define BLE_101
//#define DEBUG
#define SERIAL


#ifdef BLE_101
#include <CurieBLE.h>
#endif


#define SCAN_PAUSE 0
#define MAX_INFINITY_COUNT 1
const int ledPin = 13; // set ledPin to use on-board LED
const int Trig = 2;  //pin 2 Arduino połączony z pinem Trigger czujnika
const int Echo = 3;  //pin 3 Arduino połączony z pinem Echo czujnika



#define STATE_UNINITIALIZED -1
#define STATE_NEAR 0
#define STATE_INFINITY 1

int CM = -1;        //odległość w cm
int sqrtCM = -1;
long CZAS;     //długość powrotnego impulsu w uS
int oldCM = -1;
int oldSqrtCM = -1;
int state = STATE_UNINITIALIZED;
int oldState = STATE_UNINITIALIZED;
int infinityCount = 0;



#ifdef BLE_101
BLEPeripheral blePeripheral; // create peripheral instance
BLEService ledService("181C"); // create service

// create switch characteristic and allow remote device to read and write
BLEUnsignedShortCharacteristic switchChar("2A19", BLERead | BLENotify);
#endif


void setup() {
#if defined(SERIAL) || defined(DEBUG)
  Serial.begin(9600);                        //inicjalizaja monitora szeregowego
#endif
  pinMode(Trig, OUTPUT);                     //ustawienie pinu 2 w Arduino jako wyjście
  pinMode(Echo, INPUT);                      //ustawienie pinu 3 w Arduino jako wejście
  pinMode(13, OUTPUT);

#ifdef DEBUG
  Serial.println("Test czujnika odleglosci");
#endif

#ifdef BLE_101
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
#ifdef DEBUG
  Serial.println(("Bluetooth device active, waiting for connections..."));
#endif
#endif

}

void loop() {
#ifdef BLE_101
  blePeripheral.poll();
#endif

  pomiar_odleglosci();              //pomiar odległości
  sqrtCM = sqrt(min(CM, 400));

#ifdef DEBUG
  Serial.print("Debug: CM=");
  Serial.print(CM);
  Serial.print(", sqrtCM=");
  Serial.println(sqrtCM);
#endif

  if (CM > 1000) {
    infinityCount++;
  } else {
    infinityCount = 0;
    if (oldCM > 1000) {
      oldCM = CM;
      oldSqrtCM = sqrtCM;
    }
  }

  oldState = state;
  if (infinityCount > MAX_INFINITY_COUNT) {
    state = STATE_INFINITY;
  } else {
    state = STATE_NEAR;
  }

#ifdef DEBUG
  Serial.print("Debug: oldState=");
  Serial.print(oldState);
  Serial.print(", state=");
  Serial.println(state);
#endif

  if ((state != oldState) || (state == 0 && infinityCount == 0 && (int(sqrt(min(oldCM, 400))) != int(sqrt(min(CM, 400)))))) {
#ifdef SERIAL
    Serial.println(CM);
#endif

#ifdef BLE_101
    switchChar.setValue(CM);
#endif
  }
  digitalWrite(13, CM < 20 ? HIGH : LOW);
  delay(SCAN_PAUSE);
}

void pomiar_odleglosci () {
  digitalWrite(Trig, HIGH);       //ustawienie stanu wysokiego na 10 uS - impuls inicjalizujacy - patrz dokumentacja
  delayMicroseconds(10);
  digitalWrite(Trig, LOW);
  CZAS = pulseIn(Echo, HIGH);
  oldCM = CM;
  CM = CZAS / 58;                //szerokość odbitego impulsu w uS podzielone przez 58 to odleglosc w cm - patrz dokumentacja
}

#ifdef BLE_101
void blePeripheralConnectHandler(BLECentral& central) {
  // central connected event handler
#ifdef DEBUG
  Serial.print("Connected event, central: ");
  Serial.println(central.address());
#endif
}

void blePeripheralDisconnectHandler(BLECentral& central) {
  // central disconnected event handler
#ifdef DEBUG
  Serial.print("Disconnected event, central: ");
  Serial.println(central.address());
#endif
}
#endif
