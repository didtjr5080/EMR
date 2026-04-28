# TreatmentTaskSystem

## Python 서버 설치 방법

1. Python 3.9+ 설치
2. 가상환경 생성 및 진입
   ```bash
   python -m venv venv
   source venv/bin/activate  # Windows: venv\Scripts\activate
   ```
3. 패키지 설치
   ```bash
   pip install -r requirements.txt
   ```

## 서버 실행 방법

```bash
python run.py
```

## 웹 관리자 접속 주소

- http://localhost:8000

## Android BASE_URL 설정 방법

- Android 코드의 BASE_URL을 PC의 IP:8000으로 설정
- 예: `http://192.168.0.10:8000/`

## 같은 Wi-Fi에서 Android 실기기 테스트

1. PC와 Android 기기가 같은 Wi-Fi에 연결되어 있어야 함
2. PC의 IP 확인 (Windows: `ipconfig`)
3. Android BASE_URL을 PC IP로 설정

## Cloudflare Tunnel로 외부 접속

1. [Cloudflare Tunnel](https://developers.cloudflare.com/cloudflare-one/connections/connect-apps/install-and-setup/tunnel-guide/) 설치
2. 터널 실행 예시:
   ```bash
   cloudflared tunnel --url http://localhost:8000
   ```
3. 발급된 URL을 Android BASE_URL로 사용
