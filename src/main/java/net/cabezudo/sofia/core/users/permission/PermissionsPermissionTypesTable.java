package net.cabezudo.sofia.core.users.permission;

import net.cabezudo.sofia.core.sites.SitesTable;

/**
 * @author <a href="http://cabezudo.net">Esteban Cabezudo</a>
 * @version 0.01.00, 2019.04.26
 */
public class PermissionsPermissionTypesTable {

  public static final String NAME = "permissionsPermissionTypes";
  public static final String CREATION_QUERY
          = "CREATE TABLE " + NAME + " "
          + "("
          + "`permission` INT NOT NULL, "
          + "`permissionType` INT NOT NULL, "
          + "`site` INT NOT NULL, "
          + "PRIMARY KEY (`permission`, `permissionType`, `site`), "
          + "FOREIGN KEY (`permission`) REFERENCES " + PermissionsTable.NAME + "(`id`), "
          + "FOREIGN KEY (`permissionType`) REFERENCES " + PermissionTypesTable.NAME + "(`id`), "
          + "FOREIGN KEY (`site`) REFERENCES " + SitesTable.NAME + "(`id`)"
          + ") "
          + "CHARACTER SET = UTF8";

  private PermissionsPermissionTypesTable() {
    // Nothing to do here. Utility classes should not have public constructors.
  }
}
