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

import de.tuberlin.dima.recsys.ssnmm.Utils;
import org.apache.mahout.cf.taste.impl.common.FullRunningAverage;
import org.apache.mahout.cf.taste.impl.common.RunningAverage;

import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.Pattern;

/**
 * Compute the average rating from the trainingset in a streaming fashion
 */
public class AverageRating {
  
  public static void main(String[] args) {

    File dir = new File("/home/ssc/Entwicklung/datasets/yahoo-songs/");

    File[] trainingFiles = dir.listFiles(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return name.startsWith("train_");
      }
    });

    Pattern sep = Pattern.compile("\t");

    RunningAverage avg = new FullRunningAverage();

    int ratingsProcessed = 0;
    for (File trainingFile : trainingFiles) {
      for (String line : Utils.readLines(trainingFile)) {
        int rating = Integer.parseInt(sep.split(line)[2]);
        avg.addDatum(rating);
        if (++ratingsProcessed % 10000000 == 0) {
          System.out.println(ratingsProcessed + " ratings processed");
        }
      }
    }
    System.out.println("average rating " + avg.getAverage());

  }
}
