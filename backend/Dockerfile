# 第一阶段：构建阶段 (使用 eclipse-temurin)
FROM maven:3.8.5-eclipse-temurin-17 AS build
WORKDIR /app

# 复制后端所有内容
COPY . .

# 移除 Windows 换行符并增加执行权限
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

# 执行构建
RUN ./mvnw clean package -DskipTests

# 第二阶段：运行阶段 (同样换成 eclipse-temurin)
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# 从构建阶段复制生成的 JAR 包
COPY --from=build /app/target/*.jar app.jar

# 暴露端口
EXPOSE 8080

# 启动命令
ENTRYPOINT ["java", "-jar", "app.jar"]
