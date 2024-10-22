/*
 * Copyright 2012 - 2024 Manuel Laggner
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
package org.tinymediamanager.scraper.interfaces;

import java.util.List;

import org.tinymediamanager.scraper.entities.MediaType;

/**
 * just a dedicated interface, for JSPF to find all "special" Kodi impls.<br>
 * 
 * @author Myron Boyle
 * @since 3.0
 */
public interface IKodiMetadataProvider extends IMediaProvider {
  /**
   * get all Kodi scraper-plugins for the desired type
   * 
   * @param type
   *          the desired media type
   * @return all found plugins
   */
  List<IMediaProvider> getPluginsForType(MediaType type);

  /**
   * get the kodi scraper by the id
   * 
   * @param id
   *          the id of the kodi scraper
   * @return the found plugin or null
   */
  IMediaProvider getPluginById(String id);
}
