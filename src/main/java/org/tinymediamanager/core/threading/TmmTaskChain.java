package org.tinymediamanager.core.threading;

import java.util.LinkedList;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TmmTaskChain implements TmmTaskListener {

  private static final Logger  LOGGER = LoggerFactory.getLogger(TmmTaskChain.class);

  private final Queue<TmmTask> tasks  = new LinkedList<>();

  public TmmTaskChain add(TmmTask task) {
    task.addListener(this);
    tasks.add(task);
    return this;
  }

  public void run() {
    if (!tasks.isEmpty()) {
      TmmTaskManager.getInstance().addUnnamedTask(tasks.remove());
    }
  }

  @Override
  public void processTaskEvent(TmmTaskHandle task) {
    if (TmmTaskHandle.TaskState.FINISHED.equals(task.getState()) || TmmTaskHandle.TaskState.FAILED.equals(task.getState())) {
      TmmTask nextTask = tasks.poll();
      if (nextTask != null) {
        TmmTaskManager.getInstance().addUnnamedTask(nextTask);
      }
    }
    else if (TmmTaskHandle.TaskState.CANCELLED.equals(task.getState())) {
      LOGGER.info("Task canceled. Stopping chain");
    }
  }
}
