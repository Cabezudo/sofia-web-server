package net.cabezudo.sofia.core.users.permission;

import net.cabezudo.sofia.core.sites.SitesTable;

/**
 * @author <a href="http://cabezudo.net">Esteban Cabezudo</a>
 * @version 0.01.00, 2019.04.26
 */
public class PermissionsTable {

  public static final String NAME = "permissions";
  public static final String CREATION_QUERY
          = "CREATE TABLE " + NAME + " "
          + "("
          + "`id` INT NOT NULL AUTO_INCREMENT, "
          + "`uri` VARCHAR(200) NOT NULL, "
          + "`site` INT NOT NULL, "
          + "PRIMARY KEY (`id`), "
          + "FOREIGN KEY (`site`) REFERENCES " + SitesTable.NAME + "(`id`), "
          + "UNIQUE INDEX `iURI` (`uri`, `site`)"
          + ") "
          + "CHARACTER SET = UTF8";

}
