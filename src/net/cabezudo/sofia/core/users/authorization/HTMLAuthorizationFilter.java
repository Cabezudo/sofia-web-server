package net.cabezudo.sofia.core.users.authorization;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.List;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.cabezudo.sofia.core.configuration.Configuration;
import net.cabezudo.sofia.core.configuration.Environment;
import net.cabezudo.sofia.core.http.QueryString;
import net.cabezudo.sofia.core.sites.Site;
import net.cabezudo.sofia.core.system.SystemMonitor;
import net.cabezudo.sofia.core.users.User;
import net.cabezudo.sofia.core.users.UserManager;
import net.cabezudo.sofia.core.users.autentication.NotLoggedException;
import net.cabezudo.sofia.core.users.permission.PermissionTypeManager;
import net.cabezudo.sofia.core.users.profiles.PermissionType;
import net.cabezudo.sofia.core.webusers.WebUserDataManager;
import net.cabezudo.sofia.core.webusers.WebUserDataManager.ClientData;

/**
 * @author <a href="http://cabezudo.net">Esteban Cabezudo</a>
 * @version 0.01.00, 2018.10.23
 */
public class HTMLAuthorizationFilter implements Filter {

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    // Nothing to do here
  }

  @Override
  public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {

    Site site = (Site) req.getAttribute("site");

    if (req instanceof HttpServletRequest) {
      HttpServletRequest request = (HttpServletRequest) req;
      HttpServletResponse response = (HttpServletResponse) res;

      ClientData clientData = (ClientData) request.getSession().getAttribute("clientData");
      System.out.println("html filter 1: " + clientData);
      User user = null;
      if (clientData != null) {
        user = clientData.getUser();
      }
      try {
        if (Environment.getInstance().isLocal()) {
          QueryString queryString = new QueryString(request);
          List<String> userParameterList = queryString.get("user");
          if (userParameterList != null && userParameterList.size() > 0) {
            String email = userParameterList.get(0);
            user = UserManager.getInstance().getByEMail(email, site);
            if (clientData == null) {
              clientData = WebUserDataManager.getInstance().get(request);
            }
            clientData.setUser(user);
          }
        }
      } catch (SQLException e) {
        SystemMonitor.log(e);
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        return;
      }

      System.out.println(request.getRequestURI() + ", user: " + user);
      request.getSession().setAttribute("user", user);
      System.out.println("html filter 2: " + clientData);

      String requestURI = request.getRequestURI();
      Path path = Paths.get(requestURI);
      if ("/".equals(path.toString())) {
        path = Paths.get("/index.html");
      }
      if (path.toString().endsWith("html")) {
        try {
          PermissionType permissionType = PermissionTypeManager.getInstance().get("read", site);
          if (!AuthorizationManager.getInstance().hasAuthorization(path.toString(), user, permissionType, site)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
          }
        } catch (NotLoggedException e) {
          if (requestURI.endsWith("html")) {
            String comebackPage = requestURI;
            if (request.getQueryString() != null) {
              comebackPage += "?" + request.getQueryString();
            }
            request.getSession().setAttribute("comebackPage", comebackPage);
          }
          response.sendRedirect(Configuration.getInstance().getLoginURL());
          return;
        } catch (SQLException e) {
          SystemMonitor.log(e);
          response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
          return;
        }
      }
    }

    chain.doFilter(req, res);
  }

  @Override
  public void destroy() {
    // Nothing to do here
  }
}
