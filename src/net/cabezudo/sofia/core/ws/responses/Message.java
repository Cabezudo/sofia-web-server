package net.cabezudo.sofia.core.ws.responses;

import static net.cabezudo.sofia.core.ws.responses.AbstractMessage.Type.OK;

/**
 * @author <a href="http://cabezudo.net">Esteban Cabezudo</a>
 * @version 0.01.00, 2019.05.10
 */
public class Message extends AbstractMessage {

  public Message(String message, Object... os) {
    super(OK, message, os);
  }

}