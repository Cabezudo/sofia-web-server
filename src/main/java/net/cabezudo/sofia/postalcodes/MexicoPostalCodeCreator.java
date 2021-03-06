package net.cabezudo.sofia.postalcodes;

import net.cabezudo.sofia.core.cluster.ClusterException;
import net.cabezudo.sofia.core.configuration.ConfigurationException;
import net.cabezudo.sofia.core.data.DataCreator;
import net.cabezudo.sofia.core.exceptions.DataConversionException;

/**
 * @author <a href="http://cabezudo.net">Esteban Cabezudo</a>
 * @version 0.01.00, 2021.04.05
 */
public class MexicoPostalCodeCreator implements DataCreator {

  // 00:33:11.022
  @Override
  public void create() throws ClusterException, ConfigurationException, DataConversionException {
//    User owner = UserManager.getInstance().getAdministrator();
//    Language language;
//    try {
//      language = LanguageManager.getInstance().get("es");
//    } catch (InvalidTwoLettersCodeException e) {
//      throw new SofiaRuntimeException(e);
//    }
//    Country country = CountryManager.getInstance().get("MX");
//    Path postalCodesFilePath = PostalCodeManager.getInstance().getPostalCodesDataFile(country.getTwoLetterCountryCode());
//
//    Logger.info("Create postal codes for country %s using file: %s", country.getTwoLetterCountryCode(), postalCodesFilePath);
//    if (!Files.exists(postalCodesFilePath)) {
//      Logger.info("The postal codes file doesn't exists.");
//    }
//    String line = null;
//    int counter = 0;
//    try (BufferedReader br = new BufferedReader(new FileReader(postalCodesFilePath.toFile())); Connection connection = Database.getConnection()) {
//      boolean headers = true;
//      long startTime = new Date().getTime();
//      while ((line = br.readLine()) != null) {
////        if (!Environment.getInstance().isProduction()) {
////          if (counter < 1000) {
//        counter++;
////            continue;
////          } else {
////            counter = 0;
////          }
////        }
//        if (counter < 3) {
//          continue;
//        }
//        if (headers) {
//          headers = false;
//        } else {
//          String[] fields = line.split(Pattern.quote("|"));
//
//          String stateName = fields[4];
//          String cityName = fields[5];
//          String municipalityName = fields[3];
//          String settlementName = fields[1];
//          String settlementTypeName = fields[2];
//          String zoneName = fields[13];
//          String postalCodeNumber = fields[6];
//          String municipalitySettlementId = fields[12];
//          State state = StateManager.getInstance().add(country, stateName);
//
//          City city = CityManager.getInstance().get(connection, state, cityName, owner);
//
//          SettlementType settlementType = SettlementTypeManager.getInstance().add(connection, settlementTypeName);
//          Municipality municipality = MunicipalityManager.getInstance().add(connection, state, municipalityName, owner);
//          Zone zone = ZoneManager.getInstance().add(connection, zoneName);
//          Settlement settlement = SettlementManager.getInstance().add(connection, language, settlementType, city, municipality, zone, municipalitySettlementId, settlementName, owner);
//          try {
//            PostalCodeManager.getInstance().add(connection, settlement, Integer.parseInt(postalCodeNumber), owner);
//          } catch (NumberFormatException nfe) {
//            throw new DataConversionException("Invalid postal code Number " + postalCodeNumber + " on line " + counter);
//          }
//        }
//      }
//      long endTime = new Date().getTime();
//      Logger.debug("Time: %s", Utils.getMillisecondsToTime(endTime - startTime));
//    } catch (SQLException | ClusterException e) {
//      throw new ClusterException("Line: " + counter, e);
//    } catch (IOException e) {
//      throw new ConfigurationException("Can not read the file with the postal codes: " + postalCodesFilePath);
//    }
  }
}
