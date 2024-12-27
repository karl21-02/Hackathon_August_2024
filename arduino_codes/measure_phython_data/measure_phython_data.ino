//이건 유선통신을 통해 파이썬으로 보내는 코드임

const int buttonPin = 2;          // 버튼이 연결된 핀 2
const int tempSensorPin = A0;     // 온도 센서가 연결된 아날로그 핀 A0
const int heartSensorPin = A1;    // 심박수 센서가 연결된 아날로그 핀 A1

// 센서 데이터를 저장할 변수
float temperature = 0.0;
int heartRate = 0;

// 측정 시간 관리를 위한 변수
unsigned long measureStartTime = 0;
bool measuring = false;

void setup() {
  pinMode(buttonPin, INPUT_PULLUP);  // 버튼 핀을 풀업 입력으로 설정
  Serial.begin(9600);                // 시리얼 통신 시작 (9600 보드)
}

void loop() {
  // 버튼이 눌렸는지 확인
  if (digitalRead(buttonPin) == LOW) {
    if (!measuring) {
      measuring = true;
      measureStartTime = millis();
      Serial.println("측정 시작...");
    }
  }

  // 측정 중일 때 데이터 읽기
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
      // 5초 후 측정 중지
      measuring = false;
      // 시리얼을 통해 데이터 전송
      sendSerialData(temperature, heartRate);
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
  int heartRate = map(sensorValue, 0, 1023, 60, 100);  // 센서 값을 BPM으로 매핑 (예시)
  return heartRate;
}

// 시리얼을 통해 데이터를 전송하는 함수
void sendSerialData(float temp, int heartRate) {
  Serial.print("온도: ");
  Serial.print(temp);
  Serial.print(" C, 심박수: ");
  Serial.print(heartRate);
  Serial.println(" BPM");
}
