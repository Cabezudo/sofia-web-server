package net.cabezudo.sofia.core.users;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import net.cabezudo.json.JSON;
import net.cabezudo.json.exceptions.JSONParseException;
import net.cabezudo.json.exceptions.PropertyNotExistException;
import net.cabezudo.json.values.JSONObject;
import net.cabezudo.sofia.core.cluster.ClusterException;
import net.cabezudo.sofia.core.database.sql.Database;
import net.cabezudo.sofia.core.http.url.parser.tokens.URLTokens;
import net.cabezudo.sofia.core.mail.MailServerException;
import net.cabezudo.sofia.core.passwords.Password;
import net.cabezudo.sofia.core.passwords.PasswordMaxSizeException;
import net.cabezudo.sofia.core.passwords.PasswordValidationException;
import net.cabezudo.sofia.core.passwords.PasswordValidator;
import net.cabezudo.sofia.core.sites.domainname.DomainNameMaxSizeException;
import net.cabezudo.sofia.core.ws.responses.Response;
import net.cabezudo.sofia.core.ws.servlet.services.Service;
import net.cabezudo.sofia.customers.CustomerService;
import net.cabezudo.sofia.emails.EMailAddressNotExistException;
import net.cabezudo.sofia.emails.EMailAddressValidationException;
import net.cabezudo.sofia.emails.EMailMaxSizeException;
import net.cabezudo.sofia.emails.EMailValidator;
import net.cabezudo.sofia.logger.Logger;
import net.cabezudo.sofia.names.LastNameManager;
import net.cabezudo.sofia.names.NameManager;
import net.cabezudo.sofia.people.PeopleManager;
import net.cabezudo.sofia.people.Person;

/**
 * @author <a href="http://cabezudo.net">Esteban Cabezudo</a>
 * @version 0.01.00, 2018.10.17
 */
public class AddUserService extends Service {

  public AddUserService(HttpServletRequest request, HttpServletResponse response, URLTokens tokens) throws ServletException {
    super(request, response, tokens);
  }

  @Override
  public void post() throws ServletException {

    String payload = getPayload();
    JSONObject jsonPayload;
    try {
      jsonPayload = JSON.parse(payload).toJSONObject();
    } catch (JSONParseException e) {
      super.sendError(HttpServletResponse.SC_BAD_REQUEST, e);
      return;
    }

    String name;
    try {
      name = jsonPayload.getString("name");
      NameManager.getInstance().validate(name);
    } catch (PropertyNotExistException e) {
      super.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing name property", e);
      return;
    }
    String lastName;
    try {
      lastName = jsonPayload.getString("lastName");
      LastNameManager.getInstance().validate(lastName);
    } catch (PropertyNotExistException e) {
      super.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing lastName property", e);
      return;
    }
    String address;
    try {
      address = jsonPayload.getString("email");
      EMailValidator.validate(address);
    } catch (EMailMaxSizeException | DomainNameMaxSizeException e) {
      Logger.warning(e);
      sendError(HttpServletResponse.SC_REQUEST_URI_TOO_LONG, e);
      return;
    } catch (PropertyNotExistException e) {
      super.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing email property", e);
      return;
    } catch (EMailAddressValidationException e) {
      sendResponse(new Response(Response.Status.ERROR, Response.Type.CREATE, e.getMessage(), e.getParameters()));
      return;
    }
    String base64Password;
    try {
      base64Password = jsonPayload.getString("password");
    } catch (PropertyNotExistException e) {
      super.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing password property", e);
      return;
    }
    Password password;
    try {
      password = Password.createFromBase64(base64Password);
      PasswordValidator.validate(password);
    } catch (PasswordMaxSizeException e) {
      super.sendError(HttpServletResponse.SC_REQUEST_URI_TOO_LONG, e);
      return;
    } catch (PasswordValidationException e) {
      super.sendResponse(new Response(Response.Status.ERROR, Response.Type.CREATE, e.getMessage()));
      return;
    }

    try (Connection connection = Database.getConnection()) {
      User owner = super.getUser();

      User user = UserManager.getInstance().getByEMail(address, site);
      if (user == null) {
        Person person = PeopleManager.getInstance().create(connection, name, lastName, owner);
        PeopleManager.getInstance().addEMailAddress(person, address);
      } else {
        try {
          CustomerService.sendRegistrationRetryAlert(address);
        } catch (MailServerException | IOException e) {
          sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, e);
          return;
        }
        sendResponse(new Response(Response.Status.ERROR, Response.Type.CREATE, "user.already.added"));
        return;
      }
      UserManager.getInstance().set(site, address, password);
      sendResponse(new Response(Response.Status.OK, Response.Type.CREATE, "user.added"));
    } catch (EMailAddressNotExistException e) {
      super.sendError(HttpServletResponse.SC_PRECONDITION_FAILED, e);
    } catch (SQLException | ClusterException e) {
      sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, e);
    }
  }
}
