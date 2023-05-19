/*
 * Copyright 2012 - 2023 Manuel Laggner
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
package org.tinymediamanager.core.threading;

import java.util.LinkedList;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * chain several {@link TmmTask}s in a controlled way
 *
 * @author Alex Bruns, Kai Werner
 */
public class TmmTaskChain implements TmmTaskListener {
  private static final Logger  LOGGER = LoggerFactory.getLogger(TmmTaskChain.class);

  private final Queue<TmmTask> tasks  = new LinkedList<>();

  public TmmTaskChain add(TmmTask task) {
    task.addListener(this);
    tasks.add(task);
    return this;
  }

  public void run() {
    startNextTask();
  }

  @Override
  public void processTaskEvent(TmmTaskHandle task) {
    switch (task.getState()) {
      case CANCELLED:
      case FAILED:
        LOGGER.debug("Task '{}' {} - continue with next", task.getClass().getName(), task.getState());
        startNextTask();
        break;

      case FINISHED:
        startNextTask();
        break;

      default:
        break;
    }
  }

  private void startNextTask() {
    TmmTask nextTask = tasks.poll();
    if (nextTask != null) {
      if (nextTask instanceof TmmThreadPool) {
        // add the main task to the named queue
        TmmTaskManager.getInstance().addMainTask((TmmThreadPool) nextTask);
      }
      else {
        // add to the unnamed queue
        TmmTaskManager.getInstance().addUnnamedTask(nextTask);
      }
    }
  }
}
