# Retrieval experiments in which hyper parameters of models (BM25,PL2,LGD,DLM) are set to default (Terrier's) or custom provided.

Useful when you need to bypass training phase of hyper parameters. Instead supply manually.

# Indexer

./run.sh Indexer -collection GOV2 -tag ASCII
./run.sh Indexer -collection GOV2 -tag ICU

./run.sh Indexer -collection CW09B -tag ASCII
./run.sh Indexer -collection CW09B -tag ICU

# Optimize

./run.sh Optimize -collection GOV2
./run.sh Optimize -collection CW09B


# Searcher

Terrer's defaults:
./run.sh Custom -collection GOV2 -task search
./run.sh Custom -collection MQ07 -task search
./run.sh Custom -collection MQ08 -task search
./run.sh Custom -collection MQ09 -task search

Average of trained parameters
./run.sh Custom -collection MQ08 -task search -models DirichletLMc1000.0_LGDc2.0_PL2c5.0_BM25k1.4b0.4
./run.sh Custom -collection MQ08 -task search -models DirichletLMc800.0_LGDc1.0_PL2c7.5_BM25k1.4b0.4
./run.sh Custom -collection MQ08 -task search -models DirichletLMc800.0_LGDc1.0_PL2c5.0_BM25k1.4b0.5

# Evaluate

./run.sh Cross -collection GOV2 -tags ICU_ASCII -baseline ICU -metric NDCG20 > tt_ndcg20.txt
./run.sh Cross -collection MQ07 -tags ICU_ASCII -baseline ICU -metric NDCG20 > mq7_ndcg20.txt
./run.sh Cross -collection MQ08 -tags ICU_ASCII -baseline ICU -metric NDCG20 > mq8_ndcg20.txt
./run.sh Cross -collection MQ09 -tags ICU_ASCII -baseline ICU -metric NDCG20 > mq9_ndcg20.txt


./run.sh Cross -collection GOV2 -tags ICU_ASCII -baseline ICU -metric NDCG100 > tt_ndcg100.txt
./run.sh Cross -collection MQ07 -tags ICU_ASCII -baseline ICU -metric NDCG100 > mq7_ndcg100.txt
./run.sh Cross -collection MQ08 -tags ICU_ASCII -baseline ICU -metric NDCG100 > mq8_ndcg100.txt
./run.sh Cross -collection MQ09 -tags ICU_ASCII -baseline ICU -metric NDCG100 > mq9_ndcg100.txt


./run.sh Cross -collection GOV2 -tags ICU_ASCII -baseline ICU -metric MAP > tt_map.txt
./run.sh Cross -collection MQ07 -tags ICU_ASCII -baseline ICU -metric MAP > mq7_map.txt
./run.sh Cross -collection MQ08 -tags ICU_ASCII -baseline ICU -metric MAP > mq8_map.txt
./run.sh Cross -collection MQ09 -tags ICU_ASCII -baseline ICU -metric MAP > mq9_map.txt

./run.sh Cross -collection MQ08 -tags ICU_Latin_ASCII -baseline ICU -metric NDCG20 -models DirichletLMc1000.0_LGDc2.0_PL2c5.0_BM25k1.4b0.4 > mq8_ndcg20_train.txt
./run.sh Cross -collection MQ08 -tags ICU_Latin_ASCII -baseline ICU -metric NDCG100 -models DirichletLMc800.0_LGDc1.0_PL2c7.5_BM25k1.4b0.4 > mq8_ndcg100_train.txt
./run.sh Cross -collection MQ08 -tags ICU_Latin_ASCII -baseline ICU -metric MAP -models DirichletLMc800.0_LGDc1.0_PL2c5.0_BM25k1.4b0.5 > mq8_map_train.txt


./run.sh Custom -collection CW09A -task search
./run.sh Cross -collection CW09A -tags ICU_ASCII -baseline ICU -metric MAP > cw09a_map.txt
./run.sh Cross -collection CW09A -tags ICU_ASCII -baseline ICU -metric NDCG20 > cw09a_ndcg20.txt
./run.sh Cross -collection CW09A -tags ICU_ASCII -baseline ICU -metric ERR20 > cw09a_err20.txt
./run.sh Cross -collection CW09A -tags ICU_ASCII -baseline ICU -metric NDCG100 > cw09a_ndcg100.txt
