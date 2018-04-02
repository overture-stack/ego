FROM java:8-jdk

ARG MAVEN_VERSION=3.5.2
ARG SHA=707b1f6e390a65bde4af4cdaf2a24d45fc19a6ded00fff02e91626e3e42ceaff
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

RUN mkdir -p /srv/ego
RUN mvn package -Dmaven.test.skip=true
WORKDIR /usr/src/app/target
RUN tar xvzf ego-*-dist.tar.gz --directory /srv/ego
RUN mv /srv/ego/ego* /srv/ego/install

# setup required environment variables
ENV EGO_INSTALL_PATH /srv/ego
ENV EGO_KEYSTORE_PATH $EGO_INSTALL_PATH/cert/ego-jwt.jks

# start ego server
WORKDIR $EGO_INSTALL_PATH
CMD $EGO_INSTALL_PATH/install/exec/run.sh
