# GitHub Actions Firebase Distribution 설정

## 개요
이 가이드는 GitHub Actions를 사용해 자동으로 Firebase App Distribution에 앱을 배포하는 방법을 설명합니다.

## 필수 준비사항

### 1. Firebase 서비스 계정 키 생성

1. [Firebase Console](https://console.firebase.google.com) 접속
2. 프로젝트 선택 → ⚙️ 프로젝트 설정
3. **서비스 계정** 탭 선택
4. **새로운 비공개 키 생성** 클릭
5. JSON 파일 다운로드

### 2. GitHub Repository Secrets 설정

#### Android 설정

1. GitHub Repository 접속 → **Settings** → **Secrets and variables** → **Actions**
2. **New repository secret** 클릭
3. 다음 secrets 추가:

| Name | 값 |
|------|-----|
| `FIREBASE_ADMIN_KEY` | 다운로드한 JSON 파일의 전체 내용 |

#### iOS 설정 (fastlane 사용)

1. Firebase CLI 토큰 생성:
```bash
firebase login:ci
```

2. GitHub Secrets에 추가:

| Name | 값 |
|------|-----|
| `FIREBASE_ADMIN_KEY` | Firebase 서비스 계정 키 JSON |
| `FIREBASE_CLI_TOKEN` | firebase login:ci로 생성한 토큰 |
| `GOOGLE_SERVICE_INFO_PLIST` | GoogleService-Info.plist를 Base64로 인코딩한 값 |

**GoogleService-Info.plist Base64 인코딩:**
```bash
base64 -i PawWatch/GoogleService-Info.plist | pbcopy
```

### 3. fastlane 설정 (iOS만 필요)

`ios/fastlane/Fastfile` 생성:

```ruby
default_platform(:ios)

platform :ios do
  desc "Firebase App Distribution 배포"
  lane :distribute do
    setup_ci if is_ci
    
    match(
      type: "development",
      readonly: is_ci
    ) if is_ci

    build_app(
      workspace: "PawWatch.xcworkspace",
      scheme: "PawWatch",
      configuration: "Debug",
      destination: "generic/platform=iOS",
      derivedDataPath: "build",
      archivePath: "build/PawWatch.xcarchive",
      export_options: {
        method: "development",
        signingStyle: "automatic"
      }
    )

    firebase_app_distribution(
      app: "1:YOUR_PROJECT_ID:ios:YOUR_APP_ID",
      ipa_path: "build/PawWatch.ipa",
      firebase_cli_token: ENV["FIREBASE_CLI_TOKEN"],
      testers: "coooldoggy@gmail.com",
      release_notes: "Automated release via GitHub Actions"
    )
  end
end
```

**YOUR_PROJECT_ID와 YOUR_APP_ID 찾기:**
```bash
firebase projects:list
firebase apps:list --project=YOUR_PROJECT_ID
```

## 워크플로우 실행

### GitHub UI에서 수동 실행

1. Repository → **Actions** 탭
2. **Firebase App Distribution - [Android/iOS]** 선택
3. **Run workflow** 클릭
4. Build type 선택 (debug/release)
5. **Run workflow** 클릭

### 자동 실행 (선택사항)

Workflow를 푸시/릴리스에 자동으로 실행하도록 수정:

**Android:**
```yaml
on:
  push:
    branches: [main]
    paths:
      - 'app/**'
      - 'build.gradle.kts'
```

**iOS:**
```yaml
on:
  push:
    branches: [main]
    paths:
      - 'PawWatch/**'
      - 'Podfile'
```

## 트러블슈팅

| 문제 | 해결방법 |
|------|--------|
| `FIREBASE_ADMIN_KEY not found` | GitHub Secrets에서 FIREBASE_ADMIN_KEY 확인 |
| `Firebase CLI token not found` (iOS) | FIREBASE_CLI_TOKEN이 정확한지 확인, `firebase login:ci` 다시 실행 |
| Build 실패 | 로컬에서 먼저 빌드 성공 확인 후 workflow 실행 |
| 테스터가 빌드를 못 받음 | Firebase Console에서 테스터 이메일 확인 |
| iOS 서명 에러 | fastlane에서 `match` 설정 확인, Apple Developer 인증서 확인 |

## 주의사항

- ⚠️ **firebase-admin-key.json과 FIREBASE_CLI_TOKEN은 절대 git에 커밋하지 마세요**
- Secrets는 로그에 마스킹됩니다
- 각 workflow 실행마다 새로운 버전 번호를 자동으로 증가시키려면 추가 설정 필요

## 다음 단계

1. Secrets 설정 완료 후 workflow 테스트 실행
2. 각 플랫폼별로 첫 배포 확인
3. 테스터가 제대로 빌드를 받는지 확인
