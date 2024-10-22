package org.amfoss.paneeer.data.local;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class TrashBinRealmModel extends RealmObject {

  @PrimaryKey private String trashbinpath;
  private String oldpath;
  private String datetime;
  private String timeperiod;

  public TrashBinRealmModel() {}

  public String getTrashbinpath() {
    return trashbinpath;
  }

  public void setDatetime(String datetime) {
    this.datetime = datetime;
  }

  public String getDatetime() {
    return datetime;
  }

  public void setOldpath(String oldpath) {
    this.oldpath = oldpath;
  }

  public String getOldpath() {
    return oldpath;
  }

  public void setTimeperiod(String timeperiod) {
    this.timeperiod = timeperiod;
  }
}
