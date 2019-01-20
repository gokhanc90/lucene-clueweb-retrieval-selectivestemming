#!/usr/bin/env bash

collection=CW09B
tags=NoStem_SnowballEng
metrics=(MAP NDCG20 NDCG100)
selectionMethods=(MSTTF MSTDF LSTDF TFOrder DFOrder KendallTauTFOrder KendallTauDFOrder MSTTFBinning MSTDFBinning TFOrderBinning DFOrderBinning KendallTauTFOrderBinning KendallTauDFOrderBinning)
kendallTauThreshold=(0.55 0.65 0.75 0.85 0.95)

path=results/${tags}/residualNeeds/${collection}
mkdir -p ${path}

for m in ${metrics[*]}; do
	for s in ${selectionMethods[*]}; do
		if [[ ${s} == KendallTau* ]]; then
			for th in ${kendallTauThreshold[*]}; do
				./run.sh SelectiveStemming -collection ${collection}  -tags ${tags} -residualNeeds -metric ${m} -selection ${s} -KTT ${th} 1>${path}/${collection}_${m}_${s}_${th}.txt 2>${path}/FEATURE_${collection}_${m}_${s}_${th}.txt
			done
		else
			./run.sh SelectiveStemming -collection ${collection} -tags ${tags}  -residualNeeds -metric ${m} -selection ${s} 1> ${path}/${collection}_${m}_${s}.txt 2>${path}/FEATURE_${collection}_${m}_${s}.txt
		fi
		
	done
	
done
