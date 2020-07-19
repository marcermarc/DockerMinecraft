######################
# Download Minecraft #
######################
FROM alpine:latest as loadminecraft

# --------------
# Define version
# --------------
ARG VERSION=latest

# -------------------------
# Download minecraft server
# -------------------------
RUN apk add --update --no-cache jq curl \
 && if [ "$VERSION" = "latest" ]; then \
      export INDEX="$(curl "https://launchermeta.mojang.com/mc/game/version_manifest.json" | jq --raw-output '.latest.release')" \
    ; else \
      export INDEX="${VERSION}" \
    ; fi \
 && cd /tmp \
 && curl -o /tmp/minecraft-server.jar $(curl $(curl https://launchermeta.mojang.com/mc/game/version_manifest.json | jq --arg INDEX "$INDEX" --raw-output '.versions | map(select(.id==$INDEX))[0].url') | jq --raw-output '.downloads.server.url') 

#####################
# Build Start.class #
#####################
FROM openjdk:8-jdk-alpine as starter

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
# -----------------------------------------------------------------------------------------------------------
ENV WORKDIR="/mnt/minecraft" \
  COMMAND="java -Xmx2G -Xms2G -XX:+UseG1GC -XX:+UnlockExperimentalVMOptions -XX:MaxGCPauseMillis=100 -XX:+DisableExplicitGC -XX:TargetSurvivorRatio=90 -XX:G1NewSizePercent=50 -XX:G1MaxNewSizePercent=80 -XX:InitiatingHeapOccupancyPercent=10 -XX:G1MixedGCLiveThresholdPercent=50 -XX:+AggressiveOpts -XX:+AlwaysPreTouch -XX:+UseLargePagesInMetaspace -d64 -Dfile.encoding=UTF-8 -jar /opt/minecraft/minecraft-server.jar nogui" \
  CLASSPATH=/opt/start

# --------------------------------
# Copy files from the build images
# --------------------------------
COPY --from=loadminecraft /tmp/minecraft-server.jar /opt/minecraft/
COPY --from=starter /tmp/*.class /opt/start/

# ------------------
# Add user minecraft
# ------------------
RUN mkdir -p /mnt/minecraft \ 
 && apk add --no-cache openjdk8-jre-base \
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