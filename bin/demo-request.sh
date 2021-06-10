#!/bin/bash

readonly base_dir="$(cd "$(dirname "$0")"; pwd)"
readonly env_file="${base_dir}/../.env"
readonly account_no="$1"

readonly ANSI_COLOR_RED='\e[31m'
readonly ANSI_COLOR_GREEN='\e[32m'
readonly ANSI_COLOR_RESET='\e[0m'

if [[ -z "${account_no}" ]]
then
  echo 'account_no must be set' >&2
  exit 1
fi

if [[ -f "${env_file}" ]]
then
  # loads env file for docker-compose
  source "${env_file}"
fi

function timestamp {
  date '+%T.%3N'
}

function decorate_stdin {
  IFS='\n' read line
  if [ -n "${line}" ]
  then
    echo -e "$(timestamp) ${ANSI_COLOR_GREEN}[OK] ${line}${ANSI_COLOR_RESET}"
  fi
}

function decorate_stdout {
  IFS='\n' read line
  if [ -n "${line}" ]
  then
    echo -e "$(timestamp) ${ANSI_COLOR_RED}[NG] ${line}${ANSI_COLOR_RESET}" >&2
  fi
}

while sleep 0.1
do
  curl --noproxy localhost --max-time 1 -sS -X POST \
    "localhost:${APP_PORT:-8080}/accounts/${account_no}/deposit?amount=100&transactionId=$(date +%s%3N)" \
    1> >(decorate_stdin) 2> >(decorate_stdout)
done
