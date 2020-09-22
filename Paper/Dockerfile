#######################
# Build paperclip.jar #
#######################
FROM maven:3-openjdk-11 as loadpaper

# --------------
# Define version
# --------------
ARG VERSION=latest

# -----------
# Build paper
# -----------
COPY Paper/buildPaper.sh /tmp/
RUN sh /tmp/buildPaper.sh

#####################
# Build Start.class #
#####################
FROM adoptopenjdk/openjdk11:alpine-slim as starter

# ------------------
# Compile Start.java
# ------------------
COPY StartProgram/* /tmp/
RUN sh /tmp/buildStart.sh

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
  COMMAND="java -Xmx2G -Xms2G -XX:+UseG1GC -XX:+UnlockExperimentalVMOptions -XX:MaxGCPauseMillis=100 -XX:+DisableExplicitGC -XX:TargetSurvivorRatio=90 -XX:G1NewSizePercent=50 -XX:G1MaxNewSizePercent=80 -XX:InitiatingHeapOccupancyPercent=10 -XX:G1MixedGCLiveThresholdPercent=50 -XX:+AlwaysPreTouch -XX:+UseLargePagesInMetaspace -Dcom.mojang.eula.agree=true -Dfile.encoding=UTF-8 -jar /opt/minecraft/paperclip.jar nogui" \
  CLASSPATH=/opt/start

# --------------------------------
# Copy files from the build images
# --------------------------------
COPY --from=loadpaper /tmp/paper/paperclip.jar /opt/minecraft/
COPY --from=starter /tmp/*.class /opt/start/

# ------------------
# Add user minecraft
# ------------------
RUN mkdir -p /mnt/minecraft \ 
 && apk add --no-cache -X http://dl-cdn.alpinelinux.org/alpine/edge/community openjdk11-jre-headless \
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
