##################
# Install Fabirc #
##################
FROM openjdk:14-jdk-alpine as installfabric

# --------------
# Define version
# --------------
ARG VERSION=latest

# ---------------------------------
# Download and install forge server
# ---------------------------------
RUN apk add --update --no-cache curl unzip \
 && mkdir /tmp/installer/ /tmp/installation/ \
 && curl -o /tmp/installer/fabric-installer.zip https://jenkins.modmuss50.me/job/FabricMC/job/fabric-installer/job/master/lastSuccessfulBuild/artifact/build/libs/fabric*.jar/*zip*/fabric-installer.zip \
 && unzip /tmp/installer/fabric-installer.zip -d /tmp/installer \
 && mv /tmp/installer/fabric-installer*.jar /tmp/installer/fabric-installer.jar \
 && if [ "$VERSION" = "latest" ]; then \
      java -jar /tmp/installer/fabric-installer.jar server -downloadMinecraft -dir /tmp/installation \
    ; else \
      java -jar /tmp/installer/fabric-installer.jar server -downloadMinecraft -dir /tmp/installation -mcversion ${VERSION} \
    ; fi

#####################
# Build Start.class #
#####################
FROM openjdk:14-jdk-alpine as starter

# ------------------
# Compile Start.java
# ------------------
RUN apk add --update --no-cache wget \
 && cd /tmp/ \
 && wget https://raw.githubusercontent.com/marcermarc/DockerMinecraft/master/StartProgram/Start.java \
 && javac Start.java \
 && chmod +x Start.class

#######
# Run #
#######
FROM alpine:latest

LABEL maintainer "docker@marcermarc.de"

# -----------------------------------------------------------------------------------------------------------
# Default extra params from:
#  https://aquifermc.org/threads/optimizing-paper-remove-lag-spikes-fix-tps-improve-performance.101/#post-822
# Set the classpath can start the Start.class
# The fabric parameters are set to not need the fabric-server-launcher.properties in volume
# -----------------------------------------------------------------------------------------------------------
ENV WORKDIR="/mnt/minecraft" \
  COMMAND="java -Xmx2G -Xms2G -XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC -Dfile.encoding=UTF-8 -Dfabric.gameJarPath=/opt/minecraft/server.jar -cp /opt/minecraft/* net.fabricmc.loader.launch.knot.KnotServer nogui" \
  CLASSPATH=/opt/start

# --------------------------------
# Copy files from the build images
# --------------------------------
COPY --from=installfabric /tmp/installation/ /opt/minecraft/
COPY --from=starter /tmp/*.class /opt/start/

# ------------------
# Add user minecraft
# ------------------
RUN mkdir -p /mnt/minecraft \ 
 && apk add --no-cache -X http://dl-cdn.alpinelinux.org/alpine/edge/testing openjdk14-jre-headless \
 && adduser -D minecraft -h /opt \
 && chown -R minecraft /mnt/minecraft /opt \
 && chmod -R 777 /mnt/minecraft /opt

# ------------
# Expose ports
# ------------
EXPOSE 25565:25565/tcp 25565:25565/udp

# -------------
# Define volume
# -------------
VOLUME ["/mnt/minecraft"]

# ----------------
# Set startup user
# ----------------
USER minecraft

# ---------------------
# Start the Start.class
# ---------------------
ENTRYPOINT ["java", "Start"]
