package net.cabezudo.sofia.core.users.autentication;

import net.cabezudo.sofia.core.cluster.ClusterException;
import net.cabezudo.sofia.core.passwords.Password;
import net.cabezudo.sofia.core.passwords.PasswordMaxSizeException;
import net.cabezudo.sofia.core.passwords.PasswordValidationException;
import net.cabezudo.sofia.core.passwords.PasswordValidator;
import net.cabezudo.sofia.core.sites.Site;
import net.cabezudo.sofia.core.sites.domainname.DomainNameMaxSizeException;
import net.cabezudo.sofia.core.users.User;
import net.cabezudo.sofia.core.users.UserManager;
import net.cabezudo.sofia.emails.EMailAddressValidationException;
import net.cabezudo.sofia.emails.EMailMaxSizeException;
import net.cabezudo.sofia.emails.EMailValidator;

/**
 * @author <a href="http://cabezudo.net">Esteban Cabezudo</a>
 * @version 0.01.00, 2018.07.16
 */
public class Authenticator {

  public User authorize(Site site, String address, Password password)
          throws EMailMaxSizeException, DomainNameMaxSizeException, PasswordMaxSizeException, EMailAddressValidationException, PasswordValidationException, ClusterException {
    EMailValidator.validate(address);
    PasswordValidator.validate(password);
    return UserManager.getInstance().login(site, address, password);
  }
}
