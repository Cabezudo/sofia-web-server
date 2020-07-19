package net.cabezudo.sofia.customers;

import java.io.IOException;
import java.sql.SQLException;
import net.cabezudo.sofia.core.mail.MailServer;
import net.cabezudo.sofia.core.mail.MailServerException;
import net.cabezudo.sofia.core.mail.Message;
import net.cabezudo.sofia.core.mail.Messages;
import net.cabezudo.sofia.core.passwords.Hash;
import net.cabezudo.sofia.core.sites.Site;
import net.cabezudo.sofia.core.users.UserManager;

/**
 * @author <a href="http://cabezudo.net">Esteban Cabezudo</a>
 * @version 0.01.00, 2018.08.06
 */
public class CustomerService {

  public static Hash sendPasswordRecoveryEMail(Site site, String address) throws SQLException, MailServerException, IOException {
    Hash hash = new Hash();

    Message message = UserManager.getInstance().getRecoveryEMailData(site, address, hash);
    Messages messages = new Messages(message);
    MailServer.getInstance().send(messages);

    return hash;
  }

  public static void sendPasswordChangedEMail(Site site, String address) throws MailServerException, SQLException, IOException {
    Message message = UserManager.getInstance().getPasswordChangedEMailData(site, address);
    Messages messages = new Messages(message);
    MailServer.getInstance().send(messages);
  }

  public static void sendRegistrationRetryAlert(Site site, String address) throws MailServerException, SQLException, IOException {
    Message message = UserManager.getInstance().getRegistrationRetryAlertEMailData(site, address);
    Messages messages = new Messages(message);
    MailServer.getInstance().send(messages);
  }
}