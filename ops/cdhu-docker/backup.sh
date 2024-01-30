START_TIME=$SECONDS

TODAY="$(date +'%Y%m%d')"

LOG_FILE="./backup$TODAY.log"

echo "Creating PostgreSQL DB dump" | tee -a "$LOG_FILE"

docker exec -i postgres pg_dump -U recogito_backup recogito > ./backup$TODAY.sql


echo "Zipping all" | tee -a "$LOG_FILE"

backup_date=$(date +"%Y%m%d")
source_folder="./recogito-server"
backup_destination="./recogito-backup"

tar -czvf "$backup_destination/recogito-backup-latest.tar.gz" "$source_folder"


ELAPSED_TIME=$(($SECONDS - $START_TIME))
echo "Done. Took $ELAPSED_TIME seconds" | tee -a "$LOG_FILE"
