import glob
import json
import os
import requests
from datetime import datetime

MAX_AGE = 7

def getAgeDays(name):
    delta = datetime.now() - datetime.strptime(name, '%Y%m%d')
    return delta.days

def cleanupFiles(pattern):
    logfiles = glob.glob(pattern)
    for l in logfiles:
        age = getAgeDays(l[len(pattern) - 1:l.index('.')])
        if age > 7:
            print 'Deleting file ' + l + ' (' + str(age) + ' days old)'
            os.remove(l)

def cleanupLogfiles():
    cleanupFiles('/mnt/data/recogito/backup/backup*')

def cleanupPGDumps():
    cleanupFiles('/mnt/data/recogito/backup/postgresql/backup*')

def cleanupESSnapshots():
    r = requests.get('http://localhost:9200/_snapshot/recogito/_all')
    for snapshot in json.loads(r.text)['snapshots']:
        name = snapshot['snapshot']
        age = getAgeDays(name[8:])
        if age > 7:
            print 'Deleting snapshot ' + name + ' (' + str(age) + ' days old)'
            requests.delete('http://localhost:9200/_snapshot/recogito/' + name)

cleanupLogfiles()
cleanupPGDumps()
cleanupESSnapshots()
