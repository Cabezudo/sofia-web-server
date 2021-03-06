package net.cabezudo.sofia.core.sites;

import net.cabezudo.sofia.core.configuration.Configuration;

/**
 * @author <a href="http://cabezudo.net">Esteban Cabezudo</a>
 * @version 0.01.00, 2018.07.17
 */
public class SitesTable {

  private SitesTable() {
    // Utility classes should not have public constructors
  }
  public static final String DATABASE_NAME = Configuration.getInstance().getDatabaseName();
  public static final String NAME = "sites";
  public static final String CREATION_QUERY
          = "CREATE TABLE " + NAME + " "
          + "("
          + "`id` INT NOT NULL AUTO_INCREMENT, "
          + "`name` VARCHAR(" + Site.NAME_MAX_LENGTH + ") NOT NULL, "
          + "`basePath` VARCHAR(100) NOT NULL, "
          + "`domainName` INT NOT NULL DEFAULT 0, "
          + "`version` INT NOT NULL DEFAULT " + SiteManager.DEFAULT_VERSION + ", "
          + "PRIMARY KEY (`id`), "
          + "UNIQUE INDEX `iName` (`name`)"
          + ") "
          + "CHARACTER SET = UTF8";
}
