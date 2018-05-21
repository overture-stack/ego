FROM java:8-jdk

ARG MAVEN_VERSION=3.5.3
ARG SHA=b52956373fab1dd4277926507ab189fb797b3bc51a2a267a193c931fffad8408
ARG BASE_URL=https://apache.osuosl.org/maven/maven-3/${MAVEN_VERSION}/binaries

RUN mkdir -p /usr/share/maven /usr/share/maven/ref \
  && curl -fsSL -o /tmp/apache-maven.tar.gz ${BASE_URL}/apache-maven-${MAVEN_VERSION}-bin.tar.gz \
  && echo "${SHA}  /tmp/apache-maven.tar.gz" | sha256sum -c - \
  && tar -xzf /tmp/apache-maven.tar.gz -C /usr/share/maven --strip-components=1 \
  && rm -f /tmp/apache-maven.tar.gz \
  && ln -s /usr/share/maven/bin/mvn /usr/bin/mvn

WORKDIR /usr/src/app

# copy just the pom.xml and install dependencies for caching
ADD pom.xml .
RUN mvn verify clean --fail-never

ADD . .

RUN mkdir -p /srv/ego/install \
    && mkdir -p /srv/ego/exec \
    && mvn package -Dmaven.test.skip=true \
    && mv /usr/src/app/target/ego-*-SNAPSHOT-exec.jar /srv/ego/install/ego.jar \
    && mv /usr/src/app/src/main/resources/scripts/run.sh /srv/ego/exec/run.sh

# setup required environment variables
ENV EGO_INSTALL_PATH /srv/ego

# start ego server
WORKDIR $EGO_INSTALL_PATH
CMD $EGO_INSTALL_PATH/exec/run.sh
