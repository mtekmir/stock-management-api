FROM openjdk:8-jre-alpine

RUN mkdir -p /opt/app

WORKDIR /opt/app

COPY ./target/scala-2.13/app-assembly.jar ./

EXPOSE 3111

CMD java -jar app-assembly.jar
