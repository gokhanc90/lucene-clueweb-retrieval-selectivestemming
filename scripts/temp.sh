#!/usr/bin/env bash

collection=MQ07
tags=NoStem_SnowballEng
metrics=(NDCG20)
selectionMethods=(DFOrderBinning)
bins=(50 100 150 250 500 750 1000 1250 1500 2000 5000 7500 10000)

path=resultsDFBinningParameter/${tags}/all/${collection}
mkdir -p ${path}

for m in ${metrics[*]}; do
    if [ ${m} == NDCG20 ];then
        mdls=BM25k1.2b0.4_PL2c6.5_LGDc2.5_DirichletLMc1000.0_DPH_DFRee_DFIC_DLH13
    elif [ ${m} == NDCG100 ];then
        mdls=BM25k1.5b0.4_PL2c6.5_LGDc2.0_DirichletLMc900.0_DPH_DFRee_DFIC_DLH13
    elif [ ${m} == MAP ];then
        mdls=BM25k1.2b0.4_PL2c6.5_LGDc2.0_DirichletLMc900.0_DPH_DFRee_DFIC_DLH13
    fi

	for s in ${selectionMethods[*]}; do
		
			for th in ${bins[*]}; do
				./run.sh SelectiveStemming -collection ${collection}  -tags ${tags} -models ${mdls}  -metric ${m} -selection ${s} -numBin ${th} 1>${path}/${collection}_${m}_${s}_${th}.txt 2>${path}/FEATURE_${collection}_${m}_${s}_${th}.txt
			done
	done
	
done

#===============================================

collection=MQ08

path=resultsDFBinningParameter/${tags}/all/${collection}
mkdir -p ${path}

for m in ${metrics[*]}; do
    if [ ${m} == NDCG20 ];then
        mdls=BM25k1.7b0.4_PL2c5.0_LGDc2.0_DirichletLMc900.0_DPH_DFRee_DFIC_DLH13
    elif [ ${m} == NDCG100 ];then
        mdls=BM25k1.7b0.45_PL2c5.0_LGDc1.5_DirichletLMc800.0_DPH_DFRee_DFIC_DLH13
    elif [ ${m} == MAP ];then
        mdls=BM25k1.1b0.45_PL2c5.0_LGDc1.5_DirichletLMc650.0_DPH_DFRee_DFIC_DLH13
    fi

	for s in ${selectionMethods[*]}; do
			for th in ${bins[*]}; do
				./run.sh SelectiveStemming -collection ${collection}  -tags ${tags} -models ${mdls}  -metric ${m} -selection ${s} -numBin ${th} 1>${path}/${collection}_${m}_${s}_${th}.txt 2>${path}/FEATURE_${collection}_${m}_${s}_${th}.txt
			done
		
	done

done

#===============================================

collection=MQ09

path=resultsDFBinningParameter/${tags}/all/${collection}
mkdir -p ${path}

for m in ${metrics[*]}; do
    if [ ${m} == NDCG20 ];then
        mdls=BM25k1.3b0.5_PL2c4.0_LGDc2.0_DirichletLMc500.0_DPH_DFRee_DFIC_DLH13
    elif [ ${m} == NDCG100 ];then
        mdls=BM25k1.4b0.5_PL2c5.5_LGDc2.5_DirichletLMc650.0_DPH_DFRee_DFIC_DLH13
    elif [ ${m} == MAP ];then
        mdls=BM25k1.6b0.35_PL2c9.0_LGDc4.0_DirichletLMc900.0_DPH_DFRee_DFIC_DLH13
    fi

	for s in ${selectionMethods[*]}; do
			for th in ${bins[*]}; do
				./run.sh SelectiveStemming -collection ${collection}  -tags ${tags} -models ${mdls}  -metric ${m} -selection ${s} -numBin ${th} 1>${path}/${collection}_${m}_${s}_${th}.txt 2>${path}/FEATURE_${collection}_${m}_${s}_${th}.txt
			done
		
	done

done

#===============================================

collection=CW09B

path=resultsDFBinningParameter/${tags}/all/${collection}
mkdir -p ${path}

for m in ${metrics[*]}; do
    if [ ${m} == NDCG20 ];then
        mdls=BM25k2.0b0.25_PL2c17.0_LGDc2.0_DirichletLMc650.0_DPH_DFRee_DFIC_DLH13
    elif [ ${m} == NDCG100 ];then
        mdls=
    elif [ ${m} == MAP ];then
        mdls=
    fi

	for s in ${selectionMethods[*]}; do
			for th in ${bins[*]}; do
				./run.sh SelectiveStemming -collection ${collection}  -tags ${tags} -models ${mdls}  -metric ${m} -selection ${s} -numBin ${th} 1>${path}/${collection}_${m}_${s}_${th}.txt 2>${path}/FEATURE_${collection}_${m}_${s}_${th}.txt
			done
		
	done

done
#===============================================

collection=CW12B

path=resultsDFBinningParameter/${tags}/all/${collection}
mkdir -p ${path}

for m in ${metrics[*]}; do
    if [ ${m} == NDCG20 ];then
        mdls=BM25k3.0b0.2_PL2c0.65_LGDc5.0_DirichletLMc2000.0_DPH_DFRee_DFIC_DLH13
    elif [ ${m} == NDCG100 ];then
        mdls=
    elif [ ${m} == MAP ];then
        mdls=
    fi

	for s in ${selectionMethods[*]}; do
			for th in ${bins[*]}; do
				./run.sh SelectiveStemming -collection ${collection}  -tags ${tags} -models ${mdls}  -metric ${m} -selection ${s} -numBin ${th} 1>${path}/${collection}_${m}_${s}_${th}.txt 2>${path}/FEATURE_${collection}_${m}_${s}_${th}.txt
			done
		
	done

done

#===============================================

collection=NTCIR

path=resultsDFBinningParameter/${tags}/all/${collection}
mkdir -p ${path}

for m in ${metrics[*]}; do
    if [ ${m} == NDCG20 ];then
        mdls=BM25k1.2b0.55_PL2c4.0_LGDc2.5_DirichletLMc650.0_DPH_DFRee_DFIC_DLH13
    elif [ ${m} == NDCG100 ];then
        mdls=
    elif [ ${m} == MAP ];then
        mdls=
    fi

	for s in ${selectionMethods[*]}; do
			for th in ${bins[*]}; do
				./run.sh SelectiveStemming -collection ${collection}  -tags ${tags} -models ${mdls}  -metric ${m} -selection ${s} -numBin ${th} 1>${path}/${collection}_${m}_${s}_${th}.txt 2>${path}/FEATURE_${collection}_${m}_${s}_${th}.txt
			done
		
	done

done

#===============================================

collection=GOV2

path=resultsDFBinningParameter/${tags}/all/${collection}
mkdir -p ${path}

for m in ${metrics[*]}; do
    if [ ${m} == NDCG20 ];then
        mdls=BM25k1.2b0.3_PL2c11.0_LGDc7.5_DirichletLMc1500.0_DPH_DFRee_DFIC_DLH13
    elif [ ${m} == NDCG100 ];then
        mdls=
    elif [ ${m} == MAP ];then
        mdls=
    fi

	for s in ${selectionMethods[*]}; do
			for th in ${bins[*]}; do
				./run.sh SelectiveStemming -collection ${collection}  -tags ${tags} -models ${mdls}  -metric ${m} -selection ${s} -numBin ${th} 1>${path}/${collection}_${m}_${s}_${th}.txt 2>${path}/FEATURE_${collection}_${m}_${s}_${th}.txt
			done
		
	done

done

#===============================================
#===============================================

collection=MQ07

path=resultsDFBinningParameter/${tags}/all/${collection}
mkdir -p ${path}

for m in ${metrics[*]}; do
    if [ ${m} == NDCG20 ];then
        mdls=BM25k1.4b0.4_PL2c6.5_LGDc3.0_DirichletLMc1000.0_DPH_DFRee_DFIC_DLH13
    elif [ ${m} == NDCG100 ];then
        mdls=BM25k1.6b0.4_PL2c6.5_LGDc2.0_DirichletLMc900.0_DPH_DFRee_DFIC_DLH13
    elif [ ${m} == MAP ];then
        mdls=BM25k1.3b0.4_PL2c6.5_LGDc2.0_DirichletLMc900.0_DPH_DFRee_DFIC_DLH13
    fi

	for s in ${selectionMethods[*]}; do
			for th in ${bins[*]}; do
				./run.sh SelectiveStemming -collection ${collection}  -tags ${tags} -models ${mdls}  -metric ${m} -selection ${s} -numBin ${th} 1>${path}/${collection}_${m}_${s}_${th}.txt 2>${path}/FEATURE_${collection}_${m}_${s}_${th}.txt
			done


	done

done

#===============================================

collection=MQ08

path=resultsDFBinningParameter/${tags}/all/${collection}
mkdir -p ${path}

for m in ${metrics[*]}; do
    if [ ${m} == NDCG20 ];then
        mdls=BM25k1.7b0.4_PL2c6.5_LGDc2.0_DirichletLMc1000.0_DPH_DFRee_DFIC_DLH13
    elif [ ${m} == NDCG100 ];then
        mdls=BM25k1.8b0.5_PL2c6.5_LGDc1.5_DirichletLMc650.0_DPH_DFRee_DFIC_DLH13
    elif [ ${m} == MAP ];then
        mdls=BM25k1.2b0.5_PL2c5.0_LGDc1.5_DirichletLMc650.0_DPH_DFRee_DFIC_DLH13
    fi

	for s in ${selectionMethods[*]}; do
			for th in ${bins[*]}; do
				./run.sh SelectiveStemming -collection ${collection}  -tags ${tags} -models ${mdls}  -metric ${m} -selection ${s} -numBin ${th} 1>${path}/${collection}_${m}_${s}_${th}.txt 2>${path}/FEATURE_${collection}_${m}_${s}_${th}.txt
			done
	
	done

done

#===============================================

collection=MQ09

path=resultsDFBinningParameter/${tags}/all/${collection}
mkdir -p ${path}

for m in ${metrics[*]}; do
    if [ ${m} == NDCG20 ];then
        mdls=BM25k1.5b0.5_PL2c3.5_LGDc1.5_DirichletLMc500.0_DPH_DFRee_DFIC_DLH13
    elif [ ${m} == NDCG100 ];then
        mdls=BM25k1.5b0.5_PL2c5.5_LGDc2.5_DirichletLMc650.0_DPH_DFRee_DFIC_DLH13
    elif [ ${m} == MAP ];then
        mdls=BM25k1.4b0.3_PL2c9.0_LGDc3.5_DirichletLMc900.0_DPH_DFRee_DFIC_DLH13
    fi

	for s in ${selectionMethods[*]}; do
			for th in ${bins[*]}; do
				./run.sh SelectiveStemming -collection ${collection}  -tags ${tags} -models ${mdls}  -metric ${m} -selection ${s} -numBin ${th} 1>${path}/${collection}_${m}_${s}_${th}.txt 2>${path}/FEATURE_${collection}_${m}_${s}_${th}.txt
			done
	
	done

done

#===============================================

collection=CW09B

path=resultsDFBinningParameter/${tags}/all/${collection}
mkdir -p ${path}

for m in ${metrics[*]}; do
    if [ ${m} == NDCG20 ];then
        mdls=BM25k2.3b0.25_PL2c15.0_LGDc2.0_DirichletLMc900.0_DPH_DFRee_DFIC_DLH13
    elif [ ${m} == NDCG100 ];then
        mdls=
    elif [ ${m} == MAP ];then
        mdls=
    fi

	for s in ${selectionMethods[*]}; do
			for th in ${bins[*]}; do
				./run.sh SelectiveStemming -collection ${collection}  -tags ${tags} -models ${mdls}  -metric ${m} -selection ${s} -numBin ${th} 1>${path}/${collection}_${m}_${s}_${th}.txt 2>${path}/FEATURE_${collection}_${m}_${s}_${th}.txt
			done
		
	done

done
#===============================================

collection=CW12B

path=resultsDFBinningParameter/${tags}/all/${collection}
mkdir -p ${path}

for m in ${metrics[*]}; do
    if [ ${m} == NDCG20 ];then
        mdls=BM25k3.0b0.2_PL2c8.0_LGDc5.0_DirichletLMc2000.0_DPH_DFRee_DFIC_DLH13
    elif [ ${m} == NDCG100 ];then
        mdls=
    elif [ ${m} == MAP ];then
        mdls=
    fi

	for s in ${selectionMethods[*]}; do
			for th in ${bins[*]}; do
				./run.sh SelectiveStemming -collection ${collection}  -tags ${tags} -models ${mdls}  -metric ${m} -selection ${s} -numBin ${th} 1>${path}/${collection}_${m}_${s}_${th}.txt 2>${path}/FEATURE_${collection}_${m}_${s}_${th}.txt
			done
		
	done

done

#===============================================

collection=NTCIR

path=resultsDFBinningParameter/${tags}/all/${collection}
mkdir -p ${path}

for m in ${metrics[*]}; do
    if [ ${m} == NDCG20 ];then
        mdls=BM25k1.8b0.5_PL2c5.0_LGDc2.5_DirichletLMc650.0_DPH_DFRee_DFIC_DLH13
    elif [ ${m} == NDCG100 ];then
        mdls=
    elif [ ${m} == MAP ];then
        mdls=
    fi

	for s in ${selectionMethods[*]}; do
			for th in ${bins[*]}; do
				./run.sh SelectiveStemming -collection ${collection}  -tags ${tags} -models ${mdls}  -metric ${m} -selection ${s} -numBin ${th} 1>${path}/${collection}_${m}_${s}_${th}.txt 2>${path}/FEATURE_${collection}_${m}_${s}_${th}.txt
			done
		
	done

done

#===============================================

collection=GOV2

path=resultsDFBinningParameter/${tags}/all/${collection}
mkdir -p ${path}

for m in ${metrics[*]}; do
    if [ ${m} == NDCG20 ];then
        mdls=BM25k1.2b0.25_PL2c11.0_LGDc9.0_DirichletLMc1750.0_DPH_DFRee_DFIC_DLH13
    elif [ ${m} == NDCG100 ];then
        mdls=
    elif [ ${m} == MAP ];then
        mdls=
    fi

	for s in ${selectionMethods[*]}; do
			for th in ${bins[*]}; do
				./run.sh SelectiveStemming -collection ${collection}  -tags ${tags} -models ${mdls}  -metric ${m} -selection ${s} -numBin ${th} 1>${path}/${collection}_${m}_${s}_${th}.txt 2>${path}/FEATURE_${collection}_${m}_${s}_${th}.txt
			done
		
	done

done
