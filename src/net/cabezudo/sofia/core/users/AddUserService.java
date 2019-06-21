package net.cabezudo.sofia.core.users;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.cabezudo.json.JSON;
import net.cabezudo.json.exceptions.JSONParseException;
import net.cabezudo.json.exceptions.PropertyNotExistException;
import net.cabezudo.json.values.JSONObject;
import net.cabezudo.sofia.core.configuration.Configuration;
import net.cabezudo.sofia.core.database.Database;
import net.cabezudo.sofia.core.logger.Logger;
import net.cabezudo.sofia.core.mail.MailServerException;
import net.cabezudo.sofia.core.sites.Site;
import net.cabezudo.sofia.core.users.User;
import net.cabezudo.sofia.core.users.UserManager;
import net.cabezudo.sofia.core.ws.responses.ErrorMessage;
import net.cabezudo.sofia.core.ws.responses.Message;
import net.cabezudo.sofia.core.ws.responses.Messages;
import net.cabezudo.sofia.core.ws.responses.MultipleMessageResponse;
import net.cabezudo.sofia.core.ws.responses.SingleMessageResponse;
import net.cabezudo.sofia.customers.CustomerService;
import net.cabezudo.sofia.domains.DomainMaxSizeException;
import net.cabezudo.sofia.emails.EMailAddressNotExistException;
import net.cabezudo.sofia.emails.EMailMaxSizeException;
import net.cabezudo.sofia.emails.EMailValidator;
import net.cabezudo.sofia.names.LastNameManager;
import net.cabezudo.sofia.names.NameManager;
import net.cabezudo.sofia.core.passwords.Password;
import net.cabezudo.sofia.core.passwords.PasswordMaxSizeException;
import net.cabezudo.sofia.core.passwords.PasswordValidator;
import net.cabezudo.sofia.core.ws.servlet.services.Service;
import net.cabezudo.sofia.people.PeopleManager;
import net.cabezudo.sofia.people.Person;

/**
 * @author <a href="http://cabezudo.net">Esteban Cabezudo</a>
 * @version 0.01.00, 2018.10.17
 */
public class AddUserService extends Service {

  public AddUserService(HttpServletRequest request, HttpServletResponse response) throws ServletException {
    super(request, response);
  }

  @Override
  public void execute() throws ServletException {

    User owner = super.getUser();
    Site site = super.getSite();

    try (Connection connection = Database.getConnection(Configuration.getInstance().getDatabaseName())) {
      String payload = getPayload();
      JSONObject jsonPayload = JSON.parse(payload).toJSONObject();

      String name;
      try {
        name = jsonPayload.getString("name");
        Messages messages = NameManager.getInstance().validate(name);
        if (messages.hasErrors()) {
          sendResponse(new MultipleMessageResponse("NAME_VALIDATION", messages));
          return;
        }
      } catch (PropertyNotExistException e) {
        super.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing name property");
        return;
      }
      String lastName;
      try {
        lastName = jsonPayload.getString("lastName");
        Messages messages = LastNameManager.getInstance().validate(lastName);
        if (messages.hasErrors()) {
          sendResponse(new MultipleMessageResponse("LAST_NAME_VALIDATION", messages));
          return;
        }
      } catch (PropertyNotExistException e) {
        super.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing lastName property");
        return;
      }
      String address;
      try {
        address = jsonPayload.getString("email");
        Messages messages = EMailValidator.validate(address);
        if (messages.hasErrors()) {
          sendResponse(new MultipleMessageResponse("EMAIL_VALIDATION", messages));
          return;
        }
      } catch (EMailMaxSizeException | DomainMaxSizeException e) {
        Logger.warning(e);
        sendError(HttpServletResponse.SC_REQUEST_URI_TOO_LONG, e);
        return;
      } catch (PropertyNotExistException e) {
        super.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing email property");
        return;
      }
      String base64Password;
      try {
        base64Password = jsonPayload.getString("password");
      } catch (PropertyNotExistException e) {
        super.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing password property");
        return;
      }
      Password password;
      try {
        password = Password.createFromBase64(base64Password);
        Messages messages = PasswordValidator.validate(password);
        if (messages.hasErrors()) {
          sendResponse(new MultipleMessageResponse("PASSWORD_VALIDATION", messages));
          return;
        }
      } catch (PasswordMaxSizeException e) {
        super.sendError(HttpServletResponse.SC_REQUEST_URI_TOO_LONG, e);
        return;
      }

      User user = UserManager.getInstance().getByEMail(address, site);
      if (user == null) {
        Person person = PeopleManager.getInstance().create(connection, name, lastName, owner);
        PeopleManager.getInstance().addEMailAddress(person, address);
      } else {
        try {
          CustomerService.sendRegistrationRetryAlert(site, address);
        } catch (MailServerException | IOException su) {
          sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, su.getMessage());
        }
        ErrorMessage message = new ErrorMessage("user.already.added");
        sendResponse(new SingleMessageResponse(message));
        return;
      }
      UserManager.getInstance().set(site, address, password);
      Message message = new Message("user.added");
      sendResponse(new SingleMessageResponse(message));
    } catch (EMailAddressNotExistException e) {
      super.sendError(HttpServletResponse.SC_PRECONDITION_FAILED, e);
    } catch (SQLException e) {
      sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, e.getMessage());
    } catch (JSONParseException e) {
      super.sendError(HttpServletResponse.SC_BAD_REQUEST, e);
    }
  }
}