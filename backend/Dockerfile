# 第一阶段：构建阶段
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app

# 将后端文件夹内的所有内容复制到镜像中
COPY . .

# 执行构建，跳过测试以加快速度
RUN chmod +x mvnw && ./mvnw clean package -DskipTests

# 第二阶段：运行阶段
FROM openjdk:17-jdk-slim
WORKDIR /app

# 从构建阶段复制生成的 JAR 包
# 注意：你的项目打包后通常在 target 目录下
COPY --from=build /app/target/*.jar app.jar

# 暴露后端默认端口 8080
EXPOSE 8080

# 启动命令
ENTRYPOINT ["java", "-jar", "app.jar"]
