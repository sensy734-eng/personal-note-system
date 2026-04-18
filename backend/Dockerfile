# 第一阶段：构建阶段 (使用内置 Maven 的镜像)
FROM maven:3.8.5-eclipse-temurin-17 AS build
WORKDIR /app

# 1. 复制项目描述文件
COPY pom.xml .

# 2. 预下载依赖 (可选优化：利用 Docker 缓存，以后改代码时构建会极快)
RUN mvn dependency:go-offline

# 3. 复制源代码并执行构建
COPY . .
# 注意：这里直接用 mvn，不用 ./mvnw，完美避开权限和换行符问题
RUN mvn clean package -DskipTests

# 第二阶段：运行阶段
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# 从构建阶段复制生成的 JAR 包
COPY --from=build /app/target/*.jar app.jar

# 暴露后端默认端口 8080
EXPOSE 8080

# 启动命令
ENTRYPOINT ["java", "-jar", "app.jar"]
# ... 前面保持不变 ...

# 运行阶段
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# 优化：创建一个上传目录防止权限问题
RUN mkdir -p /tmp/uploads && chmod 777 /tmp/uploads

# 优化：增加 JVM 内存限制和时区设置
ENTRYPOINT ["java", \
            "-Xmx300m", \
            "-Duser.timezone=GMT+08", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-jar", "app.jar"]
