#!/bin/bash

readonly account_no="$1"

if [[ -z "${account_no}" ]]
then
  echo 'account_no must be set' >&2
  exit 1
fi

while sleep 0.1
do
  echo "$(date '+%T.%3N') - $(curl --max-time 0.5 -sS -X POST "localhost:8080/accounts/${account_no}/deposit?amount=100" 2>&1)"
done
