package net.cabezudo.sofia.core.hostname;

/**
 * @author <a href="http://cabezudo.net">Esteban Cabezudo</a>
 * @version 0.01.00, 2018.07.14
 */
public class HostnameValidator {

  private static HostnameValidator INSTANCE;

  public static HostnameValidator getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new HostnameValidator();
    }
    return INSTANCE;
  }

  public String validate(String hostname) throws HostnameMaxSizeException, HostnameValidationException {
    try {
      HostnameManager.getInstance().validate(hostname);
    } catch (EmptyHostnameException e) {
      throw new HostnameValidationException("hostname.empty", hostname);
    } catch (InvalidCharacterException e) {
      throw new HostnameValidationException("hostname.invalidCharacter", e.getChar().toString(), hostname);
    } catch (HostnameNotExistsException e) {
      throw new HostnameValidationException("hostname.notExists", hostname);
    }
    return "hostname.ok";
  }
}
