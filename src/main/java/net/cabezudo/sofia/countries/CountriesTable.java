package net.cabezudo.sofia.countries;

/**
 * @author <a href="http://cabezudo.net">Esteban Cabezudo</a>
 * @version 0.01.00, 2019.04.26
 */
public class CountriesTable {

  public static final String DATABASE_NAME = "sofia";
  public static final String NAME = "countries";
  public static final String CREATION_QUERY
          = "CREATE TABLE " + CountriesTable.NAME + " "
          + "("
          + "`id` INT NOT NULL AUTO_INCREMENT, "
          + "`phoneCode` INT NOT NULL, "
          + "`twoLettersCountryCode` CHAR(2) NOT NULL, "
          + "PRIMARY KEY (`id`)"
          + ") "
          + "CHARACTER SET = UTF8";

  private CountriesTable() {
    // Nothing to do here. Utility classes should not have public constructors.
  }
}
