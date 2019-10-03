package net.cabezudo.sofia.core.users.autentication;

import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.cabezudo.sofia.core.logger.Logger;
import net.cabezudo.sofia.core.ws.responses.Response;
import net.cabezudo.sofia.core.ws.servlet.services.Service;

/**
 * @author <a href="http://cabezudo.net">Esteban Cabezudo</a>
 * @version 0.01.00, 2018.10.18
 */
public class AuthenticatedService extends Service {

  public AuthenticatedService(HttpServletRequest request, HttpServletResponse response) throws ServletException {
    super(request, response);
  }

  @Override
  public void execute() throws ServletException {
    try {
      Logger.fine("Call the web service to return if the user is logged");
      if (getClientData().isLogged()) {
        sendResponse(new Response("OK", "login.logged"));
      } else {
        sendResponse(new Response("ERROR", "login.notLogged"));
      }
    } catch (SQLException e) {
      sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, e);
    }
  }
}
