#!/bin/bash

readonly account_no="$1"

readonly ANSI_COLOR_RED='\e[31m'
readonly ANSI_COLOR_GREEN='\e[32m'
readonly ANSI_COLOR_RESET='\e[0m'

if [[ -z "${account_no}" ]]
then
  echo 'account_no must be set' >&2
  exit 1
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
  curl --max-time 1 -sS -X POST "localhost:8080/accounts/${account_no}/deposit?amount=100" 1> >(decorate_stdin) 2> >(decorate_stdout)
done
