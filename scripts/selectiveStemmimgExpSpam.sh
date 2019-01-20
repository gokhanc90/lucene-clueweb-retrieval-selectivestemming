#!/usr/bin/env bash

spam=(45 50 55 60 65 70)
collections=(CW09B)
tagsList=(NoStem_SnowballEng NoStem_KStem)

#collection=CW09B
#tags=NoStem_SnowballEng
metrics=(MAP NDCG20 NDCG100)
selectionMethods=(MSTTF MSTDF LSTDF TFOrder DFOrder KendallTauTFOrder KendallTauDFOrder MSTTFBinning MSTDFBinning TFOrderBinning DFOrderBinning KendallTauTFOrderBinning KendallTauDFOrderBinning)
kendallTauThreshold=(0.55 0.65 0.75 0.85 0.95)

#mkdir -p results/${tags}/excludedOneTerm/${collection}
for sp in ${spam[*]}; do
	for collection in ${collections[*]}; do
		for tags in ${tagsList[*]}; do
			path=resultsSpam/${tags}/excludedOneTerm/${collection}/${sp}
			mkdir -p ${path}

			for m in ${metrics[*]}; do
				for s in ${selectionMethods[*]}; do
					if [[ ${s} == KendallTau* ]]; then
						for th in ${kendallTauThreshold[*]}; do
							./run.sh SelectiveStemming -collection ${collection} -tags ${tags} -excludeOneTermNeeds  -residualNeeds -spam ${sp} -metric ${m} -selection ${s} -KTT ${th} 1>${path}/${collection}_sp_${sp}_${m}_${s}_${th}.txt 2>${path}/FEATURE_${collection}_sp_${sp}_${m}_${s}_${th}.txt 
						 done
					else
						./run.sh SelectiveStemming -collection ${collection} -tags ${tags} -excludeOneTermNeeds  -residualNeeds -spam ${sp}  -metric ${m} -selection ${s} 1>${path}/${collection}_sp_${sp}_${m}_${s}.txt 2>${path}/FEATURE_${collection}_sp_${sp}_${m}_${s}.txt
					fi
		
				done
	
			done

		done
	done
done
