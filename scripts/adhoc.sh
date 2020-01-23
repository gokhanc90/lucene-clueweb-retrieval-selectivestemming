#!/usr/bin/env bash

if [ -z "$TFD_HOME" ]; then
  TFD_HOME=~/TFD_HOME
fi

if [ "$1" = "parameter" ]; then
	RUNS=parameter_runs
    EVALS=parameter_evals
else
	RUNS=runs
    EVALS=evals
fi

echo "starting Ad Hoc TREC evaluator with RUNS = $RUNS and EVALS = $EVALS ..."

 qrels[1]=qrels.51-100-removed.txt
 qrels[2]=qrels.101-150-removed.txt
 qrels[3]=qrels.151-200-removed.txt


for T in 1 2 3; do
 printf "%s\n" ${qrels[${T}]}
done


for set in WSJ; do

if [ ! -d "${TFD_HOME}/${set}/${RUNS}" ]; then
       continue
fi

for tag in ${TFD_HOME}/${set}/${RUNS}/*; do
if [[ ! -d ${tag} ]]; then
    continue
fi
tag=$(basename "${tag}")
mkdir -p "$TFD_HOME/$set/${EVALS}/$tag"
# TREC Ad hoc Tracks from TREC-1 to TREC-3
for T in 1 2 3; do

    if [ ! -d "${TFD_HOME}/${set}/${RUNS}/${tag}/TREC${T}AdHoc" ]; then
        # Control will enter here if $DIRECTORY does not exist.
        echo "${TFD_HOME}/${set}/${RUNS}/${tag}/TREC${T}AdHoc does not exist!"
        continue
    fi

     mkdir -p "$TFD_HOME/$set/${EVALS}/$tag/TREC${T}AdHoc"
     mkdir -p "$TFD_HOME/$set/${EVALS}/$tag/TREC${T}AdHoc/trec_eval"

    for f in ${TFD_HOME}/${set}/${RUNS}/${tag}/TREC${T}AdHoc/*.txt; do
        ${TFD_HOME}/scripts/trec_eval -M1000 -q ${TFD_HOME}/topics-and-qrels/${qrels[${T}]} ${f} > "${TFD_HOME}/${set}/${EVALS}/${tag}/TREC${T}AdHoc/trec_eval/${f##/*/}" &
        for k in 20 100 1000; do
          mkdir -p "$TFD_HOME/$set/${EVALS}/$tag/TREC${T}AdHoc/$k"
          ${TFD_HOME}/scripts/gdeval.pl -k ${k} ${TFD_HOME}/topics-and-qrels/${qrels[${T}]} ${f} > "${TFD_HOME}/${set}/${EVALS}/${tag}/TREC${T}AdHoc/${k}/${f##/*/}" &
        done
    done
    wait

done
done
done