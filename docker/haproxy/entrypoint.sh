#!/bin/bash

# forward from /dev/log to stdout
socat UNIX-RECV:/dev/log,mode=666 STDOUT &

exec docker-entrypoint.sh "$@"
