package net.cabezudo.sofia.core.ws.responses;

import java.util.Locale;
import net.cabezudo.json.JSONPair;
import net.cabezudo.json.values.JSONObject;
import net.cabezudo.sofia.core.sites.Site;
import net.cabezudo.sofia.core.texts.TextManager;

/**
 * @author <a href="http://cabezudo.net">Esteban Cabezudo</a>
 * @version 0.01.00, 2019.05.10
 */
public class Response {

  public enum Type {
    ACTION, CREATE, DATA, READ, SET, UPDATE, VALIDATION
  }

  private JSONObject jsonObject;
  private final String id;
  private final Type messageType;
  private final String message;
  private final Object[] os;

  public Response(String id, Type messageType, String message, Object... os) {
    this.id = id;
    this.messageType = messageType;
    this.message = message;
    this.os = os;
  }

  public JSONObject toJSON(Site site, Locale locale) {
    if (jsonObject == null) {
      jsonObject = new JSONObject();
      jsonObject.add(new JSONPair("status", id));
      jsonObject.add(new JSONPair("type", messageType.toString()));
      jsonObject.add(new JSONPair("message", TextManager.get(site, locale, message, os)));
    }
    return jsonObject;
  }
}