package net.cabezudo.sofia.core.mail;

/**
 * @author <a href="http://cabezudo.net">Esteban Cabezudo</a>
 * @version 0.01.00, 2018.08.06
 */
public class MailServer {

  private static MailServer instance;

  public static MailServer getInstance() {
    if (instance == null) {
      instance = new MailServer();
    }
    return instance;
  }

  public void send(Messages messages) throws MailServerException {
    sendWithJetMail(messages);
  }

  public void sendWithJetMail(Messages messages) throws MailServerException {
//    final String APIKey = Configuration.getInstance().get("mail.server.mailJet.api.key");
//    final String SecretKey = Configuration.getInstance().get("mail.server.mailJet.secret.key");
//
//    try (CloseableHttpClient client = HttpClients.createDefault()) {
//      HttpPost post = new HttpPost("https://api.mailjet.com/v3.1/send");
//      post.addHeader("Content-Type", "application/json");
//
//      JSONObject jsonObject = new JSONObject();
//      JSONPair jsonMessagesPair = new JSONPair("Messages", messages.toJSON());
//      jsonObject.add(jsonMessagesPair);
//
//      StringEntity entity = new StringEntity(jsonObject.toString());
//      post.setEntity(entity);
//      post.setHeader("Accept", "application/json");
//      post.setHeader("Content-type", "application/json");
//
//      UsernamePasswordCredentials creds = new UsernamePasswordCredentials(APIKey, SecretKey);
//      post.addHeader(new BasicScheme().authenticate(creds, post, null));
//
//      CloseableHttpResponse response = client.execute(post);
//      HttpEntity httpEntity = response.getEntity();
//      String body = EntityUtils.toString(httpEntity, StandardCharsets.UTF_8);
//    } catch (UnsupportedEncodingException e) {
//      throw new SofiaRuntimeException(e);
//    } catch (IOException e) {
//      throw new MailServerException(e);
//    } catch (org.apache.http.auth.AuthenticationException e) {
//      throw new MailServerException("Incorrect MailJet credentials on configuration.");
//    }
  }
}
