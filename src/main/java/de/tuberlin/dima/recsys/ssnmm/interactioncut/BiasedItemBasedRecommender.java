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

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.common.FullRunningAverage;
import org.apache.mahout.cf.taste.impl.common.LongPrimitiveIterator;
import org.apache.mahout.cf.taste.impl.common.RunningAverage;
import org.apache.mahout.cf.taste.impl.recommender.GenericItemBasedRecommender;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.model.Preference;
import org.apache.mahout.cf.taste.model.PreferenceArray;
import org.apache.mahout.cf.taste.similarity.ItemSimilarity;
import org.apache.mahout.math.map.OpenLongDoubleHashMap;

/**
 * Itembased recommender that uses weighted sum estimation enhanced by baseline estimates
 */
public class BiasedItemBasedRecommender extends GenericItemBasedRecommender {
  
  private final int k;
  
  private final double mu;
  private final OpenLongDoubleHashMap itemBiases;
  private final OpenLongDoubleHashMap userBiases;

  private final ItemSimilarity similarity;

  public BiasedItemBasedRecommender(DataModel dataModel, ItemSimilarity similarity, int k, double lambda2,
      double lambda3) throws TasteException {
    super(dataModel, similarity);
    this.k = k;
    this.similarity = similarity;

    RunningAverage averageRating = new FullRunningAverage();
    LongPrimitiveIterator itemIDs = getDataModel().getItemIDs();
    while (itemIDs.hasNext()) {
      for (Preference pref : getDataModel().getPreferencesForItem(itemIDs.next())) {
        averageRating.addDatum(pref.getValue());
      }
    }

    mu = averageRating.getAverage();

    itemBiases = new OpenLongDoubleHashMap(getDataModel().getNumItems());
    userBiases = new OpenLongDoubleHashMap(getDataModel().getNumUsers());

    itemIDs = getDataModel().getItemIDs();
    while (itemIDs.hasNext()) {
      long itemID = itemIDs.nextLong();
      PreferenceArray preferences = getDataModel().getPreferencesForItem(itemID);
      double sum = 0;
      for (Preference pref : preferences) {
        sum += pref.getValue() - mu;
      }
      double bi = sum / (lambda2 + preferences.length());
      itemBiases.put(itemID, bi);
    }

    LongPrimitiveIterator userIDs = getDataModel().getUserIDs();
    while (userIDs.hasNext()) {
      long userID = userIDs.nextLong();
      PreferenceArray preferences = getDataModel().getPreferencesFromUser(userID);
      double sum = 0;
      for (Preference pref : preferences) {
        sum += pref.getValue() - mu - itemBiases.get(pref.getItemID());
      }
      double bu = sum / (lambda3 + preferences.length());
      userBiases.put(userID, bu);
    }
  }

  @Override
  public float estimatePreference(long userID, long itemID) throws TasteException {
    PreferenceArray preferencesFromUser = getDataModel().getPreferencesFromUser(userID);
    Float actualPref = getPreferenceForItem(preferencesFromUser, itemID);
    if (actualPref != null) {
      return actualPref;
    }
    return doEstimatePreference(userID, preferencesFromUser, itemID);
  }

  private static Float getPreferenceForItem(PreferenceArray preferencesFromUser, long itemID) {
    int size = preferencesFromUser.length();
    for (int i = 0; i < size; i++) {
      if (preferencesFromUser.getItemID(i) == itemID) {
        return preferencesFromUser.getValue(i);
      }
    }
    return null;
  }

  protected double baselineEstimate(long userID, long itemID) throws TasteException {
    return mu + userBiases.get(userID) + itemBiases.get(itemID);
  }

  @Override
  protected float doEstimatePreference(long userID, PreferenceArray preferencesFromUser, long itemID)
      throws TasteException {
    double preference = 0.0;
    double totalSimilarity = 0.0;
    int count = 0;
    long[] userIDs = preferencesFromUser.getIDs();
    float[] ratings = new float[userIDs.length];
    long[] itemIDs = new long[userIDs.length];
            
    double[] similarities = similarity.itemSimilarities(itemID, userIDs);

    for (int n = 0; n < preferencesFromUser.length(); n++) {
      ratings[n] = preferencesFromUser.get(n).getValue();
      itemIDs[n] = preferencesFromUser.get(n).getItemID();
    }

    quickSort(similarities, ratings, itemIDs, 0, (similarities.length - 1));

    for (int i = 0; i < Math.min(k, similarities.length); i++) {
      double theSimilarity = similarities[i];
      if (!Double.isNaN(theSimilarity)) {
        preference += theSimilarity * (ratings[i] - baselineEstimate(userID, itemIDs[i]));
        totalSimilarity += theSimilarity;
        count++;
      }
    }
    if (count <= 1) {
      return Float.NaN;
    }

    float estimate = (float) (baselineEstimate(userID, itemID) + (preference / totalSimilarity));
    return estimate;
  }  

  protected void quickSort(double[] similarities, float[] values, long[] otherValues, int start, int end) {
    if (start < end) {
      double pivot = similarities[end];
      float pivotValue = values[end];
      int i = start;
      int j = end;
      while (i != j) {
        if (similarities[i] > pivot) {
          i = i + 1;
        }
        else {
          similarities[j] = similarities[i];
          values[j] = values[i];
          otherValues[j] = otherValues[i];
          similarities[i] = similarities[j - 1];
          values[i] = values[j - 1];
          otherValues[i] = otherValues[j - 1];
          j = j - 1;
        }
      }
      similarities[j] = pivot;
      values[j] = pivotValue ;
      quickSort(similarities, values, otherValues, start, j - 1);
      quickSort(similarities, values, otherValues, j + 1, end);
    }
  }  
}
