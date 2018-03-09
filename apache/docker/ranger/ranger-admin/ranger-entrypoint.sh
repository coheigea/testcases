#!/bin/sh

cd $RANGER_HOME
./setup.sh
ranger-admin start
# Keep the container running
tail -f /dev/null

