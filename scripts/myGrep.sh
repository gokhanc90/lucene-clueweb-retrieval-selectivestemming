#!/usr/bin/env bash

fileName=$1; #i.e. CW09B_*
searchFolder=$2; #i.e. results
searchPattern=$3; #i.e. SelectiveStemming\*\(.+\)\t.+\t.+$

if [ "$1" == "" ]; then
	echo "Set file name to search"
fi

if [ "$2" == "" ]; then
        searchFolder="results";
fi


if [ "$3" == "" ]; then
        searchPattern="SelectiveStemming\*\(.+\)\t.+\t.+$";
fi

echo ${searchPattern}

grep -rHP -e "${searchPattern}" --include="${fileName}" --color='auto' ${searchFolder}
