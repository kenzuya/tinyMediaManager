/*
 * Copyright 2012 - 2022 Manuel Laggner
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tinymediamanager.thirdparty;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.junit.Test;
import org.tinymediamanager.core.BasicITest;
import org.tinymediamanager.core.MediaSource;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.entities.MediaFileAudioStream;
import org.tinymediamanager.core.entities.MediaFileSubtitle;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.entities.MediaTrailer;
import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.entities.MovieSet;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.core.tvshow.entities.TvShowSeason;
import org.tinymediamanager.scraper.DynaEnum;

import com.alibaba.fastjson.util.ParameterizedTypeImpl;

public class ITJmtePatternTest extends BasicITest {

  @Test
  public void getProperties() throws Exception {
    printBeanInfo(Movie.class);
    printBeanInfo(MovieSet.class);
    printBeanInfo(TvShow.class);
    printBeanInfo(TvShowSeason.class);
    printBeanInfo(TvShowEpisode.class);
    printBeanInfo(Person.class);
    printBeanInfo(MediaRating.class);
    printBeanInfo(MediaFile.class);
    printBeanInfo(MediaFileAudioStream.class);
    printBeanInfo(MediaFileSubtitle.class);
    printBeanInfo(MediaTrailer.class);
    printBeanInfo(MediaSource.class);
  }

  private void printBeanInfo(Class<?> clazz) throws Exception {
    System.out.println("\n\n" + clazz.getName() + "\n");

    // access properties as Map
    BeanInfo info = Introspector.getBeanInfo(clazz);
    PropertyDescriptor[] pds = info.getPropertyDescriptors();

    for (PropertyDescriptor descriptor : pds) {
      if ("class".equals(descriptor.getDisplayName())) {
        continue;
      }

      if ("declaringClass".equals(descriptor.getDisplayName())) {
        continue;
      }

      if (descriptor.getReadMethod() != null) {
        final Type type = descriptor.getReadMethod().getGenericReturnType();
        if (type instanceof ParameterizedTypeImpl) {
          ParameterizedType pt = (ParameterizedTypeImpl) type;

          String typeAsString;
          Class<?> rawTypeClass = (Class<?>) pt.getRawType();
          typeAsString = rawTypeClass.getSimpleName() + "\\<";

          int index = 0;
          for (Type arg : pt.getActualTypeArguments()) {
            Class<?> argClass = (Class<?>) arg;
            typeAsString += getTypeName(argClass);

            index++;

            if (index < pt.getActualTypeArguments().length) {
              typeAsString += ",";
            }
          }
          typeAsString += "\\>";
          System.out.println("|" + typeAsString + "|" + descriptor.getDisplayName() + "|");
        }
        else {
          System.out.println("|" + getTypeName(descriptor.getReadMethod().getReturnType()) + "|" + descriptor.getDisplayName() + "|");
        }
      }
    }
  }

  private String getTypeName(Class<?> clazz) {
    String typeAsString;

    if (clazz.isEnum()) {
      typeAsString = "String";
    }
    else if (DynaEnum.class.isAssignableFrom(clazz)) {
      typeAsString = "String";
    }
    else {
      typeAsString = clazz.getSimpleName();
    }
    return typeAsString;
  }

}
