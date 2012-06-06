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

import com.google.common.base.Preconditions;
import com.google.common.collect.UnmodifiableIterator;
import org.apache.mahout.common.iterator.FileLineIterator;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.regex.Pattern;

/**
 * A class to iterate over a ratings file in a streaming manner
 */
public class RatingsIterable extends UnmodifiableIterator<Rating> implements Iterable<Rating> {

  private Iterator<String> lines;
  private final Rating rating;

  private static final Pattern SEP = Pattern.compile("[,\t]");

  public RatingsIterable(File ratings) throws IOException {
    Preconditions.checkNotNull(ratings);
    this.rating = new Rating();
    this.lines = new FileLineIterator(ratings);
  }

  @Override
  public Iterator<Rating> iterator() {
    return this;
  }

  @Override
  public boolean hasNext() {
    return lines.hasNext();
  }

  @Override
  public Rating next() {
    String[] parts = SEP.split(lines.next());
    int user = Integer.parseInt(parts[0]);
    int item = Integer.parseInt(parts[1]);
    double value = Double.parseDouble(parts[2]);

    rating.set(user, item, value);

    return rating;
  }

}