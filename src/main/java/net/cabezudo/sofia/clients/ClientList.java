package net.cabezudo.sofia.clients;

import java.sql.SQLException;
import java.util.Iterator;
import net.cabezudo.json.JSONPair;
import net.cabezudo.json.values.JSONArray;
import net.cabezudo.json.values.JSONObject;
import net.cabezudo.json.values.JSONValue;
import net.cabezudo.sofia.core.EntityList;
import net.cabezudo.sofia.core.users.UserNotExistException;
import net.cabezudo.sofia.emails.EMail;
import net.cabezudo.sofia.emails.EMails;

/**
 * @author <a href="http://cabezudo.net">Esteban Cabezudo</a>
 * @version 0.01.00, 2019.03.13
 */
public class ClientList extends EntityList<Client> {

  private final Clients clients;

  public ClientList(int offset, int pageSize) {
    super(offset, pageSize);
    this.clients = new Clients();
  }

  @Override
  public Iterator<Client> iterator() {
    return clients.iterator();
  }

  public void add(Client p) throws SQLException, UserNotExistException {
    clients.add(p);
  }

  @Override
  public JSONValue toJSONTree() {
    JSONObject listObject = new JSONObject();
    JSONArray jsonRecords = new JSONArray();
    JSONPair jsonRecordsPair = new JSONPair("records", jsonRecords);
    listObject.add(jsonRecordsPair);
    clients.forEach((client) -> {
      JSONObject jsonPerson = new JSONObject();
      jsonPerson.add(new JSONPair("id", client.getId()));
      jsonPerson.add(new JSONPair("name", client.getName()));
      jsonPerson.add(new JSONPair("lastName", client.getLastName()));
      JSONArray jsonMails = new JSONArray();
      jsonPerson.add(new JSONPair("userNames", jsonMails));
      EMails eMails = client.getEMails();
      for (EMail eMail : eMails) {
        JSONObject jsonEMail = new JSONObject();
        jsonEMail.add(new JSONPair("id", eMail.getId()));
        jsonEMail.add(new JSONPair("address", eMail.getAddress()));
        jsonMails.add(jsonEMail);
      }
      jsonRecords.add(jsonPerson);
    });

    return listObject;
  }
}
