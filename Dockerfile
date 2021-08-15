FROM openjdk:8-jdk-alpine
VOLUME /tmp
ADD ./demojenkins.jar demojenkins.jar
ENTRYPOINT ["java","-jar","/demojenkins.jar", "&"]