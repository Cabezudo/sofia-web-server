package net.cabezudo.sofia.core.server.html;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import net.cabezudo.sofia.core.http.domains.DomainName;
import net.cabezudo.sofia.logger.Logger;

/**
 * @author <a href="http://cabezudo.net">Esteban Cabezudo</a>
 * @version 0.01.00, 2018.10.23
 */
public class URLTransformationFilter implements Filter {

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    // Nothing to do here
  }

  @Override
  public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws ServletException, IOException {

    if (req instanceof HttpServletRequest) {
      SofiaHTMLServletRequest request = new SofiaHTMLServletRequest((HttpServletRequest) req);
      Logger.debug("URL transformation filter.");

      changeURL(request);
      chain.doFilter(request, res);
    } else {
      chain.doFilter(req, res);
    }
  }

  private void changeURL(SofiaHTMLServletRequest request) {
    DomainName domainName = new DomainName(request.getServerName());
    String requestURI = request.getRequestURI();
    Logger.debug("ServerName: " + request.getServerName());
    Logger.debug("requestURI: " + requestURI);

    if (domainName.match("local.**")) {
      request.setServerName(domainName.parent().toString());
      Logger.debug("local.** change : serverName is now %s", domainName);
    }
    Logger.debug("%s no match with local.**", domainName);

    if (domainName.match("api.**")) {
      request.setServerName(domainName.parent().toString());
      request.setRequestURI("/api" + requestURI);
    }
    Logger.debug("%s no match with api.**", domainName);

    if (domainName.match("admin.**")) {
      request.setServerName(domainName.parent().toString());
      request.setRequestURI("/admin" + requestURI);
    }
    Logger.debug("%s no match with admin.**", domainName);
  }

  @Override
  public void destroy() {
    // Nothing to do here
  }
}
