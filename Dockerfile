FROM adoptopenjdk/openjdk11:jdk-11.0.6_10-alpine-slim as builder
WORKDIR /usr/src/app
ADD . .
RUN ./mvnw package -Dmaven.test.skip=true

#####################################################

FROM adoptopenjdk/openjdk11:jre-11.0.6_10-alpine
COPY --from=builder /usr/src/app/target/ego-*-exec.jar /usr/bin/ego.jar
ENV EGO_USER ego
ENV EGO_USER_ID 9999
ENV EGO_GROUP_ID 9999
ENV EGO_DIR /target
RUN addgroup -S -g $EGO_GROUP_ID $EGO_USER \
    && adduser -S -u $EGO_USER_ID -g $EGO_GROUP_ID $EGO_USER  \
    && mkdir -p $EGO_DIR \
    && chown -R $EGO_USER $EGO_DIR 
USER $EGO_USER
ENTRYPOINT ["java", "-jar", "/usr/bin/ego.jar"]
EXPOSE 8081/tcp
