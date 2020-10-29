package net.cabezudo.sofia.core.webusers;

/**
 * @author <a href="http://cabezudo.net">Esteban Cabezudo</a>
 * @version 0.01.00, 2018.08.04
 */
public class WebUserDataTable {

  public static final String DATABASE = "sofia";
  public static final String NAME = "webUsersData";
  public static final String CREATION_QUERY
          = "CREATE TABLE " + NAME + " "
          + "("
          + "`id` INT NOT NULL AUTO_INCREMENT, "
          + "`sessionId` VARCHAR(32) NOT NULL, "
          + "`failLoginResponseTime` INT NOT NULL, "
          + "`languageCode` VARCHAR(2) NOT NULL, "
          + "`languageCountryCode` VARCHAR(2) NOT NULL, "
          + "`user` INT NOT NULL DEFAULT 0, "
          + "PRIMARY KEY (`id`), "
          + "UNIQUE INDEX `iSessionId` (`sessionId`(32)) "
          + ") "
          + "CHARACTER SET=UTF8";

  private WebUserDataTable() {
    // Nothing to do here. Utility classes should not have public constructors.
  }
}
