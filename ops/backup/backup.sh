#!/bin/bash

# An example shell script you can use as a basis to set up nightly backups for your own
# Recogito installation. This script produces snapshots of:
# - database
# - ElasticSearch index
# - user uploads folder
#
# Please refer to corresponding documentation (PostgreSQL, ElasticSearch) for
# additional prerequisites (read-only database user, ElasticSearch snapshot repository, etc.)

START_TIME=$SECONDS

TODAY="$(date +'%Y%m%d')"

LOG_FILE="/mnt/backup/backup$TODAY.log"

echo "Creating PostgreSQL DB dump" | tee -a "$LOG_FILE"
pg_dump --username=backup recogito > /mnt/backup/postgresql/backup$TODAY.sql

echo "Creating ElasticSearch snapshot" | tee -a "$LOG_FILE"
curl -XPUT http://localhost:9200/_snapshot/recogito/snapshot$TODAY?wait_for_completion=true >> $LOG_FILE

echo "Zipping user uploads" | tee -a "$LOG_FILE"
zip -r /mnt/backup/uploads/uploads$TODAY.zip uploads

ELAPSED_TIME=$(($SECONDS - $START_TIME))
echo "Done. Took $ELAPSED_TIME seconds" | tee -a "$LOG_FILE"
