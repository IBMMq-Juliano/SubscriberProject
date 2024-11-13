# Usar uma imagem base com OpenJDK 17
FROM openjdk:17-jdk-slim AS build

# Instalar o Maven
RUN apt-get update && apt-get install -y maven

# Definir o diretório de trabalho
WORKDIR /app

# Copiar o arquivo pom.xml e o código fonte para o diretório de trabalho
COPY pom.xml /app
COPY src /app/src

# Executar o Maven para compilar o projeto
RUN mvn clean install

# Usar a imagem base do OpenJDK 17 para executar a aplicação
FROM openjdk:17-jdk-slim

# Definir o diretório de trabalho
WORKDIR /app

# Copiar o JAR gerado para o contêiner
COPY --from=build /app/target/my-app.jar /app/my-app.jar

# Comando para executar a aplicação
CMD ["java", "-cp", "/app/my-app.jartarget/SubscriberProject-1.0-SNAPSHOT.jar", "com.exemplo.subscriber.Subscriber"]
