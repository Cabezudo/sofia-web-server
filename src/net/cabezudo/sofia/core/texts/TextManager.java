package net.cabezudo.sofia.core.texts;

import java.util.Locale;
import net.cabezudo.sofia.core.sites.Site;

/**
 * @author <a href="http://cabezudo.net">Esteban Cabezudo</a>
 * @version 0.01.00, 2018.07.13
 */
public class TextManager {

  public static String get(Site site, Locale locale, String messageKey, Object... parameters) {
    switch (messageKey) {
      case "email.ok":
        return "La dirección de correo es correcta.";
      case "email.isEmpty":
        return "Debe especificar una dirección de correo.";
      case "email.invalidLocalPart":
        return "La direccion de correo '" + parameters[0] + "' tiene un caracter '" + parameters[1] + "' no válido.";
      case "email.arrobaMissing":
        return "Las direcciones de correo deben tener un arroba.";
      case "email.domain.ok":
        return "El dominio de la dirección de correo es correcto.";
      case "domain.ok":
        return "El nombre de dominio es correcto.";
      case "domain.empty":
        return "El nombre de dominio no puede estar vacío.";
      case "domain.invalidCharacter":
        return "El caracter '" + parameters[0] + "' en el nombre de dominio '" + parameters[1] + "' no es válido.";
      case "domain.missingDot":
        return "El nombre de dominio '" + parameters[0] + "' debe tener un punto.";
      case "domain.notExists":
        return "El nombre de dominio '" + parameters[0] + "' no existe.";
      case "password.ok":
        return "La contraseña tiene la forma correcta.";
      case "password.empty":
        return "Debe especificar una contraseña.";
      case "password.short":
        return "La contraseña es muy corta.";
      case "password.recovery.mail.sent":
        return "El correo para recuperar su contraseña ha sido enviado";
      case "login.fail":
        return "El usuario o la contraseña son incorrectos.";
      case "user.logged":
        return "El usuario está registrado en el sistema.";
      case "user.notLogged":
        return "El usuario no ha accedido al sistema.";
      case "name.ok":
        return "El nombre es correcto.";
      case "lastName.ok":
        return "El apellido es correcto.";
      case "person.email.in.use":
        return "El correo '" + parameters[0] + "' ya está siendo utilizado por un usuario.";
      case "user.already.added":
        return "El usuario ya ha sido agregado en el sistema.";
      case "user.added":
        return "El usuario ha sido agregado.";
      default:
        throw new InvalidKeyException("I can't found the text key " + messageKey + " for the locale " + locale);
    }
  }
}