#!/usr/bin/env bash

collection=MQ07
tags=NoStem_SnowballEng
metrics=(MAP NDCG20 NDCG100)
selectionMethods=(MSTTF MSTDF LSTDF TFOrder DFOrder KendallTauTFOrder KendallTauDFOrder MSTTFBinning MSTDFBinning TFOrderBinning DFOrderBinning KendallTauTFOrderBinning KendallTauDFOrderBinning)
kendallTauThreshold=(0.55 0.65 0.75 0.85 0.95)

path=resultsEightModel/${tags}/residualNeeds/${collection}
mkdir -p ${path}

for m in ${metrics[*]}; do
    if [${m} == NDCG20];then
        mdls=BM25k1.2b0.4_PL2c6.5_LGDc2.5_DirichletLMc1000.0_DPH_DFRee_DFIC_DLH13
    elif [${m} == NDCG100];then
        mdls=BM25k1.5b0.4_PL2c6.5_LGDc2.0_DirichletLMc900.0_DPH_DFRee_DFIC_DLH13
    elif [${m} == MAP];then
        mdls=BM25k1.2b0.4_PL2c6.5_LGDc2.0_DirichletLMc900.0_DPH_DFRee_DFIC_DLH13
    fi

	for s in ${selectionMethods[*]}; do
		if [[ ${s} == KendallTau* ]]; then
			for th in ${kendallTauThreshold[*]}; do
				./run.sh SelectiveStemming -collection ${collection}  -tags ${tags} -models ${mdls} -residualNeeds -metric ${m} -selection ${s} -KTT ${th} 1>${path}/${collection}_${m}_${s}_${th}.txt 2>${path}/FEATURE_${collection}_${m}_${s}_${th}.txt
			done
		else
			./run.sh SelectiveStemming -collection ${collection} -tags ${tags} -models ${mdls} -residualNeeds -metric ${m} -selection ${s} 1> ${path}/${collection}_${m}_${s}.txt 2>${path}/FEATURE_${collection}_${m}_${s}.txt
		fi
		
	done
	
done

#===============================================

collection=MQ08
tags=NoStem_SnowballEng
metrics=(MAP NDCG20 NDCG100)
selectionMethods=(MSTTF MSTDF LSTDF TFOrder DFOrder KendallTauTFOrder KendallTauDFOrder MSTTFBinning MSTDFBinning TFOrderBinning DFOrderBinning KendallTauTFOrderBinning KendallTauDFOrderBinning)
kendallTauThreshold=(0.55 0.65 0.75 0.85 0.95)

path=resultsEightModel/${tags}/residualNeeds/${collection}
mkdir -p ${path}

for m in ${metrics[*]}; do
    if [${m} == NDCG20];then
        mdls=BM25k1.7b0.4_PL2c5.0_LGDc2.0_DirichletLMc900.0_DPH_DFRee_DFIC_DLH13
    elif [${m} == NDCG100];then
        mdls=BM25k1.7b0.45_PL2c5.0_LGDc1.5_DirichletLMc800.0_DPH_DFRee_DFIC_DLH13
    elif [${m} == MAP];then
        mdls=BM25k1.1b0.45_PL2c5.0_LGDc1.5_DirichletLMc650.0_DPH_DFRee_DFIC_DLH13
    fi

	for s in ${selectionMethods[*]}; do
		if [[ ${s} == KendallTau* ]]; then
			for th in ${kendallTauThreshold[*]}; do
				./run.sh SelectiveStemming -collection ${collection}  -tags ${tags} -models ${mdls} -residualNeeds -metric ${m} -selection ${s} -KTT ${th} 1>${path}/${collection}_${m}_${s}_${th}.txt 2>${path}/FEATURE_${collection}_${m}_${s}_${th}.txt
			done
		else
			./run.sh SelectiveStemming -collection ${collection} -tags ${tags} -models ${mdls} -residualNeeds -metric ${m} -selection ${s} 1> ${path}/${collection}_${m}_${s}.txt 2>${path}/FEATURE_${collection}_${m}_${s}.txt
		fi

	done

done

#===============================================

collection=MQ09
tags=NoStem_SnowballEng
metrics=(MAP NDCG20 NDCG100)
selectionMethods=(MSTTF MSTDF LSTDF TFOrder DFOrder KendallTauTFOrder KendallTauDFOrder MSTTFBinning MSTDFBinning TFOrderBinning DFOrderBinning KendallTauTFOrderBinning KendallTauDFOrderBinning)
kendallTauThreshold=(0.55 0.65 0.75 0.85 0.95)

path=resultsEightModel/${tags}/residualNeeds/${collection}
mkdir -p ${path}

for m in ${metrics[*]}; do
    if [${m} == NDCG20];then
        mdls=BM25k1.3b0.5_PL2c4.0_LGDc2.0_DirichletLMc500.0_DPH_DFRee_DFIC_DLH13
    elif [${m} == NDCG100];then
        mdls=BM25k1.4b0.5_PL2c5.5_LGDc2.5_DirichletLMc650.0_DPH_DFRee_DFIC_DLH13
    elif [${m} == MAP];then
        mdls=BM25k1.6b0.35_PL2c9.0_LGDc4.0_DirichletLMc900.0_DPH_DFRee_DFIC_DLH13
    fi

	for s in ${selectionMethods[*]}; do
		if [[ ${s} == KendallTau* ]]; then
			for th in ${kendallTauThreshold[*]}; do
				./run.sh SelectiveStemming -collection ${collection}  -tags ${tags} -models ${mdls} -residualNeeds -metric ${m} -selection ${s} -KTT ${th} 1>${path}/${collection}_${m}_${s}_${th}.txt 2>${path}/FEATURE_${collection}_${m}_${s}_${th}.txt
			done
		else
			./run.sh SelectiveStemming -collection ${collection} -tags ${tags} -models ${mdls} -residualNeeds -metric ${m} -selection ${s} 1> ${path}/${collection}_${m}_${s}.txt 2>${path}/FEATURE_${collection}_${m}_${s}.txt
		fi

	done

done

#===============================================
#===============================================

collection=MQ07
tags=NoStem_KStem
metrics=(MAP NDCG20 NDCG100)
selectionMethods=(MSTTF MSTDF LSTDF TFOrder DFOrder KendallTauTFOrder KendallTauDFOrder MSTTFBinning MSTDFBinning TFOrderBinning DFOrderBinning KendallTauTFOrderBinning KendallTauDFOrderBinning)
kendallTauThreshold=(0.55 0.65 0.75 0.85 0.95)

path=resultsEightModel/${tags}/residualNeeds/${collection}
mkdir -p ${path}

for m in ${metrics[*]}; do
    if [${m} == NDCG20];then
        mdls=BM25k1.4b0.4_PL2c6.5_LGDc3.0_DirichletLMc1000.0_DPH_DFRee_DFIC_DLH13
    elif [${m} == NDCG100];then
        mdls=BM25k1.6b0.4_PL2c6.5_LGDc2.0_DirichletLMc900.0_DPH_DFRee_DFIC_DLH13
    elif [${m} == MAP];then
        mdls=BM25k1.3b0.4_PL2c6.5_LGDc2.0_DirichletLMc900.0_DPH_DFRee_DFIC_DLH13
    fi

	for s in ${selectionMethods[*]}; do
		if [[ ${s} == KendallTau* ]]; then
			for th in ${kendallTauThreshold[*]}; do
				./run.sh SelectiveStemming -collection ${collection}  -tags ${tags} -models ${mdls} -residualNeeds -metric ${m} -selection ${s} -KTT ${th} 1>${path}/${collection}_${m}_${s}_${th}.txt 2>${path}/FEATURE_${collection}_${m}_${s}_${th}.txt
			done
		else
			./run.sh SelectiveStemming -collection ${collection} -tags ${tags} -models ${mdls} -residualNeeds -metric ${m} -selection ${s} 1> ${path}/${collection}_${m}_${s}.txt 2>${path}/FEATURE_${collection}_${m}_${s}.txt
		fi

	done

done

#===============================================

collection=MQ08
tags=NoStem_KStem
metrics=(MAP NDCG20 NDCG100)
selectionMethods=(MSTTF MSTDF LSTDF TFOrder DFOrder KendallTauTFOrder KendallTauDFOrder MSTTFBinning MSTDFBinning TFOrderBinning DFOrderBinning KendallTauTFOrderBinning KendallTauDFOrderBinning)
kendallTauThreshold=(0.55 0.65 0.75 0.85 0.95)

path=resultsEightModel/${tags}/residualNeeds/${collection}
mkdir -p ${path}

for m in ${metrics[*]}; do
    if [${m} == NDCG20];then
        mdls=BM25k1.7b0.4_PL2c6.5_LGDc2.0_DirichletLMc1000.0_DPH_DFRee_DFIC_DLH13
    elif [${m} == NDCG100];then
        mdls=BM25k1.8b0.5_PL2c6.5_LGDc1.5_DirichletLMc650.0_DPH_DFRee_DFIC_DLH13
    elif [${m} == MAP];then
        mdls=BM25k1.2b0.5_PL2c5.0_LGDc1.5_DirichletLMc650.0_DPH_DFRee_DFIC_DLH13
    fi

	for s in ${selectionMethods[*]}; do
		if [[ ${s} == KendallTau* ]]; then
			for th in ${kendallTauThreshold[*]}; do
				./run.sh SelectiveStemming -collection ${collection}  -tags ${tags} -models ${mdls} -residualNeeds -metric ${m} -selection ${s} -KTT ${th} 1>${path}/${collection}_${m}_${s}_${th}.txt 2>${path}/FEATURE_${collection}_${m}_${s}_${th}.txt
			done
		else
			./run.sh SelectiveStemming -collection ${collection} -tags ${tags} -models ${mdls} -residualNeeds -metric ${m} -selection ${s} 1> ${path}/${collection}_${m}_${s}.txt 2>${path}/FEATURE_${collection}_${m}_${s}.txt
		fi

	done

done

#===============================================

collection=MQ09
tags=NoStem_KStem
metrics=(MAP NDCG20 NDCG100)
selectionMethods=(MSTTF MSTDF LSTDF TFOrder DFOrder KendallTauTFOrder KendallTauDFOrder MSTTFBinning MSTDFBinning TFOrderBinning DFOrderBinning KendallTauTFOrderBinning KendallTauDFOrderBinning)
kendallTauThreshold=(0.55 0.65 0.75 0.85 0.95)

path=resultsEightModel/${tags}/residualNeeds/${collection}
mkdir -p ${path}

for m in ${metrics[*]}; do
    if [${m} == NDCG20];then
        mdls=BM25k1.5b0.5_PL2c3.5_LGDc1.5_DirichletLMc500.0_DPH_DFRee_DFIC_DLH13
    elif [${m} == NDCG100];then
        mdls=BM25k1.5b0.5_PL2c5.5_LGDc2.5_DirichletLMc650.0_DPH_DFRee_DFIC_DLH13
    elif [${m} == MAP];then
        mdls=BM25k1.4b0.3_PL2c9.0_LGDc3.5_DirichletLMc900.0_DPH_DFRee_DFIC_DLH13
    fi

	for s in ${selectionMethods[*]}; do
		if [[ ${s} == KendallTau* ]]; then
			for th in ${kendallTauThreshold[*]}; do
				./run.sh SelectiveStemming -collection ${collection}  -tags ${tags} -models ${mdls} -residualNeeds -metric ${m} -selection ${s} -KTT ${th} 1>${path}/${collection}_${m}_${s}_${th}.txt 2>${path}/FEATURE_${collection}_${m}_${s}_${th}.txt
			done
		else
			./run.sh SelectiveStemming -collection ${collection} -tags ${tags} -models ${mdls} -residualNeeds -metric ${m} -selection ${s} 1> ${path}/${collection}_${m}_${s}.txt 2>${path}/FEATURE_${collection}_${m}_${s}.txt
		fi

	done

done
