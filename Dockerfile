FROM adoptopenjdk/openjdk11:jdk-11.0.6_10-slim as builder
WORKDIR /usr/src/app
ADD . .
RUN ./mvnw package -Dmaven.test.skip=true

#####################################################

FROM adoptopenjdk/openjdk11:jre-11.0.6_10
COPY --from=builder /usr/src/app/target/ego-*-exec.jar /usr/bin/ego.jar
ENV EGO_USER ego
ENV EGO_USER_ID 9999
ENV EGO_GROUP_ID 9999
ENV EGO_DIR /target
RUN addgroup --system --gid $EGO_GROUP_ID $EGO_USER \
	&& adduser --system --uid $EGO_USER_ID $EGO_USER_ID --ingroup $EGO_USER $EGO_USER  \
	&& mkdir -p $EGO_DIR \
	&& chown -R $EGO_USER $EGO_DIR 
USER $EGO_USER_ID
CMD ["java", "-jar", "/usr/bin/ego.jar"]
EXPOSE 8081/tcp
