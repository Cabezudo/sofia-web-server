package net.cabezudo.sofia.cities;

import net.cabezudo.sofia.core.configuration.Configuration;
import net.cabezudo.sofia.core.users.UsersTable;
import net.cabezudo.sofia.states.StatesTable;

/**
 * @author <a href="http://cabezudo.net">Esteban Cabezudo</a>
 * @version 0.01.00, 2019.04.26
 */
public class CitiesTable {

  public static final String DATABASE_NAME = Configuration.getInstance().getDatabaseName();
  public static final String NAME = "cities";
  public static final String CREATION_QUERY
          = "CREATE TABLE " + NAME + " "
          + "("
          + "`id` INT NOT NULL AUTO_INCREMENT, "
          + "`state` INT NOT NULL, "
          + "`name` VARCHAR(100) NOT NULL, "
          + "`latitude` DECIMAL(7, 5) NOT NULL, "
          + "`longitude` DECIMAL(8, 5) NOT NULL, "
          + "`owner` INT NOT NULL, "
          + "PRIMARY KEY (`id`), "
          + "FOREIGN KEY (`state`) REFERENCES " + StatesTable.DATABASE_NAME + "." + StatesTable.NAME + "(`id`), "
          + "FOREIGN KEY (`owner`) REFERENCES " + UsersTable.DATABASE_NAME + "." + UsersTable.NAME + "(`id`), "
          + "UNIQUE INDEX `iStateName` (`state`, `name`)"
          + ") "
          + "CHARACTER SET = UTF8";

  private CitiesTable() {
    // Utility classes should not have public constructors.
  }

}
