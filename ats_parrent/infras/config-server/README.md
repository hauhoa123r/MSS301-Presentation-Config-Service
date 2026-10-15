# Phần Hoàng Đức Thuận: Xây dựng Config Server

## 1. Mục tiêu và giới hạn

Phần của Thuận nhận yêu cầu tên service + profile + label, đọc Git và trả cấu hình.
Luồng: Postman/trình duyệt hoặc Config Client -> Config Server -> Git của Nam.
Config Server không xử lý ứng tuyển, không lưu ứng viên, không cần database/Eureka để demo độc lập.
Kết quả server trả đúng chưa đồng nghĩa service ATS đã dùng cấu hình đó: Tiến Nam/Sơn tích hợp client.

## 2. Những file đã thêm

- pom.xml: dependencies, Java 21, plugin tạo JAR chạy được.
- src/main/java/fu/ats/configserver/ConfigServerApplication.java: điểm khởi động.
- src/main/resources/application.yml: cổng, địa chỉ Git, nhánh, thời gian chờ.
- scripts/verify-config.ps1: kiểm tra health và 9 tổ hợp service/profile.
- ../.. /pom.xml (ats_parrent/pom.xml): thêm module infras/config-server.
- .gitignore: bỏ qua target và log.

Module dùng parent Spring Boot 4.0.8 riêng và Cloud BOM 2025.1.3.
ATS cha hiện khai báo Boot 4.1.1; bảng hỗ trợ Cloud 2025.1 chỉ ra Boot 4.0.x.
Không đổi phiên bản các module có sẵn. Maven aggregator vẫn liệt kê module này;
aggregator và parent dependency management không bắt buộc là cùng một POM.
https://github.com/spring-cloud/spring-cloud-release/wiki/Supported-Versions

## 3. Hiểu code

@SpringBootApplication: khởi động Spring Boot, tự cấu hình các thư viện cần thiết.
@EnableConfigServer: bật API cung cấp cấu hình; không cần tự viết controller đọc YAML.
SpringApplication.run: chạy ứng dụng.

spring-cloud-config-server: xử lý API cấu hình, đọc Git, tổng hợp các property sources.
spring-boot-starter-actuator: kiểm tra health.
spring-boot-maven-plugin: đóng gói JAR có thể chạy bằng java -jar.
Không cần thêm JPA, PostgreSQL hoặc Eureka Client vào module này.

## 4. Hiểu application.yml của server

- server.port: mặc định 8888; có thể đổi bằng CONFIG_SERVER_PORT.
- spring.application.name: config-server, tên ứng dụng server.
- git.uri: mặc định https://github.com/devnam1210/ats-config-repo.git.
- git.default-label: main; có thể đổi bằng CONFIG_GIT_LABEL.
- try-master-branch: false, tránh tự chuyển sang master khi main sai.
- clone-on-start: true, phát hiện lỗi kết nối Git ngay khi khởi động.
- timeout: 15 giây cho thao tác kết nối Git.
- management endpoints: chỉ health/info.

File này cấu hình chính server. Nó khác application.yml trên Git của Nam, nơi
chứa cấu hình chung cho các client.
Không đặt spring.config.import trong Config Server cho luồng demo này.
Không phải đổi profile của server sang dev/test/prod để lấy ba bộ cấu hình:
profile cần lấy được truyền trong URL.

## 5. Chạy bằng IntelliJ

1. Mở đúng D:/Ky_8/MSS301/MSS301-Presentation-Config-Service.
2. Reload Maven project. Nếu module chưa được nhận, thêm POM config-server làm Maven project.
3. Dùng JDK 21 trở lên; bài đã được kiểm tra bằng JDK 25, target Java 21.
4. Mở ConfigServerApplication.java, chọn Run main().
5. Đợi log Tomcat started on port 8888 và Started ConfigServerApplication.
6. Mở http://localhost:8888/actuator/health và thấy status UP.

Không cần PostgreSQL, Gateway, Eureka hoặc ba service nghiệp vụ chỉ để chạy phần Thuận.
Cần internet để server đọc GitHub. Không cần token cho repository public.
Trong cùng máy chỉ chạy một instance trên cổng 8888.

## 6. Chạy bằng PowerShell

Mở terminal tại thư mục gốc project:

    cd D:\Ky_8\MSS301\MSS301-Presentation-Config-Service
    mvn -B -ntp -f ats_parrent/infras/config-server/pom.xml package
    java -jar ats_parrent/infras/config-server/target/config-server-1.0-SNAPSHOT.jar

Giữ terminal chạy server. Dùng terminal thứ hai để kiểm tra.
Ctrl+C trong terminal chạy server để dừng.

Tùy chọn đổi cổng trước khi chạy:

    $env:CONFIG_SERVER_PORT = '8889'

Mặc định đã đúng repo Nam và main, không cần thêm biến môi trường.
Nếu thay địa chỉ server, nhớ đổi URL trong request và cấu hình client.

## 7. Demo thủ công: từng bước và điều cần nói

Bước 1 - Mở Git của Nam:
Chỉ các file application.yml, application-dev.yml, job-service.yml, job-service-dev.yml.
Nói: Đây là nơi lưu cấu hình, chưa phải một chương trình phục vụ cấu hình.

Bước 2 - Mở mã Config Server:
Chỉ @EnableConfigServer và git.uri, default-label.
Nói: Server kết nối kho Git và cung cấp API HTTP.

Bước 3 - Gọi:
http://localhost:8888/job-service/dev/main

Giải thích:
- job-service: tên ứng dụng cần cấu hình.
- dev: profile cần lấy.
- main: nhánh Git cần đọc.
Có thể bỏ /main để dùng default-label.

Bước 4 - Đọc JSON:
- name: job-service.
- profiles: dev.
- label: main.
- version: commit Git server đang phục vụ.
- propertySources: danh sách nguồn và các khóa/giá trị.
- ats.demo.message: job-service - DEV.
- ats.environment: dev.
- spring.datasource.url: placeholder có giá trị mặc định job_sdb.

Không nói JSON đã là kết nối database thành công. Server chỉ cung cấp thông số.
Placeholder mật khẩu còn nguyên là đúng: client giải quyết từ môi trường chạy của nó.
Danh sách propertySources được xếp nguồn ưu tiên cao trước. Khi trùng khóa,
giá trị của nguồn ưu tiên cao được dùng trong Environment phía server.
Client còn có các nguồn khác như biến môi trường/đối số dòng lệnh.

Bước 5 - So sánh:
http://localhost:8888/job-service/test/main
http://localhost:8888/job-service/prod/main

Test: thông điệp TEST, database mặc định job_sdb_test, ddl-auto validate.
Prod: thông điệp PROD, database phải cung cấp qua biến môi trường.
Không cần tạo database prod để chứng minh Config Server trả cấu hình prod.

Bước 6 - Kết:
Server đã đọc đúng Git và trả cấu hình theo service/profile.
Tiến Nam tiếp tục tích hợp client để job-service thực sự nhận và sử dụng cấu hình.

## 8. Kiểm tra tự động

Khi server đang chạy, tại thư mục gốc project:

    .\ats_parrent\infras\config-server\scripts\verify-config.ps1

Nếu dùng cổng khác:

    .\ats_parrent\infras\config-server\scripts\verify-config.ps1 -BaseUrl http://localhost:8889

Script đọc HTTP, không sửa Git, không thay database.
Kiểm tra health UP; đúng tên/profile/commit; đủ cấu hình chung và riêng;
đúng thông điệp; mật khẩu là placeholder; database test riêng;
cấu hình URL Feign của application-service không bị lẫn vào Job/Candidate.
Nếu Git đã sửa thông điệp demo, cập nhật kỳ vọng trong script hoặc hoàn tác commit demo.

## 9. Phối hợp

Nam: cung cấp repo, main, cấu trúc file; sửa nội dung cấu hình nếu sai.
Thuận: đảm bảo server đọc repo và trả đúng; bàn giao URL và hướng dẫn chạy.
Tiến Nam: thêm Config Client vào job-service, cấu hình tên/profile/import.
Sơn: áp dụng client cho các service còn lại và kiểm tra database/kết nối.
Hòa: refresh, rollback, lỗi vận hành. Thuận hỗ trợ chạy/dừng server khi cần.
Huyền: dùng sơ đồ đúng luồng yêu cầu/đáp ứng trong bài giới thiệu.

Trên một máy: server http://localhost:8888.
Trên máy khác: localhost là máy của chính người đó; phải dùng IP máy chạy server
và bảo đảm kết nối mạng được phép, hoặc mỗi người tự chạy server.
Server không tự làm client nhận cấu hình chỉ vì Git đã có file.

## 10. Lỗi thường gặp

- Port 8888 already in use: kiểm tra có server chạy sẵn không; dùng lại hoặc dừng
  đúng tiến trình mình đã chạy; không tắt tùy tiện tiến trình khác.
- Could not resolve dependencies: kiểm tra mạng và Maven Reload, chạy đúng POM module.
- Không clone được Git: kiểm tra URL, internet, nhánh và quyền truy cập.
- HTTP 200 nhưng thiếu file riêng: kiểm tra tên service/profile và propertySources.
  File chung có thể vẫn được trả về dù gõ sai tên service.
- / trả 404: dùng endpoint /job-service/dev/main, server không có trang chủ UI.
- /actuator/info trả {}: bình thường khi chưa khai báo thông tin ứng dụng.
- Database chưa chạy: không ảnh hưởng demo server thuần; ảnh hưởng phần client sau đó.
- Log server ghi profile default: bình thường; profile khách yêu cầu nằm trong URL.

## 11. Kết quả kiểm chứng ngày 14/10/2026

- Maven package: BUILD SUCCESS, executable JAR.
- Runtime: Spring Boot 4.0.8, Java 25.0.1, target Java 21.
- Đọc trực tiếp https://github.com/devnam1210/ats-config-repo.git.
- Commit cấu hình: 042dd44e81340ad9f0728be254463ce5fa56a591.
- Health UP và đủ 9/9 tổ hợp ba service x dev/test/prod PASS.
- Kiểm tra không lẫn URL Feign sang Job/Candidate PASS.
- Chưa tích hợp Config Client, chưa kiểm chứng database hay nghiệp vụ ATS.
- Không phải toàn bộ reactor ATS đã build: chỉ module config-server được build.

## 12. Nội dung trình bày gợi ý

Slide 1: Tại sao cần Config Server?
Git lưu file; các service cần một API thống nhất để lấy cấu hình theo tên/môi trường.
ATS có nhiều service và nhiều môi trường nên cần chọn đúng bộ thông số.

Slide 2: Xây dựng bằng gì?
Một ứng dụng Spring Boot + dependency Config Server + @EnableConfigServer.
application.yml server trỏ đến repo của Nam; cổng 8888.

Slide 3: Chứng minh hoạt động thế nào?
Yêu cầu /job-service/dev/main; đối chiếu JSON và nguồn Git.
Đổi dev sang test/prod để thấy cấu hình thay đổi. Bàn giao cho phần Config Client.

Tài liệu:
https://docs.spring.io/spring-cloud-config/reference/server.html
https://docs.spring.io/spring-cloud-config/reference/server/environment-repository/git-backend.html
