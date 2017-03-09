#!/bin/bash

# Change this to your prefered timezone
echo "Set timezone to Europe/Vienna"
timedatectl set-timezone Europe/Vienna
echo " "

#-------------------------------------------------------------------------------
echo "Update the system"
echo "deb https://dl.bintray.com/sbt/debian /" | sudo tee -a /etc/apt/sources.list.d/sbt.list
sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 642AC823
sudo sh -c 'echo "deb http://apt.postgresql.org/pub/repos/apt/ `lsb_release -cs`-pgdg main" >> /etc/apt/sources.list.d/pgdg.list'
wget -q https://www.postgresql.org/media/keys/ACCC4CF8.asc -O - | sudo apt-key add -
apt-get update
#apt-get -y dist-upgrade
echo " "

#-------------------------------------------------------------------------------
echo "Install needed software"
sudo apt-get -y install postgresql postgresql-contrib git zip sbt unzip 

#required for the add-apt-repository command
sudo apt-get install software-properties-common python-software-properties libvips-tools -y
#install java openjdk8
sudo add-apt-repository ppa:openjdk-r/ppa -y
sudo apt-get update
sudo apt-get install openjdk-8-jdk  openjdk-8-jre -y

# scala install
sudo wget www.scala-lang.org/files/archive/scala-2.11.8.deb
sudo dpkg -i scala-2.11.8.deb

#-------------------------------------------------------------------------------
echo "Installing Lightbend activator"
sudo mkdir /data
sudo su
cd /data
wget http://downloads.typesafe.com/typesafe-activator/1.3.10/typesafe-activator-1.3.10.zip
unzip typesafe-activator-1.3.10.zip
PATH=$PATH:/data/activator-dist-1.3.10/bin; export PATH
activator --help

#-------------------------------------------------------------------------------
echo "Prepare and populate PostgreSQL"
echo "set postgres config to listen on localhost"
# switch to root to alter the config file
sudo su
sudo sed -i "s?#listen_addresses?listen_addresses?g" /etc/postgresql/9.6/main/postgresql.conf

echo "Start postgres server locally"
sudo service postgresql start

su postgres << EOF
psql -c "
  CREATE USER recogitodbu WITH PASSWORD 'recogitodb';
  ALTER USER recogitodbu WITH SUPERUSER;
  "
EOF
su postgres << EOF
psql -c "
  CREATE DATABASE recogito2;
  "
EOF
su postgres << EOF
psql -c "
  ALTER DATABASE recogito2 OWNER TO recogitodbu;
  ALTER SCHEMA public OWNER TO recogitodbu;
  "
EOF
#switch back to root user
#exit

#-------------------------------------------------------------------------------
echo "configuring Recogito system"
cd /data
git clone https://$GITUSER:$GITPWD@github.com/pelagios/recogito2.git

echo  "Configure and start recogito"
echo "create a new configration based on the template"
cd /data/recogito2
sudo cp conf/application.conf.template application.conf

echo "Adapt the play app settings"
sudo sed -i "s?db.default.username=\"postgres-username-goes-here\"?db.default.username=\"recogitodbu\"?g" application.conf
sudo sed -i "s?db.default.password=\"postgres-password-goes-here\"?db.default.password=\"recogitodb\"?g" application.conf
sudo cp application.conf conf/application.conf

#---------------------------------------------------------------------------------
# create a .pgpass file to enable access to recogito db without pw input
printf 'localhost:5432:recogito2:recogitodbu:recogitodb' > /root/.pgpass
chmod 600 /root/.pgpass
sudo service postgresql restart

#---------------------------------------------------------------------------------
echo "Start server in development mode"
cd /data/recogito2
sudo /data/activator-dist-1.3.10/bin/activator run

echo "Point your browser to http://localhost:9000"

