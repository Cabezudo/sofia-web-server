package net.cabezudo.sofia.core.sites;

import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.cabezudo.json.JSON;
import net.cabezudo.json.exceptions.JSONParseException;
import net.cabezudo.json.exceptions.PropertyNotExistException;
import net.cabezudo.json.values.JSONObject;
import net.cabezudo.sofia.core.InvalidPathParameterException;
import net.cabezudo.sofia.core.sites.domainname.DomainName;
import net.cabezudo.sofia.core.sites.domainname.DomainNameManager;
import net.cabezudo.sofia.core.system.SystemMonitor;
import net.cabezudo.sofia.core.users.User;
import net.cabezudo.sofia.core.ws.parser.tokens.Token;
import net.cabezudo.sofia.core.ws.parser.tokens.Tokens;
import net.cabezudo.sofia.core.ws.responses.Response;
import net.cabezudo.sofia.core.ws.servlet.services.Service;
import net.cabezudo.sofia.hostname.HostnameMaxSizeException;
import net.cabezudo.sofia.hostname.HostnameValidationException;
import net.cabezudo.sofia.hostname.HostnameValidator;

/**
 * @author <a href="http://cabezudo.net">Esteban Cabezudo</a>
 * @version 0.01.00, 2019.24.10
 *
 */
public class SiteModifyDomainNameService extends Service {

  private final Tokens tokens;

  public SiteModifyDomainNameService(HttpServletRequest request, HttpServletResponse response, Tokens tokens) throws ServletException {
    super(request, response);
    this.tokens = tokens;
  }

  @Override
  public void execute() throws ServletException {
    User owner = super.getUser();

    Token siteIdToken = tokens.getValue("siteId");
    Token domainNameIdToken = tokens.getValue("domainNameId");

    try {
      int siteId;
      int domainNameId;

      try {
        siteId = siteIdToken.toInteger();
      } catch (InvalidPathParameterException e) {
        sendError(HttpServletResponse.SC_NOT_FOUND, "Resource not found");
        return;
      }
      try {
        domainNameId = domainNameIdToken.toInteger();
      } catch (InvalidPathParameterException e) {
        sendError(HttpServletResponse.SC_NOT_FOUND, "Resource not found");
        return;
      }

      Site site = SiteManager.getInstance().getById(siteId, owner);
      if (site == null) {
        sendError(HttpServletResponse.SC_NOT_FOUND, "Resource not found");
        return;
      }

      String payload = getPayload();
      JSONObject jsonData = JSON.parse(payload).toJSONObject();
      String domainNameName = jsonData.getString("value");
      String messageKey = HostnameValidator.validate(domainNameName);

      DomainName domainName = new DomainName(domainNameId, siteId, domainNameName);

      DomainNameManager.getInstance().update(site, domainName, owner);

      sendResponse(new Response("OK", Response.Type.UPDATE, messageKey, domainNameName));
    } catch (JSONParseException | PropertyNotExistException | HostnameMaxSizeException e) {
      sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
    } catch (SQLException e) {
      SystemMonitor.log(e);
      sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Service unavailable");
    } catch (HostnameValidationException e) {
      sendResponse(new Response("ERROR", Response.Type.UPDATE, e.getMessage(), e.getParameters()));
    }
  }
}