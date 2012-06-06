/*
 * Copyright (C) 2012 Sebastian Schelter <sebastian.schelter [at] tu-berlin.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package de.tuberlin.dima.recsys.ssnmm.ratingprediction;

import com.google.common.base.Preconditions;
import de.tuberlin.dima.recsys.ssnmm.Rating;
import de.tuberlin.dima.recsys.ssnmm.RatingsIterable;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.mahout.cf.taste.impl.common.FullRunningAverage;
import org.apache.mahout.cf.taste.impl.common.RunningAverage;
import org.apache.mahout.common.Pair;
import org.apache.mahout.common.iterator.FileLineIterable;
import org.apache.mahout.common.iterator.sequencefile.PathFilters;
import org.apache.mahout.common.iterator.sequencefile.PathType;
import org.apache.mahout.common.iterator.sequencefile.SequenceFileDirIterable;
import org.apache.mahout.math.Matrix;
import org.apache.mahout.math.SparseRowMatrix;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.VectorWritable;
import org.apache.mahout.math.map.OpenIntDoubleHashMap;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.regex.Pattern;

/**
 * Reads the similarity matrix as well as the item and user biases into memory,
 * computes the prediction error towards held out ratings in a single read through the data
 */
public class Evaluate {
  
  public static void main(String[] args) throws IOException {

    int numUsers = 1823179;
    int numItems = 136736;
    double mu = 3.157255412010664;

    String distributedSimilarityMatrixPath = "/home/ssc/Desktop/yahoo/similarityMatrix/";
    String itemBiasesFilePath = "/home/ssc/Desktop/yahoo/itemBiases.tsv";
    String userBiasesFilePath = "/home/ssc/Desktop/yahoo/userBiases.tsv";
    String trainingSetPath = "/home/ssc/Entwicklung/datasets/yahoo-songs/songs.tsv";
    String holdoutSetPath = "home/ssc/Entwicklung/datasets/yahoo-songs/holdout.tsv";

    Matrix similarities = new SparseRowMatrix(numItems, numItems);

    System.out.println("Reading similarities...");
    int similaritiesRead = 0;
    Configuration conf = new Configuration();
    for (Pair<IntWritable, VectorWritable> pair : new SequenceFileDirIterable<IntWritable, VectorWritable>(
        new Path(distributedSimilarityMatrixPath), PathType.LIST, PathFilters.partFilter(), conf)) {

      int item = pair.getFirst().get();
      Iterator<Vector.Element> elements = pair.getSecond().get().iterateNonZero();
      
      while (elements.hasNext()) {
        Vector.Element elem = elements.next();
        similarities.setQuick(item, elem.index(), elem.get());
        similaritiesRead++;
      }
    }
    System.out.println("Found " + similaritiesRead + " similarities");
    
    Pattern sep = Pattern.compile("\t");

    double[] itemBiases = new double[numItems];
    double[] userBiases = new double[numUsers];

    System.out.println("Reading item biases");
    for (String line : new FileLineIterable(new File(itemBiasesFilePath))) {
      String[] parts = sep.split(line);
      itemBiases[Integer.parseInt(parts[0])] = Double.parseDouble(parts[1]);
    }

    System.out.println("Reading user biases");
    for (String line : new FileLineIterable(new File(userBiasesFilePath))) {
      String[] parts = sep.split(line);
      userBiases[Integer.parseInt(parts[0])] = Double.parseDouble(parts[1]);
    }

    Iterator<Rating> trainRatings = new RatingsIterable(new File(trainingSetPath)).iterator();
    Iterator<Rating> heldOutRatings = new RatingsIterable(new File(holdoutSetPath)).iterator();
    
    int currentUser = 0;
    OpenIntDoubleHashMap prefs = new OpenIntDoubleHashMap();
    
    int usersProcessed = 0;
    RunningAverage rmse = new FullRunningAverage();
    RunningAverage mae = new FullRunningAverage();

    RunningAverage rmseBase = new FullRunningAverage();
    RunningAverage maeBase = new FullRunningAverage();    

    while (trainRatings.hasNext()) {
      Rating rating = trainRatings.next();
      if (rating.user() != currentUser) {

        for (int n = 0; n < 10; n++) {
          Rating heldOutRating = heldOutRatings.next();
          Preconditions.checkState(heldOutRating.user() == currentUser);

          double preference = 0.0;
          double totalSimilarity = 0.0;
          int count = 0;

          Iterator<Vector.Element> similarItems = similarities.viewRow(heldOutRating.item()).iterateNonZero();
          while (similarItems.hasNext()) {
            Vector.Element similarity = similarItems.next();
            int similarItem = similarity.index();
            if (prefs.containsKey(similarItem)) {
              preference += similarity.get() * (prefs.get(similarItem) - (mu + userBiases[currentUser] +
                  itemBiases[similarItem]));
              totalSimilarity += Math.abs(similarity.get());
              count++;

            }
          }

          double baselineEstimate = mu + userBiases[currentUser] + itemBiases[heldOutRating.item()];
          double estimate = baselineEstimate;
          
          if (count > 1) {
            estimate += preference / totalSimilarity;
          }
          
          double baseError = Math.abs(heldOutRating.rating() - baselineEstimate);
          maeBase.addDatum(baseError);
          rmseBase.addDatum(baseError * baseError);

          double error = Math.abs(heldOutRating.rating() - estimate);
          mae.addDatum(error);
          rmse.addDatum(error * error);

        }

        if (++usersProcessed % 10000 == 0) {
          System.out.println(usersProcessed + " users processed, MAE " + mae.getAverage() +
              ", RMSE " + Math.sqrt(rmse.getAverage()) + " | baseline MAE " + maeBase.getAverage() +
              ", baseline RMSE " + Math.sqrt(rmseBase.getAverage()));
        }

        currentUser = rating.user();
        prefs.clear();
        
      } 
      prefs.put(rating.item(), rating.rating());

    }

    System.out.println(usersProcessed + " users processed, MAE " + mae.getAverage() +
        ", RMSE " + Math.sqrt(rmse.getAverage()) + " | baseline MAE " + maeBase.getAverage() +
        ", baseline RMSE " + Math.sqrt(rmseBase.getAverage()));
  }
}
