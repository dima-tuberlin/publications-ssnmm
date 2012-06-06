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

import com.google.common.collect.Iterators;
import org.apache.mahout.cf.taste.eval.DataModelBuilder;
import org.apache.mahout.cf.taste.impl.common.FastByIDMap;
import org.apache.mahout.cf.taste.impl.common.LongPrimitiveIterator;
import org.apache.mahout.cf.taste.impl.model.GenericDataModel;
import org.apache.mahout.cf.taste.impl.model.GenericUserPreferenceArray;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.model.Preference;
import org.apache.mahout.cf.taste.model.PreferenceArray;
import org.apache.mahout.common.iterator.FixedSizeSamplingIterator;

import java.util.Arrays;

/**
 * Applies the 'interaction-cut' (selective down-sampling of power users) to the dataset
 */
class InteractionCutDataModelBuilder implements DataModelBuilder {

    private final int maxPrefsPerUser;

    public InteractionCutDataModelBuilder(int maxPrefsPerUser) {
      this.maxPrefsPerUser = maxPrefsPerUser;
    }

    @Override
    public DataModel buildDataModel(FastByIDMap<PreferenceArray> trainingData) {

      FastByIDMap<PreferenceArray> sampledTrainingData = new FastByIDMap<PreferenceArray>();

      LongPrimitiveIterator userIDs = trainingData.keySetIterator();
      while (userIDs.hasNext()) {
        long userID = userIDs.nextLong();
        PreferenceArray prefs = trainingData.get(userID);
        if (prefs.length() > maxPrefsPerUser) {
          Preference[] sampledPrefs = Iterators.toArray(new FixedSizeSamplingIterator<Preference>(
              maxPrefsPerUser, prefs.iterator()), Preference.class);
          sampledTrainingData.put(userID, new GenericUserPreferenceArray(Arrays.asList(sampledPrefs)));
        } else {
          sampledTrainingData.put(userID, prefs);
        }
      }

      return new GenericDataModel(sampledTrainingData);
    }
  }
