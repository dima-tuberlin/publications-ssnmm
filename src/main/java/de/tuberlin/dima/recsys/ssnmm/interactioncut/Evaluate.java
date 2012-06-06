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

package de.tuberlin.dima.recsys.ssnmm.interactioncut;

import com.google.common.collect.Lists;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.eval.RecommenderBuilder;
import org.apache.mahout.cf.taste.eval.RecommenderEvaluator;
import org.apache.mahout.cf.taste.impl.eval.AverageAbsoluteDifferenceRecommenderEvaluator;
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;
import org.apache.mahout.cf.taste.impl.similarity.CachingItemSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.TanimotoCoefficientSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.recommender.Recommender;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Compute the prediction error achieved by several 'interaction-cuts'
 */
public class Evaluate {
  
  public static void main(String[] args) throws Exception {

    DataModel interactions = new FileDataModel(new File("/home/ssc/Entwicklung/datasets/movielens1M/movielens.csv"));
    //DataModel interactions = new FileDataModel(new File("/home/ssc/Entwicklung/datasets/flixster/ratings.txt"));


    int k = 80;
    double lambda2 = 10;
    double lambda3 = 25;
    double trainingPercentage = 0.8;
    int numRuns = 10;

    int minP = 800;
    int maxP = 800;
    int pStepSize = 250;

    runEvaluation(interactions, k, lambda2, lambda3, trainingPercentage, numRuns, minP, maxP, pStepSize);
  }

  static void runEvaluation(DataModel interactions, int k, double lambda2, double lambda3, double trainingPercentage,
      int numRuns, int minP, int maxP, int pStepSize) throws IOException, TasteException {

    RecommenderEvaluator maeEvaluator = new AverageAbsoluteDifferenceRecommenderEvaluator();
    List<Errors> errors = Lists.newArrayList();

    for (int maxPrefsPerUser = minP; maxPrefsPerUser <= maxP; maxPrefsPerUser += pStepSize) {
      Errors error = new Errors(maxPrefsPerUser);

      for (int n = 0; n < numRuns; n++) {
        double maeSampled = maeEvaluator.evaluate(new BiasedRecommenderBuilder(lambda2, lambda3, k),
            new InteractionCutDataModelBuilder(maxPrefsPerUser), interactions, trainingPercentage,
            1 - trainingPercentage);
        error.record(0, maeSampled);
      }
      errors.add(error);
    }
    for (Errors res : errors) {
      System.out.println(res);
    }
  }

  static class BiasedRecommenderBuilder implements RecommenderBuilder {
    
    private final int k;
    private final double lambda2;
    private final double lambda3;

    BiasedRecommenderBuilder(double lambda2, double lambda3, int k) {
      this.lambda2 = lambda2;
      this.lambda3 = lambda3;
      this.k = k;
    }

    @Override
    public Recommender buildRecommender(DataModel dataModel) throws TasteException {
      return new BiasedItemBasedRecommender(dataModel, new CachingItemSimilarity(
          new TanimotoCoefficientSimilarity(dataModel), 10000000), k, lambda2, lambda3);
    }
  }  

}
