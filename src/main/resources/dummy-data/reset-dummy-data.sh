#!/bin/bash

# usage: ./reset-dummy-data.sh --path='/srv/ego/dummy-data' --user='dbuser' --log='/path/to/logfile.log'
# Note: This script assumes the user specified is the current user OR that there is a PGPASSFILE with the required password for that user.

while [ $# -gt 0 ]; do
  case "$1" in
    --path=*)
      path="${1#*=}"
      ;;
    --user=*)
      user="${1#*=}"
      ;;
    --log=*)
      log="${1#*=}"
      ;;
    *)
      printf "***************************\n"
      printf "* Error: Invalid argument.*\n"
      printf "***************************\n"
      exit 1
  esac
  shift
done

psql -w -U $user ego -a -f $path/01-insert-dummy-users.sql -L $log
psql -w -U $user ego -a -f $path/02-insert-dummy-groups.sql -L $log
psql -w -U $user ego -a -f $path/03-insert-dummy-applications.sql -L $log
psql -w -U $user ego -a -f $path/04-insert-dummy-rel-user-group.sql -L $log
psql -w -U $user ego -a -f $path/05-insert-dummy-rel-user-application.sql -L $log
psql -w -U $user ego -a -f $path/06-insert-dummy-rel-group-application.sql -L $log