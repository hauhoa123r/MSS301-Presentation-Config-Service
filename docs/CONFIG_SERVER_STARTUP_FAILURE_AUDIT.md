# Config Server Startup Failure Audit

Ngày audit: 2026-10-14 (Asia/Saigon). Phạm vi: khảo sát source, output build hiện có, Git và metadata IntelliJ. Chỉ tạo báo cáo này; không sửa source/POM/config, đổi dependency, xóa file hay tạo repository.

## 1. Symptom

Theo log người dùng cung cấp, Config Server thất bại với `Invalid config server configuration` và yêu cầu Git URI. `.idea/workspace.xml` cũng lưu trạng thái `ConfigServerApplication = FAILED`.

Nguyên nhân trực tiếp được chứng minh bằng cấu trúc source và output hiện có: cấu hình nằm tại `src/main/resource/application.yml`, thiếu chữ **s** trong `resources`; Maven không có khai báo resource tùy chỉnh. Vì vậy file chứa URI không được đưa vào classpath mặc định. `target/classes/application.yml` thực tế không tồn tại.

Không chạy lại server hoặc build trong audit này. Kết luận runtime dựa trên output hiện có và run configuration lưu trên đĩa; không giả định có thể quan sát môi trường của phiên IntelliJ đã thất bại.

## 2. Config Server Module Structure

Đường dẫn tính từ Git root:

```text
ats_parrent/infras/config-server/
├── pom.xml
├── src/main/java/fu/ats/ConfigServerApplication.java
├── src/main/resource/application.yml       # thực tế, sai tên thư mục
└── target/classes/fu/ats/ConfigServerApplication.class
```

`ats_parrent/infras/config-server/src/main/resources/application.yml`: không tồn tại. Tên file thực tế là `application.yml`, đúng extension; lỗi nằm ở thư mục cha.

`ConfigServerApplication.java` có đúng package `fu.ats`, `@SpringBootApplication`, `@EnableConfigServer` và `SpringApplication.run(ConfigServerApplication.class, args)`; không thêm profile hoặc location bằng code.

## 3. Maven Configuration

| Kiểm tra | Bằng chứng / kết quả |
| --- | --- |
| Parent reactor | `ats_parrent/pom.xml` có `<module>infras/config-server</module>` |
| Parent coordinates | Module kế thừa `fu.ats:ats_parrent:1.0-SNAPSHOT` |
| relativePath | `../../pom.xml`, resolve đúng về `ats_parrent/pom.xml` |
| Config Server dependency | Có `org.springframework.cloud:spring-cloud-config-server` |
| Dependency khác | Chỉ thêm `spring-boot-starter-actuator` trong module |
| Resource override | Parent và module không có `<build><resources>` tùy chỉnh |
| Versions hiện có | Boot `4.1.1`, Cloud `2025.1.2`, compiler source/target `21`; giữ nguyên, không kết luận compatibility là nguyên nhân của lỗi thiếu URI |
| IntelliJ import | `.idea/misc.xml` đăng ký `ats_parrent/pom.xml`; `.idea/compiler.xml` liệt kê module `config-server`; run configuration chọn module này |
| Output build | Có compiled main class, không có YAML/properties trong `target/classes` của module |

Theo [Maven Standard Directory Layout](https://maven.apache.org/guides/introduction/introduction-to-the-standard-directory-layout), resource mặc định nằm trong `src/main/resources` và được copy vào classpath. `src/main/resource` không đáp ứng quy ước này. Metadata IDE cho thấy module đã được nhận diện; không có bằng chứng module bị thiếu import. Không thực hiện Maven build/effective-POM trong audit này.

## 4. Runtime Configuration File

Nội dung source thực tế tại `ats_parrent/infras/config-server/src/main/resource/application.yml`:

```yaml
server:
  port: 8888

spring:
  application:
    name: config-server

  cloud:
    config:
      server:
        git:
          uri: file:///D:/Ky8/MSS301/FA26_MSS301_SE1946-JV
          default-label: Hòa-Đẹp-Trai-Vũ-Trụ
          search-paths: ats-config-repo
```

Kiểm tra bytes: đầu file `73-65-72-76-65-72-3A-0D` (`server:`), không có UTF-8/UTF-16 BOM ở đầu. Không có tab. Indentation đúng. Parse bằng SnakeYAML `2.6` có sẵn trong Maven cache qua JShell thành công, trả về mapping `spring → cloud → config → server → git → uri`; kiểm tra Java URI cũng thành công. Đây là kiểm tra parser riêng, không khẳng định phiên runtime dùng đúng version parser này. Không cài dependency mới.

Không tìm thấy `application.properties`, `application.yaml`, `application-*.yml` hay `bootstrap*` khác trong module; output module cũng không có config thay thế. Các `application.yml` của service khác và `ats-config-repo/application.yml` không tự động là cấu hình khởi động của module Config Server.

Không thấy `spring.config.location`, `spring.config.name`, `spring.config.import`, active profile hoặc `spring.cloud.config.server.bootstrap=true` trong source của module hoặc run configuration tương ứng. Một active profile bình thường không tự làm Boot bỏ qua base `application.yml`.

**Kết luận:** với classpath output và run configuration đang lưu, Config Server không nhìn thấy URI từ file này. Property đã viết đúng nhưng file chứa nó chưa vào vị trí được load. Không có bằng chứng URI đang bị một file khác override.

## 5. Git Repository Topology

Đã chạy các lệnh ở cả root và `ats-config-repo`:

```powershell
git rev-parse --show-toplevel
git branch --show-current
git status
git -C ats-config-repo rev-parse --show-toplevel
git -C ats-config-repo branch --show-current
git -C ats-config-repo status --short
```

Cả hai trả cùng Git root `D:/Ky8/MSS301/FA26_MSS301_SE1946-JV` và branch `Hòa-Đẹp-Trai-Vũ-Trụ`. Trước khi tạo báo cáo, working tree clean. `Test-Path ats-config-repo/.git` trả `False`.

`ats-config-repo` là **subfolder của repository ATS**, không phải repository độc lập. `git ls-tree -r --name-only HEAD ats-config-repo` xác nhận cả `application.yml` và `job-service-dev.yml` đã có trong commit hiện tại.

## 6. Branch / Label Analysis

`git show-ref --verify 'refs/heads/Hòa-Đẹp-Trai-Vũ-Trụ'` thành công:

```text
46bc9bcf4a19c461c0217f6a8fdbfe27bfced12c refs/heads/Hòa-Đẹp-Trai-Vũ-Trụ
```

Branch này thuộc repository ATS ở root nêu trên. Vì Git URI thực tế cũng trỏ root này, `default-label: Hòa-Đẹp-Trai-Vũ-Trụ` hợp lệ. Không có config repository độc lập để suy ra một branch khác. Unicode trong tên branch không phải bằng chứng lỗi; khi đưa label vào URL nên percent-encode.

## 7. URI Analysis

Thư mục cấu hình thực tế:

```text
D:/Ky8/MSS301/FA26_MSS301_SE1946-JV/ats-config-repo
```

URI minh họa trong yêu cầu `file:///D:/Ky8/MSS301/ats-config-repo` thiếu `FA26_MSS301_SE1946-JV`; `Test-Path D:/Ky8/MSS301/ats-config-repo` trả `False`. Tuy nhiên **source thực tế không còn dùng URI đó**. Source đang có đúng:

```text
uri: file:///D:/Ky8/MSS301/FA26_MSS301_SE1946-JV
search-paths: ats-config-repo
```

Theo [Spring Cloud Config Git Backend](https://docs.spring.io/spring-cloud-config/reference/server/environment-repository/git-backend.html), backend Git dùng repository URI và hỗ trợ search paths cho các thư mục cấu hình bên trong repository. Với topology này, dùng Git root + `search-paths` là phương án đúng. Không trỏ Git URI tới subfolder không có repository riêng.

Lỗi đường dẫn hoặc label có thể gây lỗi truy cập repository về sau; chúng không giải thích việc URI đã viết trong một file không vào classpath vẫn bị báo thiếu. Audit không thử endpoint nên chưa khẳng định mọi thao tác Git lúc chạy sẽ thành công.

## 8. IntelliJ Run Configuration

Nguồn: `.idea/workspace.xml`, configuration `ConfigServerApplication`.

| Thuộc tính | Giá trị / giới hạn quan sát |
| --- | --- |
| Type | `SpringBootApplicationConfigurationType` |
| Module | `config-server` |
| Main class | `fu.ats.ConfigServerApplication` |
| Before launch | `Make` enabled |
| Working directory | Không khai báo tường minh trong XML; không khẳng định resolved directory của phiên đã chạy |
| Active profiles | Không khai báo tường minh |
| VM options | Không khai báo tường minh |
| Program arguments | Không khai báo tường minh |
| Environment overrides | Không có khai báo tường minh trong configuration |
| Saved status | `FAILED` |

Trong process shell audit không có biến khớp `SPRING*`, `JAVA_TOOL_OPTIONS`, `JDK_JAVA_OPTIONS`, `MAVEN_OPTS`, bao gồm `SPRING_CONFIG_LOCATION`, `SPRING_CONFIG_NAME`, `SPRING_PROFILES_ACTIVE`, `SPRING_CLOUD_CONFIG_SERVER_GIT_URI`. Điều này không chứng minh môi trường IntelliJ giống shell audit. Không có shared `.run.xml` được tìm thấy trong workspace.

`.idea/misc.xml` lưu JDK `temurin-24`, language level `JDK_24_PREVIEW`, trong khi POM compiler là 21. Đây là thông tin môi trường, không phải bằng chứng gây lỗi thiếu URI; không đề xuất đổi version để xử lý lỗi này.

## 9. Root Cause

**Nguyên nhân chính: đặt cấu hình trong `src/main/resource` thay vì `src/main/resources`, nên file không được copy vào classpath.**

Chuỗi evidence: file sai thư mục tồn tại → thư mục chuẩn không có file → POM không khai báo thư mục resource riêng → compiled main class tồn tại nhưng `target/classes/application.yml` không tồn tại → IntelliJ chạy đúng main class/module mà không có location override → Git backend thiếu property dù YAML source có property đúng.

Xếp hạng chẩn đoán cho lỗi được cung cấp (ước lượng kỹ thuật, không phải thống kê):

1. **99% — config không được load do sai tên thư mục resource.** Bằng chứng trực tiếp ở source, POM và build output.
2. **1% — trạng thái runtime/IDE chưa quan sát khác với metadata trên đĩa.** Chưa có command line và environment của process thất bại; chỉ giữ một phần bất định này, không có evidence về override.
3. **Không có evidence cho URI/label sai trong source hiện tại.** URI trỏ root thật, label tồn tại, search path đúng; không xếp chúng thành nguyên nhân có xác suất cao chỉ từ ví dụ cũ trong yêu cầu.

## 10. Correct Configuration Proposal

Giữ nguyên YAML tại mục 4 và đặt vào đường dẫn chuẩn:

```text
ats_parrent/infras/config-server/src/main/resources/application.yml
```

Đề xuất chuyển thư mục `resource` thành `resources` sau audit. Chưa thực hiện. Không cần sửa URI, label, search-paths, main class hoặc POM cho lỗi được xác định. Không cần tạo repository riêng hay chuyển native/composite backend.

## 11. Exact Files That Need To Change

| File / hành động đề xuất | Lý do |
| --- | --- |
| Chuyển `ats_parrent/infras/config-server/src/main/resource/application.yml` sang `ats_parrent/infras/config-server/src/main/resources/application.yml` | Để Maven/IDE nhận resource chuẩn và Boot load YAML từ classpath |

Không có thay đổi nội dung YAML cần thiết dựa trên source hiện tại. Không có thay đổi bắt buộc tại parent POM, module POM, Java main, `.idea/workspace.xml` hoặc `ats-config-repo/*.yml`.

File duy nhất được tạo trong audit: `docs/CONFIG_SERVER_STARTUP_FAILURE_AUDIT.md`.

## 12. Verification Steps

Các bước sau là hướng dẫn **sau khi thực hiện sửa**, chưa chạy trong audit:

1. Chuyển file sang `src/main/resources/application.yml`; reload Maven project trong IntelliJ.
2. Từ project root chạy `mvn -f ats_parrent/pom.xml -pl infras/config-server -am process-resources`. Không cần build toàn bộ service hoặc đổi dependency.
3. Xác nhận `ats_parrent/infras/config-server/target/classes/application.yml` tồn tại và có cùng URI/label/search-paths như source. Nếu output IDE vẫn cũ, rebuild module rồi kiểm tra lại resource.
4. Chạy IntelliJ configuration `ConfigServerApplication`, đúng module `config-server`. Kiểm tra resolved working directory, profile, VM options, program arguments và environment thực tế. Bỏ override chỉ nếu nó thực sự sai; không suy đoán cần sửa metadata IDE.
5. Có thể thêm program argument `--logging.level.org.springframework.boot.context.config=TRACE` để xem config được load từ đâu; xác nhận startup thành công trên port `8888`.
6. URL test đầu tiên dễ dùng, sử dụng default label đã cấu hình: `http://localhost:8888/job-service/dev`.
7. Test label tường minh bằng PowerShell:

   ```powershell
   $label = [Uri]::EscapeDataString('Hòa-Đẹp-Trai-Vũ-Trụ')
   Invoke-RestMethod -Uri ("http://localhost:8888/job-service/dev/" + $label)
   ```

   URL dạng đọc được: `http://localhost:8888/job-service/dev/Hòa-Đẹp-Trai-Vũ-Trụ` (HTTP client cần encode label).

8. Response phải có `name=job-service`, profile `dev`, label phù hợp và property sources từ `ats-config-repo/job-service-dev.yml` cùng cấu hình chung. Đối chiếu `server.port=8081`, `ats.environment=development`, `ats.message=Hello from ATS Config Server`, `ats.config.source=centralized-config`. Port `8081` trong response là config cho client, không đổi port Config Server `8888`.

Audit đã kiểm tra YAML bằng parser, bytes/BOM/tab, Git root/branch/status/tracked files, POM, main class, resource output hiện có và metadata IDE. Chưa xác nhận server startup hay HTTP response sau sửa vì yêu cầu chỉ audit.
