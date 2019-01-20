#!/usr/bin/env bash

#Searcher for NoStem and Stem common parameters and defaults
collection=MQ09
metric=NDCG20

SnowballParams=BM25k1.3b0.5_PL2c4.0_LGDc2.0_DirichletLMc500.0
KStemParams=BM25k1.5b0.5_PL2c3.5_LGDc1.5_DirichletLMc500.0

echo collection ${collection} metric ${metric} SnowballParam ${SnowballParams} KStemParam ${KStemParams} ...

./run.sh Custom -collection ${collection} -tag NoStem -task search  -metric ${metric} -models ${KStemParams}
./run.sh Custom -collection ${collection} -tag NoStem -task search   -metric ${metric} -models ${SnowballParams}
./run.sh Custom -collection ${collection} -tag KStem -task search   -metric ${metric} -models ${KStemParams}
./run.sh Custom -collection ${collection} -tag SnowballEng -task search  -metric ${metric} -models ${SnowballParams}

echo collection ${collection} metric ${metric} default params ...

./run.sh Custom -collection ${collection} -tag NoStem -task search   -metric ${metric}
./run.sh Custom -collection ${collection} -tag KStem -task search   -metric ${metric}
./run.sh Custom -collection ${collection} -tag SnowballEng -task search  -metric ${metric}

metric=NDCG100

SnowballParams=BM25k1.4b0.5_PL2c5.5_LGDc2.5_DirichletLMc650.0
KStemParams=BM25k1.5b0.5_PL2c5.5_LGDc2.5_DirichletLMc650.0

echo collection ${collection} metric ${metric} SnowballParam ${SnowballParams} KStemParam ${KStemParams} ...

./run.sh Custom -collection ${collection} -tag NoStem -task search  -metric ${metric} -models ${KStemParams}
./run.sh Custom -collection ${collection} -tag NoStem -task search   -metric ${metric} -models ${SnowballParams}
./run.sh Custom -collection ${collection} -tag KStem -task search   -metric ${metric} -models ${KStemParams}
./run.sh Custom -collection ${collection} -tag SnowballEng -task search  -metric ${metric} -models ${SnowballParams}

echo collection ${collection} metric ${metric} default params ...

./run.sh Custom -collection ${collection} -tag NoStem -task search   -metric ${metric}
./run.sh Custom -collection ${collection} -tag KStem -task search   -metric ${metric}
./run.sh Custom -collection ${collection} -tag SnowballEng -task search  -metric ${metric}

metric=MAP

SnowballParams=BM25k1.6b0.35_PL2c9.0_LGDc4.0_DirichletLMc900.0
KStemParams=BM25k1.4b0.3_PL2c9.0_LGDc3.5_DirichletLMc900.0

echo collection ${collection} metric ${metric} SnowballParam ${SnowballParams} KStemParam ${KStemParams} ...

./run.sh Custom -collection ${collection} -tag NoStem -task search  -metric ${metric} -models ${KStemParams}
./run.sh Custom -collection ${collection} -tag NoStem -task search   -metric ${metric} -models ${SnowballParams}
./run.sh Custom -collection ${collection} -tag KStem -task search   -metric ${metric} -models ${KStemParams}
./run.sh Custom -collection ${collection} -tag SnowballEng -task search  -metric ${metric} -models ${SnowballParams}

echo collection ${collection} metric ${metric} default params ...

./run.sh Custom -collection ${collection} -tag NoStem -task search   -metric ${metric}
./run.sh Custom -collection ${collection} -tag KStem -task search   -metric ${metric}
./run.sh Custom -collection ${collection} -tag SnowballEng -task search  -metric ${metric}

#----------------------------------------------

collection=MQ08
metric=NDCG20

SnowballParams=BM25k1.7b0.4_PL2c5.0_LGDc2.0_DirichletLMc900.0
KStemParams=BM25k1.7b0.4_PL2c6.5_LGDc2.0_DirichletLMc1000.0

echo collection ${collection} metric ${metric} SnowballParam ${SnowballParams} KStemParam ${KStemParams} ...

./run.sh Custom -collection ${collection} -tag NoStem -task search  -metric ${metric} -models ${KStemParams}
./run.sh Custom -collection ${collection} -tag NoStem -task search   -metric ${metric} -models ${SnowballParams}
./run.sh Custom -collection ${collection} -tag KStem -task search   -metric ${metric} -models ${KStemParams}
./run.sh Custom -collection ${collection} -tag SnowballEng -task search  -metric ${metric} -models ${SnowballParams}

echo collection ${collection} metric ${metric} default params ...

./run.sh Custom -collection ${collection} -tag NoStem -task search   -metric ${metric}
./run.sh Custom -collection ${collection} -tag KStem -task search   -metric ${metric}
./run.sh Custom -collection ${collection} -tag SnowballEng -task search  -metric ${metric}

metric=NDCG100

SnowballParams=BM25k1.7b0.45_PL2c5.0_LGDc1.5_DirichletLMc800.0
KStemParams=BM25k1.8b0.5_PL2c6.5_LGDc1.5_DirichletLMc650.0

echo collection ${collection} metric ${metric} SnowballParam ${SnowballParams} KStemParam ${KStemParams} ...

./run.sh Custom -collection ${collection} -tag NoStem -task search  -metric ${metric} -models ${KStemParams}
./run.sh Custom -collection ${collection} -tag NoStem -task search   -metric ${metric} -models ${SnowballParams}
./run.sh Custom -collection ${collection} -tag KStem -task search   -metric ${metric} -models ${KStemParams}
./run.sh Custom -collection ${collection} -tag SnowballEng -task search  -metric ${metric} -models ${SnowballParams}

echo collection ${collection} metric ${metric} default params ...

./run.sh Custom -collection ${collection} -tag NoStem -task search   -metric ${metric}
./run.sh Custom -collection ${collection} -tag KStem -task search   -metric ${metric}
./run.sh Custom -collection ${collection} -tag SnowballEng -task search  -metric ${metric}

metric=MAP

echo collection ${collection} metric ${metric} SnowballParam ${SnowballParams} KStemParam ${KStemParams} ...

SnowballParams=BM25k1.1b0.45_PL2c5.0_LGDc1.5_DirichletLMc650.0
KStemParams=BM25k1.2b0.5_PL2c5.0_LGDc1.5_DirichletLMc650.0

./run.sh Custom -collection ${collection} -tag NoStem -task search  -metric ${metric} -models ${KStemParams}
./run.sh Custom -collection ${collection} -tag NoStem -task search   -metric ${metric} -models ${SnowballParams}
./run.sh Custom -collection ${collection} -tag KStem -task search   -metric ${metric} -models ${KStemParams}
./run.sh Custom -collection ${collection} -tag SnowballEng -task search  -metric ${metric} -models ${SnowballParams}

echo collection ${collection} metric ${metric} default params ...

./run.sh Custom -collection ${collection} -tag NoStem -task search   -metric ${metric}
./run.sh Custom -collection ${collection} -tag KStem -task search   -metric ${metric}
./run.sh Custom -collection ${collection} -tag SnowballEng -task search  -metric ${metric}

#----------------------------------------------

collection=MQ07
metric=NDCG20

SnowballParams=BM25k1.2b0.4_PL2c6.5_LGDc2.5_DirichletLMc1000.0
KStemParams=BM25k1.4b0.4_PL2c6.5_LGDc3.0_DirichletLMc1000.0

echo collection ${collection} metric ${metric} SnowballParam ${SnowballParams} KStemParam ${KStemParams} ...

./run.sh Custom -collection ${collection} -tag NoStem -task search  -metric ${metric} -models ${KStemParams}
./run.sh Custom -collection ${collection} -tag NoStem -task search   -metric ${metric} -models ${SnowballParams}
./run.sh Custom -collection ${collection} -tag KStem -task search   -metric ${metric} -models ${KStemParams}
./run.sh Custom -collection ${collection} -tag SnowballEng -task search  -metric ${metric} -models ${SnowballParams}

echo collection ${collection} metric ${metric} default params ...

./run.sh Custom -collection ${collection} -tag NoStem -task search   -metric ${metric}
./run.sh Custom -collection ${collection} -tag KStem -task search   -metric ${metric}
./run.sh Custom -collection ${collection} -tag SnowballEng -task search  -metric ${metric}

metric=NDCG100

SnowballParams=BM25k1.5b0.4_PL2c6.5_LGDc2.0_DirichletLMc900.0
KStemParams=BM25k1.6b0.4_PL2c6.5_LGDc2.0_DirichletLMc900.0

echo collection ${collection} metric ${metric} SnowballParam ${SnowballParams} KStemParam ${KStemParams} ...

./run.sh Custom -collection ${collection} -tag NoStem -task search  -metric ${metric} -models ${KStemParams}
./run.sh Custom -collection ${collection} -tag NoStem -task search   -metric ${metric} -models ${SnowballParams}
./run.sh Custom -collection ${collection} -tag KStem -task search   -metric ${metric} -models ${KStemParams}
./run.sh Custom -collection ${collection} -tag SnowballEng -task search  -metric ${metric} -models ${SnowballParams}

echo collection ${collection} metric ${metric} default params ...

./run.sh Custom -collection ${collection} -tag NoStem -task search   -metric ${metric}
./run.sh Custom -collection ${collection} -tag KStem -task search   -metric ${metric}
./run.sh Custom -collection ${collection} -tag SnowballEng -task search  -metric ${metric}

metric=MAP

SnowballParams=BM25k1.2b0.4_PL2c6.5_LGDc2.0_DirichletLMc900.0
KStemParams=BM25k1.3b0.4_PL2c6.5_LGDc2.0_DirichletLMc900.0

echo collection ${collection} metric ${metric} SnowballParam ${SnowballParams} KStemParam ${KStemParams} ...

./run.sh Custom -collection ${collection} -tag NoStem -task search  -metric ${metric} -models ${KStemParams}
./run.sh Custom -collection ${collection} -tag NoStem -task search   -metric ${metric} -models ${SnowballParams}
./run.sh Custom -collection ${collection} -tag KStem -task search   -metric ${metric} -models ${KStemParams}
./run.sh Custom -collection ${collection} -tag SnowballEng -task search  -metric ${metric} -models ${SnowballParams}

echo collection ${collection} metric ${metric} default params ...

./run.sh Custom -collection ${collection} -tag NoStem -task search   -metric ${metric}
./run.sh Custom -collection ${collection} -tag KStem -task search   -metric ${metric}
./run.sh Custom -collection ${collection} -tag SnowballEng -task search  -metric ${metric}

