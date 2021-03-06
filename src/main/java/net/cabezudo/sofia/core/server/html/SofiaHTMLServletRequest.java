package net.cabezudo.sofia.core.server.html;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

/**
 * @author <a href="http://cabezudo.net">Esteban Cabezudo</a>
 * @version 0.01.00, 2020.08.25
 */
public class SofiaHTMLServletRequest extends HttpServletRequestWrapper {

  private String serverName;
  private String requestURI;
  private String pathInfo;
  private int id;

  public SofiaHTMLServletRequest(HttpServletRequest request) {
    super(request);
    serverName = request.getServerName();
    requestURI = request.getRequestURI();
    pathInfo = requestURI;
  }

  @Override
  public String toString() {
    return "http://" + serverName + ":" + super.getLocalPort() + requestURI;
  }

  @Override
  public String getServerName() {
    return serverName;
  }

  public void setRequestURI(String requestURI) {
    this.requestURI = requestURI;
    this.pathInfo = requestURI;
  }

  @Override
  public String getRequestURI() {
    return requestURI;
  }

  @Override
  public String getPathInfo() {
    return pathInfo;
  }

  public void setServerName(String serverName) {
    this.serverName = serverName;
  }
}
