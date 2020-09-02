package net.cabezudo.sofia.restaurants;

import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.cabezudo.sofia.core.http.url.parser.tokens.URLToken;
import net.cabezudo.sofia.core.http.url.parser.tokens.URLTokens;
import net.cabezudo.sofia.core.ws.servlet.services.Service;

/**
 * @author <a href="http://cabezudo.net">Esteban Cabezudo</a>
 * @version 0.01.00, 2018.10.18
 */
public class RestaurantDataService extends Service {

  public RestaurantDataService(HttpServletRequest request, HttpServletResponse response, URLTokens tokens) throws ServletException {
    super(request, response, tokens);
  }

  @Override
  public void execute() throws ServletException {
    URLToken token = tokens.getValue("path");
    String path = token.toString();

    try {
      Restaurant restaurant = RestaurantManager.getInstance().get(path);
      out.print(restaurant.toJSON());
    } catch (SQLException e) {
      sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, e.getMessage());
    }
  }

}