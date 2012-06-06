Datasets:
---------

Movielens1M (1M interactions)

http://www.grouplens.org/node/73

Flixster (8M interactions)

http://www.cs.sfu.ca/~sja25/personal/datasets/

Yahoo! Music (700M interactions)

http://webscope.sandbox.yahoo.com/catalog.php?datatype=r


Datasets must be preprocessed to contain a single file where each line has the format:

user[TAB]item[TAB]rating


Local experiments with interaction-cut:
--------------------------------------

Use de.tuberlin.dima.recsys.ssnmm.interactioncut.Evaluate to evaluate the sensitivity of the prediction quality
to different interaction-cuts in small datasets.


Experiments on Hadoop:
----------------------

Use de.tuberlin.dima.recsys.ssnmm.ratingprediction.AverageRating to compute the average rating in the dataset.

Use de.tuberlin.dima.recsys.ssnmm.ratingprediction.UserItemBaseline to estimate user and item biases.

Run the similarity computation in parallel on your Hadoop cluster using Apache Mahout:

hadoop jar mahout-core-0.6-job.jar org.apache.mahout.cf.taste.hadoop.similarity.item.ItemSimilarityJob \
  --input hdfs:///path/to/trainingfiles --similarityClassname SIMILARITY_PEARSON_CORRELATION \
  --threshold 0.01 --maxSimilaritiesPerItem 50 --maxPrefsPerUser 600 --output hdfs:///path/to/output \
  --tempDir hdfs:///path/to/temp

hadoop fs -copyToLocal hdfs:///path/to/temp/similarityMatrix/part* /path/on/local/disk

Use de.tuberlin.dima.recsys.ssnmm.ratingprediction.Evaluate to evaluate the prediction quality of the computed
similarities.