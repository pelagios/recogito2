# Notes on Backup and Restore

* `backup.sh` provides an example script you can use as a guideline for implementing backup of
  the contents of the database, the index, and the user uploads folder
* Additional configuration steps will be required on your system outside of this script, including
  - setting up a [read-only backup user on the database](https://github.com/pelagios/recogito2/wiki/Notes-on-Postgres-backup)
  - configuring a [snapshot repository on ElasticSearch](https://www.elastic.co/guide/en/elasticsearch/reference/2.4/modules-snapshots.html)

#### Restoring from backups

Index content can be restored via the ElasticSearch restore API.
```
POST /_snapshot/my_backup/snapshot_1/_restore
```

Database content can be restored via `psql`. Hint: run the restore process from the application
user, so that correct privileges are retained.
```
psql db_name < infile
```
__Important:__ you will need to re-assign read privileges to the backup user for subsequent
backups to work!
