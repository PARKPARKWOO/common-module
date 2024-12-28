# Common Module
`common` 모듈은 공통 로직과 유틸리티를 제공하여 여러 모듈 또는 프로젝트에서 재사용성을 높이고, 중복 코드를 줄이기 위해 설계된 모듈입니다. 이 모듈은 프로젝트 전반에서 자주 사용되는 기능과 구현체를 포함하고 있으며, Spring Boot와 Kotlin 환경에 최적화되어 있습니다.

---

## 배포방법
git branch 의 prefix 기준으로 해당 모듈을 배포합니다.
1. branch checkout
```shell
git checkout -b modulename/feature
```
2. git push


## 주요 기능
1. apm (application 모니터링 관련 모듈)
   1. log
      - webflux와 mvc 환경 모두 지원
      - 사용자 정의 로깅 필터 제공
        - MDC(Mapped Diagnostic Context) 초기화 로직 포함
   2. pyroscope

2. Mapper (mapper 모듈)
- 객체 간 매핑을 간편하게 처리할 수 있는 유틸리티 제공

3. HTTP (http 모듈)
- 공통 HTTP 클라이언트 설정 및 유틸리티 제공

4. Domain (domain-* 모듈)
- 공통 엔티티 및 데이터 모델 정의
  - 예: domain-auth, domain-user 등

5. grpc
- application 통신을 위한 proto 파일 관리


## 설치 및 의존성 추가

### Gradle
`common` 모듈의 각 하위 모듈을 필요에 따라 추가하세요.

```kotlin
repositories {
    mavenCentral()
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/PARKPARKWOO/common-module")
        credentials {
            username = project.findProperty("gpr.user") ?: System.getenv("GITHUB_USERNAME")
            password = project.findProperty("gpr.key") ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    // 필요한 모듈만 선택적으로 추가
    implementation("org.woo:apm:+")          // Logging 기능
    implementation("org.woo:mapper:+")       // Object Mapping 기능
    implementation("org.woo:http:+")         // HTTP Client 유틸리티
    implementation("org.woo:domain-auth:+")  // 인증 관련 Domain
    implementation("org.woo:grpc:+")
}

```