package net.cabezudo.sofia.core.passwords;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.cabezudo.json.JSON;
import net.cabezudo.json.exceptions.JSONParseException;
import net.cabezudo.json.exceptions.PropertyNotExistException;
import net.cabezudo.json.values.JSONObject;
import net.cabezudo.sofia.core.http.url.parser.tokens.URLTokens;
import net.cabezudo.sofia.core.ws.responses.Response;
import net.cabezudo.sofia.core.ws.servlet.services.Service;

/**
 * @author <a href="http://cabezudo.net">Esteban Cabezudo</a>
 * @version 0.01.00, 2018.10.18
 */
public class PasswordPairValidatorService extends Service {

  public PasswordPairValidatorService(HttpServletRequest request, HttpServletResponse response, URLTokens tokens) throws ServletException {
    super(request, response, tokens);
  }

  @Override
  public void post() throws ServletException {
    String payload = getPayload();
    try {
      JSONObject jsonPayload = JSON.parse(payload).toJSONObject();

      String base64Password = jsonPayload.getString("password");
      Password password = Password.createFromBase64(base64Password);

      String base64RepetitionPassword = jsonPayload.getString("repetitionPassword");
      Password repetitionPassword = Password.createFromBase64(base64RepetitionPassword);

      String messageKey;
      try {
        messageKey = PasswordValidator.validate(password);
      } catch (PasswordValidationException e) {
        sendResponse(new Response(Response.Status.ERROR, Response.Type.VALIDATION, e.getMessage(), e.getParameters()));
        return;
      }

      if (repetitionPassword.isEmpty()) {
        sendResponse(new Response(Response.Status.ERROR, Response.Type.VALIDATION, "password.pair.empty"));
        return;
      }

      if (!password.equals(repetitionPassword) && !repetitionPassword.isEmpty()) {
        sendResponse(new Response(Response.Status.ERROR, Response.Type.VALIDATION, "password.pair.do.not.match"));
        return;
      }

      sendResponse(new Response(Response.Status.OK, Response.Type.VALIDATION, messageKey));
    } catch (PasswordMaxSizeException e) {
      sendError(HttpServletResponse.SC_REQUEST_URI_TOO_LONG, e);
    } catch (JSONParseException | PropertyNotExistException e) {
      sendError(HttpServletResponse.SC_BAD_REQUEST, e);
    }
  }

}
