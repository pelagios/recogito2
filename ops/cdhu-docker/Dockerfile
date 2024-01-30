# We need JDK 8 and SBT 1.0.x
FROM sbtscala/scala-sbt:eclipse-temurin-jammy-8u392-b08_1.9.7_2.13.12 As build

# Based on https://github.com/Wadjetz/scala-sbt-nodejs/blob/master/Dockerfile

# install node
RUN  apt-get update &&  apt-get install -y ca-certificates curl gnupg
RUN curl -fsSL https://deb.nodesource.com/gpgkey/nodesource-repo.gpg.key |  gpg --dearmor -o /etc/apt/keyrings/nodesource.gpg
RUN echo "deb [signed-by=/etc/apt/keyrings/nodesource.gpg] https://deb.nodesource.com/node_20.x nodistro main" |  tee /etc/apt/sources.list.d/nodesource.list
RUN apt-get update &&  apt-get install nodejs -y
RUN apt-get install unzip
RUN apt-get clean

# Install livips and jre

RUN apt-get install libvips-tools -y
RUN apt-get install openjdk-8-jre -y


# For now we work with the master branch
RUN git clone https://github.com/Maen1/recogito.git /usr/share/recogito


# Move to workdir
WORKDIR /usr/share/recogito

#Copy application.conf ./conf/
RUN npm install -g webpack webpack-cli
RUN npm install
# We expect one failure due to jai_core being wrongly resolved
RUN sbt compile; exit 0
RUN sbt compile

# END OF SETUP
# For a development image, uncomment the following two lines, and remove or
# comment out the rest of the file. You probably want to mount a
# configuration file to /usr/share/recogito/conf/application.conf unless
# you want to redo all the configuration on each container reset
#EXPOSE 9000
#CMD ["sbt", "run"]
#CMD ["/bin/recogito2"]
# Let's create an actual distribution
RUN sbt dist

# And unzip it somewhere useful
RUN unzip target/universal/recogito2-2.2.zip -d /opt/

RUN ls -AlFh /opt

# END OF BUILD STAGE
# To slim down the final production image, we begin a new stage

FROM openjdk:8-jre

# This is the dist stage

# We still need libvips
RUN apt-get update && \
    apt-get install -y libvips-tools sendmail && \
    apt-get clean

RUN ls -AlFh /opt

# But we don't need any of the other build artifacts - just grab the dist
COPY --from=build /opt/recogito2-2.2/ /opt/recogito/

WORKDIR /opt/recogito/

# You want to mount a configuration in /opt/recogito/conf/application.conf

# For production use, you also want to mount the relevant upload directory
# for persistance - by default that would be /opt/recogito/uploads
# though it is configurable in application.conf

# This line added by JvB to enable logging
RUN mkdir /var/log/recogito

# Open the port we're running on
EXPOSE 9000
CMD ["/opt/recogito/bin/recogito2"]

