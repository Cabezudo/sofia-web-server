package net.cabezudo.sofia.countries;

import net.cabezudo.sofia.core.configuration.Configuration;
import net.cabezudo.sofia.core.words.WordsTable;

/**
 * @author <a href="http://cabezudo.net">Esteban Cabezudo</a>
 * @version 0.01.00, 2019.04.26
 */
public class CountryNamesTable {

  public static final String DATABASE_NAME = Configuration.getInstance().getDatabaseName();
  public static final String NAME = "countryNames";
  public static final String CREATION_QUERY = WordsTable.getCreationQuery(CountriesTable.DATABASE_NAME, CountriesTable.NAME, DATABASE_NAME, NAME);

  private CountryNamesTable() {
    // Utility classes should not have public constructors.
  }
}
