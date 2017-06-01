FROM openjdk:8-jdk-alpine

LABEL maintainer "docker@marcermarc.de"

# -------------------------------------
# Define version, heap and extra params
# -------------------------------------
#ARG VERSION=1.11.2

# -----------------------------------------------------------------------------------------------------------
# Default extra params from:
#  https://aquifermc.org/threads/optimizing-paper-remove-lag-spikes-fix-tps-improve-performance.101/#post-822
# Set the classpath can start the Start.class
# -----------------------------------------------------------------------------------------------------------
ENV VERSION=1.11.2 \
  HEAP=2G \
  PARAMS="-XX:+UseG1GC -XX:+UnlockExperimentalVMOptions -XX:MaxGCPauseMillis=100 -XX:+DisableExplicitGC -XX:TargetSurvivorRatio=90 -XX:G1NewSizePercent=50 -XX:G1MaxNewSizePercent=80 -XX:InitiatingHeapOccupancyPercent=10 -XX:G1MixedGCLiveThresholdPercent=50 -XX:+AggressiveOpts -XX:+AlwaysPreTouch -XX:+UseLargePagesInMetaspace" \
  CLASSPATH=/opt/start

# --------------------------
# Download and install forge
# Compile Start.java
# Add user minecraft
# --------------------------
RUN mkdir -p /opt/minecraft /mnt/minecraft /opt/start /tmp/install \
  && apk add --update --no-cache wget \
  && cd /tmp/install \
  && wget http://files.minecraftforge.net/maven/net/minecraftforge/forge/promotions_slim.json \
  && FORGE_VERSION="$(grep -e $VERSION-latest promotions_slim.json | sed -e 's|    "'"$VERSION"'-latest": "||g' -e 's|"||g' -e 's|,||g')" \
  && wget "https://files.minecraftforge.net/maven/net/minecraftforge/forge/${VERSION}-${FORGE_VERSION}/forge-${VERSION}-${FORGE_VERSION}-installer.jar" \
  && mv forge-*.jar forge-install.jar \
  && cd /opt/minecraft \
  && java -jar /tmp/install/forge-install.jar --installServer \
  && mv forge-*.jar forge.jar \
  && rm -rf /tmp/install /opt/minecraft/forge-install.jar.log /opt/minecraft/mods \
  && cd /opt/start \
  && wget https://raw.githubusercontent.com/marcermarc/DockerMinecraft/master/StartProgram/Start.java \
  && javac /opt/start/Start.java \
  && chmod +x /opt/start/Start.class \
  && apk del wget \
  && adduser -D minecraft \
  && chown -R minecraft /mnt/minecraft /opt/minecraft \
  && chmod -R 777 /mnt/minecraft /opt/minecraft
  
# ------------
# Expose ports
# ------------
EXPOSE 25565:25565/tcp 25565:25565/udp 25575:25575/tcp

# -------------
# Define volume
# -------------
VOLUME ["/mnt/minecraft"]

# ----------------
# Set startup user
# ----------------
USER minecraft

# ------------------------------------------
# Start the Start.class with two parameters:
#  1. The minecraft execute command
#  2. The minecraft workdir {$PARAMS} 
# ------------------------------------------
ENTRYPOINT ["java", "Start", "java -Xmx&HEAP& -Xms&HEAP& &PARAMS& -d64 -jar /opt/minecraft/forge.jar nogui", "/mnt/minecraft"]