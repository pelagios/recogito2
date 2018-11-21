# Copy backups  

To copy the snapshots and user upload files to a remote server, for
backup purposes, you can use rsync from the remote server like so:

```
rsync -r -a -v -e ssh --delete backup_user@recogito_host:/path/to/backups/on/recogito_server /path/to/backup_folder/on/this_server/
```