
# The first stage of our multi-stage build builds the kakadu libraries and binaries
FROM ubuntu:20.04 AS KAKADU_TOOL_CHAIN

ENV JAVA_HOME="/usr/lib/jvm/java-11-openjdk-amd64"

# First we build the Kakadu code (if we've been given a Kakadu version)
WORKDIR /build/kakadu/
COPY kakadu /build/kakadu/
RUN if [ ! -z "${kakadu.version}" ]; then \
      apt-get update -qq && DEBIAN_FRONTEND=noninteractive apt-get install -qq --no-install-recommends \
      openjdk-11-jdk-headless${openjdk.version} \
      gcc${gcc.version} \
      make${make.version} \
      build-essential${build.essential.version} \
      libtiff-tools${libtiff.version} \
      libtiff5${libtiff.version} \
      libtiff5-dev${libtiff.version} && \
      cd /build/kakadu/"${kakadu.version}"/make && \
      BUILD_ARCH="Linux-x86-64-gcc" && \
      make -f Makefile-$BUILD_ARCH && \
      cp ../lib/$BUILD_ARCH/*.so /usr/local/lib && \
      cp ../bin/$BUILD_ARCH/kdu_compress /usr/local/bin && \
      cp ../bin/$BUILD_ARCH/kdu_expand /usr/local/bin && \
      cp ../bin/$BUILD_ARCH/kdu_jp2info /usr/local/bin ; \
    else \
      touch /usr/local/lib/placeholder.so && \
      touch /usr/local/bin/kdu_placeholder ; \
    fi

# This is the Docker image we are actually going to use
FROM ubuntu:20.04

# Sets this for the build, but not persisted in the image
ARG DEBIAN_FRONTEND=noninteractive

# Copy over our Kakadu libraries
ENV LD_LIBRARY_PATH=/usr/local/lib
COPY --from=KAKADU_TOOL_CHAIN /usr/local/lib/*.so /usr/local/lib/
COPY --from=KAKADU_TOOL_CHAIN /usr/local/bin/kdu_* /usr/local/bin/

# Copy over our Bucketeer application
WORKDIR /usr/local/bucketeer
COPY maven/${project.artifactId}-${project.version}.jar .

EXPOSE 8888

# Update packages and install tools
#  Removing /var/lib/apt/lists/* prevents using `apt` unless you do `apt update` first
RUN apt-get update -qq && \
    apt-get install --reinstall -y locales && \
    apt-get install -qq --no-install-recommends \
    libtiff-tools${libtiff.version} \
    libtiff5${libtiff.version} \
    libopenjp2-tools${libopenjp2.version} \
    openjdk-11-jre-headless${openjdk.version} \
    python2${python2.version} \
    < /dev/null > /dev/null && \
    rm -rf /var/lib/apt/lists/* && \
    sed -i 's/# en_US.UTF-8 UTF-8/en_US.UTF-8 UTF-8/' /etc/locale.gen && \
    locale-gen en_US.UTF-8

# Set our UTF-8 locale
ENV LANG en_US.UTF-8
ENV LANGUAGE en_US
ENV LC_ALL en_US.UTF-8

RUN dpkg-reconfigure --terse --frontend noninteractive locales

# Set up our configuration files
COPY docker-entrypoint.sh /usr/local/bin/
COPY configs/bucketeer.properties.tmpl /etc/bucketeer/bucketeer.properties.tmpl
COPY configs/bucketeer.properties.default /etc/bucketeer/bucketeer.properties.default
COPY configs/logback.xml /etc/bucketeer/logback.xml

# Create a non-privileged user
RUN adduser --system bucketeer
WORKDIR /home/bucketeer

# Set permissions
RUN mkdir -p /var/log/bucketeer /var/cache/bucketeer \
 && touch /etc/bucketeer/bucketeer.properties \
 && chown -R bucketeer /var/log/bucketeer /var/cache/bucketeer \
    /etc/bucketeer/bucketeer.properties /usr/local/bin/docker-entrypoint.sh

# Clean up the placeholder files if we built without kakadu
RUN if [ -z "${kakadu.version}" ]; then \
      rm -rf /usr/local/lib/placeholder.so && \
      rm -rf /usr/local/bin/kdu_placeholder ; \
    fi

# Switch to our non-privileged user
USER bucketeer

# Prepare our runtime environment
ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]
ENV CACHE_DIR="-Dvertx.cacheDirBase=/tmp"
ENV CONFIG_FILE="-Dvertx-config-path=/etc/bucketeer/bucketeer.properties"
ENV JAR_PATH="/usr/local/bucketeer/bucketeer-${project.version}.jar"
ENV BOOT_CLASSPATH="-Xbootclasspath/a:/etc/bucketeer"
ENV JAVA_HEAP_SIZE="2g"
CMD ["sh", "-c", "java $BOOT_CLASSPATH $CACHE_DIR $CONFIG_FILE -Xmx$JAVA_HEAP_SIZE -jar $JAR_PATH"]
