package org.onebusaway.api.model.transit;

import java.io.Serializable;

public class TimeIntervalV2 implements Serializable {

  private static final long serialVersionUID = 1L;

  private long from = 0;

  private long to = 0;

  public long getFrom() {
    return from;
  }

  public void setFrom(long from) {
    this.from = from;
  }

  public long getTo() {
    return to;
  }

  public void setTo(long to) {
    this.to = to;
  }
}
