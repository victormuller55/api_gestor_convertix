# Estágio de Execução
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copia o jar que o Jenkins acabou de compilar para dentro da imagem
COPY target/gestor-0.0.1-SNAPSHOT.jar app.jar

# Expõe a porta interna do container
EXPOSE 5000

# Comando para rodar a aplicação
ENTRYMENT ["java", "-jar", "app.jar"]