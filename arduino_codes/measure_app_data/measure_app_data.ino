#include <SoftwareSerial.h>

// 핀 설정
const int buttonPin = 2;          // 버튼이 연결된 핀 2
const int tempSensorPin = A0;     // 온도 센서가 연결된 아날로그 핀 A0
const int heartSensorPin = A1;    // 심박수 센서가 연결된 아날로그 핀 A1

// 블루투스 설정
SoftwareSerial Bluetooth(10, 11); // HC-06 모듈의 RX, TX 핀

// 센서 데이터를 저장할 변수
float temperature = 0.0;
int heartRate = 0;

// 측정 시간 관리를 위한 변수
unsigned long measureStartTime = 0;
bool measuring = false;

void setup() {
  pinMode(buttonPin, INPUT_PULLUP);  // 버튼 핀을 풀업 입력으로 설정함
  Bluetooth.begin(9600);             // 블루투스 통신 시작 (9600 보드).. h6 다른설정은 건들지말것
  Serial.begin(9600);                // 디버깅을 위한 시리얼 통신 시작 9600 고정
}

void loop() {
  // 버튼이 눌렸는지 확인하는 코드
  if (digitalRead(buttonPin) == LOW) {
    if (!measuring) {
      measuring = true;
      measureStartTime = millis();
      Serial.println("측정 시작...");
    }
  }

  // 측정 중일 때 데이터 읽기, 시간은 5초임
  if (measuring) {
    if (millis() - measureStartTime < 5000) {
      // 온도 및 심박수 센서 값 읽기
      temperature = readTemperature();
      heartRate = readHeartRate();

      // 디버깅을 위한 시리얼 출력
      Serial.print("온도: ");
      Serial.print(temperature);
      Serial.print(" C, 심박수: ");
      Serial.print(heartRate);
      Serial.println(" BPM");
    } else {
      // 5000ms 후 측정 중지코드
      measuring = false;
      // 블루투스를 통해 데이터 전송
      sendBluetoothData(temperature, heartRate);
      Serial.println("측정 완료.");
    }
  }
}

// 온도 센서에서 값을 읽는 함수
float readTemperature() {
  int sensorValue = analogRead(tempSensorPin);
  float voltage = sensorValue * (5.0 / 1023.0);
  float tempC = (voltage - 0.5) * 100.0;  // 전압을 섭씨로 변환
  return tempC;
}

// 심박수 센서에서 값을 읽는 함수
int readHeartRate() {
  int sensorValue = analogRead(heartSensorPin);
  int heartRate = map(sensorValue, 0, 1023, 60, 100);  // 센서 값을 BPM으로 매핑 (예시코드라서 추후 변경 필요함)
  return heartRate;
}

// 블루투스를 통해 데이터를 전송하는 함수
void sendBluetoothData(float temp, int heartRate) {
  Bluetooth.print("온도: ");
  Bluetooth.print(temp);
  Bluetooth.print(" C, 심박수: ");
  Bluetooth.print(heartRate);
  Bluetooth.println(" BPM");
}
