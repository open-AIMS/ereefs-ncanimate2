# Build a Docker image for development.

FROM openjdk:8-alpine

# Set the work directory.
WORKDIR /opt/app/bin

# Create a non-root user with no password and no home directory.
RUN addgroup ereefs && adduser --system ereefs --ingroup ereefs

# Add the main JAR file.
ARG NCANIMATE_JAR_NAME
ARG NCANIMATE_FRAME_JAR_NAME
COPY --chown=ereefs:ereefs target/${NCANIMATE_JAR_NAME} /opt/app/bin/
COPY --chown=ereefs:ereefs target/${NCANIMATE_FRAME_JAR_NAME} /opt/app/bin/

# Install dependencies in the docker container (alpine linux)
#     https://wiki.alpinelinux.org/wiki/Alpine_Linux_package_management
RUN apk update
RUN apk add ttf-freefont
RUN apk add ffmpeg

# Create an 'entrypoint.sh' script that executes the JAR file.
RUN echo "java -XX:MaxRAMPercentage=80.0 -jar /opt/app/bin/${NCANIMATE_JAR_NAME}" > entrypoint.sh
RUN chmod +x /opt/app/bin/entrypoint.sh

# Debugging - uncomment the following 2 lines to help debugging
#RUN ls -al /opt/app/bin
#RUN cat /opt/app/bin/entrypoint.sh

# Use the new user when executing.
USER ereefs

# Use the 'entrypoint.sh' script when executing.
ENTRYPOINT "/opt/app/bin/entrypoint.sh"
