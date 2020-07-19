package net.cabezudo.sofia.core.ws.servlet.services;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import net.cabezudo.sofia.core.configuration.Environment;
import net.cabezudo.sofia.core.sites.Site;
import net.cabezudo.sofia.core.users.User;
import net.cabezudo.sofia.core.webusers.WebUserDataManager;
import net.cabezudo.sofia.core.webusers.WebUserDataManager.ClientData;
import net.cabezudo.sofia.core.ws.parser.tokens.WSTokens;
import net.cabezudo.sofia.core.ws.responses.Response;

/**
 * @author <a href="http://cabezudo.net">Esteban Cabezudo</a>
 * @version 0.01.00, 2018.10.17
 * @param <T>
 */
public abstract class Service<T extends Response> {

  protected final HttpServletRequest request;
  protected final HttpServletResponse response;
  protected final WSTokens tokens;
  private final HttpSession session;
  protected final PrintWriter out;
  private String payload;

  protected Service(HttpServletRequest request, HttpServletResponse response, WSTokens tokens) throws ServletException {
    this.request = request;
    this.response = response;
    this.tokens = tokens;
    this.session = request.getSession();
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    try {
      out = response.getWriter();
    } catch (IOException e) {
      throw new ServletException(e);
    }
    String requestId = request.getHeader("RequestId");
    response.setHeader("RequestId", requestId);
  }

  public abstract void execute() throws ServletException;

  protected void sendResponse(T response) throws ServletException {
    try {
      out.print(response.toJSON(getSite(), getClientData().getLocale()));
    } catch (SQLException e) {
      sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, e.getMessage());
    }
  }

  protected void setClientData(ClientData clientData) {
    request.getSession().setAttribute("clientData", clientData);
  }

  protected ClientData getClientData() throws SQLException {
    ClientData clientData = (ClientData) request.getSession().getAttribute("clientData");
    if (clientData == null) {
      WebUserDataManager clientDataManager = WebUserDataManager.getInstance();
      clientData = clientDataManager.get(request);
      setClientData(clientData);
    }
    return clientData;
  }

  private String readPayload() throws ServletException {
    String body;
    try {
      body = request.getReader().lines().reduce("", (partialBody, line) -> partialBody + (line + "\n"));
    } catch (IOException e) {
      throw new ServletException(e);
    }
    return body;
  }

  protected String getPayload() throws ServletException {
    if (payload == null) {
      payload = readPayload();
    }
    return payload;
  }

  protected void sendError(int error, Throwable cause) throws ServletException {
    if (Environment.getInstance().isDevelopment()) {
      cause.printStackTrace();
    }
    sendError(error, cause.getMessage());
  }

  protected final void sendError(int error, String message) throws ServletException {
    try {
      response.sendError(error, message);
    } catch (IOException e) {
      throw new ServletException(e);
    }
  }

  public final HttpSession getSession() {
    return session;
  }

  protected Site getSite() {
    Site site = (Site) request.getAttribute("site");
    return site;
  }

  protected User getUser() {
    return (User) request.getAttribute("user");
  }
}