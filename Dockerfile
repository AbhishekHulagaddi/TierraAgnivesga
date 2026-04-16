FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

COPY . .

RUN chmod +x start.sh

CMD ["sh", "start.sh"]
