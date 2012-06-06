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

import com.google.common.base.Charsets;
import com.google.common.io.Closeables;
import com.google.common.io.Files;
import de.tuberlin.dima.recsys.ssnmm.Rating;
import de.tuberlin.dima.recsys.ssnmm.RatingsIterable;
import org.apache.mahout.cf.taste.impl.common.FullRunningAverage;
import org.apache.mahout.cf.taste.impl.common.RunningAverage;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;


/**
 * Java port of the "UserItemBaseline" rating predictor from "mymedialite" https://github.com/zenogantner/MyMediaLite/
 */
public class UserItemBaseline {

  public static void main(String[] args) throws IOException {
    
    File trainingFile = new File("/home/ssc/Entwicklung/datasets/yahoo-songs/songs.tsv");
    File testFile = new File("/home/ssc/Entwicklung/datasets/yahoo-songs/holdout.tsv"); 
    File outputDir = new File("/home/ssc/Desktop/yahoo/");

    int numUsers = 1823179;
    int numItems = 136736;
    double mu = 3.157255412010664;

    int numIterations = 3;
    
    UserItemBaseline baseline = new UserItemBaseline(trainingFile, testFile, 0.5, 0, numUsers, numItems, mu);
                                                                                                             
    for (int n = 0; n < numIterations; n++) {
      baseline.train();
    }

    baseline.test();

    baseline.persistBiases(outputDir);
  }

  private double[] userBiases;
  private double[] itemBiases;

  private double globalAverage;

  private final File ratings;
  private final File tests;

  private final double regI;
  private final double regU;

  public UserItemBaseline(File ratings, File tests, double regU, double regI, int numUsers, int numItems, double mu) {
    this.ratings = ratings;
    this.tests = tests;
    this.regU = regU;
    this.regI = regI;

    globalAverage = mu;
    
    userBiases = new double[numUsers];
    Arrays.fill(userBiases, 0);
    itemBiases = new double[numItems];
    Arrays.fill(itemBiases, 0);

  }

  void test() throws IOException {

    RunningAverage rmse = new FullRunningAverage();
    RunningAverage mae = new FullRunningAverage();

    System.out.println("Calculating predictions");
    for (Rating rating : new RatingsIterable(tests)) {

      double error = Math.abs(rating.rating() - baselineEstimate(rating.user(), rating.item()));

      mae.addDatum(error);
      rmse.addDatum(error * error);
    }

    System.out.println("MAE " + mae.getAverage() + ", RMSE: " + Math.sqrt(rmse.getAverage()));
  }

  double baselineEstimate(int user, int item) {
    return globalAverage + userBiases[user] + itemBiases[item];
  }

  void train() throws IOException {
      optimizeItemBiases();
      optimizeUserBiases();
  }

  void optimizeItemBiases() throws IOException {
    
    System.out.println("Optimizing item biases...");
    
    int[] itemRatingsCount = new int[itemBiases.length];
    Arrays.fill(itemRatingsCount, 0);

    int ratingsProcessed = 0;
    for (Rating rating : new RatingsIterable(ratings)) {

      itemBiases[rating.item()] += rating.rating() - globalAverage - userBiases[rating.user()];
      itemRatingsCount[rating.item()]++;

      if (++ratingsProcessed % 10000000 == 0) {
        System.out.println((ratingsProcessed / 1000000) + "M ratings processed");
      }
    }
    
    for (int item = 0; item < itemBiases.length; item++) {
      if (itemRatingsCount[item] != 0) {
        itemBiases[item] /= regI + itemRatingsCount[item];
      }
    }

  }

  void optimizeUserBiases() throws IOException {

    System.out.println("Optimizing user biases...");

    int[] userRatingsCount = new int[userBiases.length];
    Arrays.fill(userRatingsCount, 0);

    int ratingsProcessed = 0;
    for (Rating rating : new RatingsIterable(ratings)) {

      userBiases[rating.user()] += rating.rating() - globalAverage - itemBiases[rating.item()];
      userRatingsCount[rating.user()]++;

      if (++ratingsProcessed % 10000000 == 0) {
        System.out.println((ratingsProcessed / 1000000) + "M ratings processed");
      }
    }

    for (int user = 0; user < userBiases.length; user++) {
      if (userRatingsCount[user] != 0) {
        userBiases[user] /= regU + userRatingsCount[user];
      }
    }
  }
  
  void persistBiases(File dir) throws IOException {
    persist(new File(dir, "userBiases.tsv"), userBiases);
    persist(new File(dir, "itemBiases.tsv"), itemBiases);
  }

  private void persist(File file, double[] biases) throws IOException {
    BufferedWriter writer = null;
    try {
      writer = Files.newWriter(file, Charsets.UTF_8);
      for (int index = 0; index < biases.length; index++) {
        writer.append(String.valueOf(index));
        writer.append("\t");
        writer.append(String.valueOf(biases[index]));
        writer.append("\n");
      }
      
    } finally {
      Closeables.closeQuietly(writer);
    }
  }

}
