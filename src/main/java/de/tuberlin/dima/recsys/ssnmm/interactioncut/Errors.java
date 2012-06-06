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

import org.apache.mahout.cf.taste.impl.common.FullRunningAverageAndStdDev;
import org.apache.mahout.cf.taste.impl.common.RunningAverageAndStdDev;

/**
 * Class to compute the prediction error in an updatable fashion
 */
class Errors {

  private final int maxPrefsPerUser;

  private final RunningAverageAndStdDev rmse = new FullRunningAverageAndStdDev();
  private final RunningAverageAndStdDev mae = new FullRunningAverageAndStdDev();

  public Errors(int maxPrefsPerUser) {
    this.maxPrefsPerUser = maxPrefsPerUser;
  }

  public void record(double rmse, double mae) {
    this.rmse.addDatum(rmse);
    this.mae.addDatum(mae);
  }

  @Override
  public String toString() {
    return maxPrefsPerUser + "\t" + rmse.getAverage() + "\t" + rmse.getStandardDeviation() + "\t" + mae.getAverage()
        + "\t" + mae.getStandardDeviation();
  }
}