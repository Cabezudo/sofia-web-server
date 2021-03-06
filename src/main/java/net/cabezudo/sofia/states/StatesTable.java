package net.cabezudo.sofia.states;

import net.cabezudo.sofia.core.configuration.Configuration;
import net.cabezudo.sofia.countries.CountriesTable;

/**
 * @author <a href="http://cabezudo.net">Esteban Cabezudo</a>
 * @version 0.01.00, 2019.04.26
 */
public class StatesTable {

  public static final String DATABASE_NAME = Configuration.getInstance().getDatabaseName();
  public static final String NAME = "states";
  public static final String CREATION_QUERY
          = "CREATE TABLE " + NAME + " "
          + "("
          + "`id` INT NOT NULL AUTO_INCREMENT, "
          + "`country` INT NOT NULL, "
          + "`name` VARCHAR(100) NOT NULL, "
          + "PRIMARY KEY (`id`), "
          + "FOREIGN KEY (`country`) REFERENCES " + CountriesTable.DATABASE_NAME + "." + CountriesTable.NAME + "(`id`), "
          + "UNIQUE INDEX `iCountryName` (`country`, `name`)"
          + ") "
          + "CHARACTER SET = UTF8";

  private StatesTable() {
    // Utility classes should not have public constructors.
  }
}
