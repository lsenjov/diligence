FROM java:8-alpine
MAINTAINER Your Name <you@example.com>

ADD target/uberjar/diligence.jar /diligence/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/diligence/app.jar"]
