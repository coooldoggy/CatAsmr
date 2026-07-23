# Firebase App Distribution 설정

## Android 설정

### 1. Firebase 서비스 계정 키 준비

1. [Firebase Console](https://console.firebase.google.com) 접속
2. 프로젝트 선택 → 프로젝트 설정 → 서비스 계정
3. "새로운 비공개 키 생성" 클릭
4. JSON 파일을 다운로드 후 `firebase-admin-key.json`으로 저장
5. 파일을 프로젝트 루트(`/app/build.gradle.kts` 옆)에 배치

### 2. Gradle 설정 (이미 완료)

- Firebase App Distribution 플러그인 추가
- `firebaseAppDistribution` 블록에서 테스터 이메일 설정

### 3. 빌드 및 배포

```bash
# 디버그 빌드 배포
./gradlew app:assembleDebug app:appDistributionUploadDebug

# 릴리스 빌드 배포
./gradlew app:assembleRelease app:appDistributionUploadRelease
```

## iOS 설정

### 1. Firebase 서비스 계정 키 준비

1. Android와 동일한 서비스 계정 키 사용 가능
2. 또는 새로운 비공개 키 생성

### 2. CocoaPods 설정 (이미 완료)

- Podfile에 `FirebaseAppDistribution` 추가됨

### 3. Pod 설치 및 빌드

```bash
cd PawWatch
pod install
open PawWatch.xcworkspace
```

### 4. fastlane으로 배포 (권장)

```bash
# fastlane 설치
sudo gem install fastlane -NV

# fastlane 초기화
cd ios
fastlane init
```

**Fastfile 예시:**
```ruby
default_platform(:ios)

platform :ios do
  desc "배포하기"
  lane :distribute do
    build_app(
      workspace: "PawWatch.xcworkspace",
      scheme: "PawWatch",
      configuration: "Debug",
      destination: "generic/platform=iOS",
      derivedDataPath: "build",
      archivePath: "build/PawWatch.xcarchive",
      export_options: { method: "development" }
    )

    firebase_app_distribution(
      app: "1:YOUR_PROJECT_ID:ios:YOUR_APP_ID",
      ipa_path: "build/PawWatch.ipa",
      firebase_cli_token: "YOUR_FIREBASE_TOKEN",
      testers: "coooldoggy@gmail.com",
      release_notes: "New release with gallery and KakaoTalk sharing features"
    )
  end
end
```

## 테스터 추가

1. Firebase Console → App Distribution → 테스터
2. 테스터 이메일 추가
3. 테스터는 이메일 초대를 받고 앱 다운로드 가능

## 주의사항

- `firebase-admin-key.json`은 .gitignore에 추가 (보안)
- 빌드 전에 서명 설정 확인 (iOS는 Signing & Capabilities 확인)
- 테스터는 정규 표현식 사용 가능 (예: `.*@example.com`)

## 문제 해결

- **Android 배포 실패**: 서비스 계정 키 경로 확인
- **iOS 배포 실패**: fastlane token 확인, Xcode 서명 설정 확인
- **테스터가 빌드를 받지 못함**: 테스터 이메일 정확성 확인, 초대 수락 확인
