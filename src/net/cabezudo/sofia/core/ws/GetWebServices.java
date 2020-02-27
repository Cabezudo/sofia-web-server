package net.cabezudo.sofia.core.ws;

import org.eclipse.jetty.http.HttpMethod;

/**
 * @author <a href="http://cabezudo.net">Esteban Cabezudo</a>
 * @version 0.01.00, 2020.02.27
 */
public class GetWebServices extends WebServices {

  @Override
  HttpMethod getMethod() {
    return HttpMethod.GET;
  }

}
