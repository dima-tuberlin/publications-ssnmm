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

package de.tuberlin.dima.recsys.ssnmm;

/**
 * Reusable class to hold a rating
 */
public class Rating implements Cloneable {

  private int user;
  private int item;
  private double rating;

  void set(int user, int item, double rating) {
    this.user = user;
    this.item = item;
    this.rating = rating;
  }

  public int user() {
    return user;
  }

  public int item() {
    return item;
  }

  public double rating() {
    return rating;
  }

  @Override
  public Rating clone() {
    Rating clone = new Rating();
    clone.set(user, item, rating);
    return clone;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof Rating) {
      Rating other = (Rating) o;
      return user == other.user && item == other.item;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return 31 * user + item;
  }
}