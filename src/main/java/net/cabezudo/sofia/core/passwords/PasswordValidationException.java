package net.cabezudo.sofia.core.passwords;

import net.cabezudo.sofia.core.ParametrizedException;

/**
 * @author <a href="http://cabezudo.net">Esteban Cabezudo</a>
 * @version 0.01.00, 2019.05.08
 */
public class PasswordValidationException extends ParametrizedException {

  public PasswordValidationException(String message, String... parameters) {
    super(message, parameters);
  }
}
