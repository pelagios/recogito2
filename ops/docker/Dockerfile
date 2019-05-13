# We need JDK 8 and SBT 1.0.x
FROM hseeberger/scala-sbt:8u151-2.12.4-1.0.4 AS build

# Based on https://github.com/Wadjetz/scala-sbt-nodejs/blob/master/Dockerfile
# Get node and libvips
RUN curl -sL https://deb.nodesource.com/setup_10.x | bash - && \
    apt install -y nodejs libvips-tools && \
    apt-get clean

#For now we work with the master branch
RUN git clone https://github.com/pelagios/recogito2 /usr/share/recogito

# Move to workdir
WORKDIR /usr/share/recogito

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

# Let's create an actual distribution
RUN sbt dist

# And unzip it somewhere useful

RUN unzip target/universal/recogito2-2.2.zip -d /opt/ 

# END OF BUILD STAGE
# To slim down the final production image, we begin a new stage

FROM openjdk:8-jre

# This is the dist stage

# We still need libvips
RUN apt-get update && \
    apt-get install -y libvips-tools && \
    apt-get clean

# But we don't need any of the other build artifacts - just grab the dist
COPY --from=build /opt/recogito2-2.2/ /opt/recogito/

WORKDIR /opt/recogito/

# You want to mount a configuration in /opt/recogito/conf/application.conf

# For production use, you also want to mount the relevant upload directory
# for persistance - by default that would be /opt/recogito/uploads
# though it is configurable in application.conf

# Open the port we're running on
EXPOSE 9000
CMD ["/opt/recogito/bin/recogito2"]

