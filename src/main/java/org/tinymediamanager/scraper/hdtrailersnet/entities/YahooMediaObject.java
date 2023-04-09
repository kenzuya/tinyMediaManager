package org.tinymediamanager.scraper.hdtrailersnet.entities;

import java.util.ArrayList;
import java.util.List;

public class YahooMediaObject extends YahooBase {

  public String             id      = "";
  public YahooMeta          meta    = new YahooMeta();
  public List<YahooStream> streams = new ArrayList<YahooStream>();

}
