package net.cabezudo.sofia.core.users;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;
import net.cabezudo.sofia.core.configuration.Configuration;
import net.cabezudo.sofia.core.database.Database;
import net.cabezudo.sofia.core.logger.Logger;
import net.cabezudo.sofia.core.mail.MailServerException;
import net.cabezudo.sofia.core.mail.Message;
import net.cabezudo.sofia.core.passwords.Hash;
import net.cabezudo.sofia.core.passwords.Password;
import net.cabezudo.sofia.core.sites.Site;
import net.cabezudo.sofia.core.sites.SiteManager;
import net.cabezudo.sofia.core.templates.EMailTemplate;
import net.cabezudo.sofia.core.templates.TemplatesManager;
import net.cabezudo.sofia.core.users.profiles.Profile;
import net.cabezudo.sofia.core.users.profiles.ProfileManager;
import net.cabezudo.sofia.core.users.profiles.Profiles;
import net.cabezudo.sofia.core.users.profiles.UsersProfilesTable;
import net.cabezudo.sofia.core.ws.responses.Messages;
import net.cabezudo.sofia.customers.CustomerService;
import net.cabezudo.sofia.domains.DomainMaxSizeException;
import net.cabezudo.sofia.emails.EMail;
import net.cabezudo.sofia.emails.EMailAddressNotExistException;
import net.cabezudo.sofia.emails.EMailManager;
import net.cabezudo.sofia.emails.EMailMaxSizeException;
import net.cabezudo.sofia.emails.EMailNotExistException;
import net.cabezudo.sofia.emails.EMailValidator;
import net.cabezudo.sofia.emails.EMails;
import net.cabezudo.sofia.emails.EMailsTable;
import net.cabezudo.sofia.people.PeopleList;
import net.cabezudo.sofia.people.PeopleManager;
import net.cabezudo.sofia.people.PeopleTable;
import net.cabezudo.sofia.people.Person;

/**
 * @author <a href="http://cabezudo.net">Esteban Cabezudo</a>
 * @version 0.01.00, 2018.07.16
 */
public class UserManager {

  private static UserManager INSTANCE;

  private UserManager() {
    // Just to protect the instance
  }

  public static UserManager getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new UserManager();
    }
    return INSTANCE;
  }

  public void createAdministrator() throws SQLException {
    if (System.console() == null) {
      UserManager.getInstance().createAdministrator("Esteban", "Cabezudo", "esteban@cabezudo.net", Password.createFromPlain("1234"));
    } else {
      System.out.println("Create root user.");
      System.out.print("Name: ");
      String name = System.console().readLine();
      System.out.print("Lastname: ");
      String lastName = System.console().readLine();

      boolean validAddress;
      String address;
      do {
        System.out.print("e-Mail: ");
        address = System.console().readLine();
        try {
          Messages messages = EMailValidator.validate(address);
          if (messages.hasErrors()) {
            System.out.println("Invalid e-mail: " + address);
            validAddress = false;
          } else {
            validAddress = true;
          }
        } catch (EMailMaxSizeException | DomainMaxSizeException e) {
          System.out.println(e.getMessage());
          validAddress = false;
        }
      } while (!validAddress);

      boolean match;
      String plainPassword;
      do {
        System.out.print("Password: ");
        plainPassword = System.console().readLine();
        System.out.print("Repeat password: ");
        String otherPlainPassword = System.console().readLine();
        match = plainPassword.equals(otherPlainPassword);
        if (!match) {
          System.out.println("The passwords don't match.");
        }
      } while (!match);

      Password password = Password.createFromPlain(plainPassword);
      createAdministrator(name, lastName, address, password);
    }
  }

  public void createAdministrator(String name, String lastName, String address, Password password) throws SQLException {
    try (Connection connection = Database.getConnection(Configuration.getInstance().getDatabaseName())) {
      Site site = SiteManager.getInstance().getById(1);
      Person person = addPerson(connection, name, lastName, 1);
      EMail eMail = EMailManager.getInstance().create(connection, person.getId(), address);
      PeopleManager.getInstance().setPrimaryEMail(connection, person, eMail);
      try {
        User user = UserManager.getInstance().set(connection, site, address, password);
        Profile profile = ProfileManager.getInstance().create(connection, "Administrator", site);
        UserManager.getInstance().add(connection, user, profile, 1);
      } catch (EMailAddressNotExistException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private Person addPerson(Connection connection, String name, String lastName, int ownerId) throws SQLException {
    String query = "INSERT INTO " + PeopleTable.NAME + " (name, lastName, owner) VALUES (?, ?, ?)";
    PreparedStatement ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
    ps.setString(1, name);
    ps.setString(2, lastName);
    ps.setInt(3, ownerId);
    Logger.fine(ps);
    ps.executeUpdate();

    ResultSet rs = ps.getGeneratedKeys();
    if (rs.next()) {
      int id = rs.getInt(1);
      Person person = new Person(id, name, lastName, 1);
      return person;
    } else {
      throw new RuntimeException("Key not generated.");
    }
  }

  public User login(Site site, String address, Password password) throws SQLException {
    try (Connection connection = Database.getConnection(Configuration.getInstance().getDatabaseName())) {
      String query
              = "SELECT u.id, `site`, `eMail`, `creationDate`, `activated`, `passwordRecoveryUUID`, `passwordRecoveryDate` "
              + "FROM " + UsersTable.NAME + " AS u "
              + "LEFT JOIN " + EMailsTable.NAME + " AS e ON u.eMail = e.id "
              + "WHERE address = ? AND (site = ? OR u.id = 1) AND password = ?";
      PreparedStatement ps = connection.prepareStatement(query);
      ps.setString(1, address);
      ps.setInt(2, site.getId());
      ps.setBytes(3, password.getBytes());
      Logger.fine(ps);
      ResultSet rs = ps.executeQuery();

      if (rs.next()) {
        int id = rs.getInt("id");
        int siteId = rs.getInt("site");
        int eMailId = rs.getInt("eMail");
        Date creationDate = rs.getDate("creationDate");
        boolean activated = rs.getBoolean("activated");
        String passwordRecoveryUUID = rs.getString("passwordRecoveryUUID");
        Date passwordRecoveryDate = rs.getDate("passwordRecoveryDate");

        User user = new User(id, siteId, eMailId, creationDate, activated, passwordRecoveryUUID, passwordRecoveryDate);
        return user;
      }
      return null;
    }
  }

  public Message getRecoveryEMailData(Site site, String address, Hash hash) throws SQLException, IOException {
    try (Connection connection = Database.getConnection(Configuration.getInstance().getDatabaseName())) {
      updateHash(connection, address, hash);
      Person person = PeopleManager.getInstance().getByEMailAddress(connection, site, address);
      EMailTemplate emailRecoveryTemplate = TemplatesManager.getInstance().getEMailPasswordRecoveryTemplate(person.getLocale());

      emailRecoveryTemplate.set("name", person.getName());
      emailRecoveryTemplate.set("site.name", Configuration.getInstance().get("site.name"));
      emailRecoveryTemplate.set("password.change.uri", Configuration.getInstance().get("password.change.uri") + "?" + hash);
      emailRecoveryTemplate.set("password.change.hash.time", Configuration.getInstance().get("password.change.hash.time"));
      emailRecoveryTemplate.set("site.uri", Configuration.getInstance().get("site.uri"));

      EMail from = EMailManager.getInstance().get(connection, Configuration.getInstance().get("no.reply.email"));
      EMail to = EMailManager.getInstance().get(connection, address);

      return new Message(
              Configuration.getInstance().get("no.reply.name"),
              from,
              person.getName() + ' ' + person.getLastName(),
              to,
              emailRecoveryTemplate.getSubject(),
              emailRecoveryTemplate.getPlainText(),
              emailRecoveryTemplate.getHtmlText());
    }
  }

  private void updateHash(Connection connection, String address, Hash hash) throws SQLException {
    String query
            = "UPDATE " + UsersTable.NAME + " "
            + "SET passwordRecoveryUUID = ?, passwordRecoveryDate = ? "
            + "WHERE eMail = (SELECT id FROM " + EMailsTable.NAME + " WHERE address = ?)";
    PreparedStatement ps = connection.prepareStatement(query);
    ps.setString(1, hash.toString());
    Timestamp timestamp = new Timestamp(new Date().getTime());
    ps.setTimestamp(2, timestamp);
    ps.setString(3, address);
    Logger.fine(ps);
    ps.executeUpdate();
  }

  public Message getPasswordChangedEMailData(Site site, String address) throws SQLException, IOException {
    try (Connection connection = Database.getConnection(Configuration.getInstance().getDatabaseName())) {
      Person person = PeopleManager.getInstance().getByEMailAddress(connection, site, address);

      EMailTemplate emailRecoveryTemplate = TemplatesManager.getInstance().getEMailPasswordChangedTemplate(person.getLocale());

      emailRecoveryTemplate.set("name", person.getName());
      emailRecoveryTemplate.set("site.name", Configuration.getInstance().get("site.name"));
      emailRecoveryTemplate.set("site.uri", Configuration.getInstance().get("site.uri"));

      EMail from = EMailManager.getInstance().get(connection, Configuration.getInstance().get("no.reply.email"));
      EMail to = EMailManager.getInstance().get(connection, Configuration.getInstance().get("no.reply.email"));

      return new Message(
              Configuration.getInstance().get("no.reply.name"),
              from,
              person.getName() + ' ' + person.getLastName(),
              to,
              emailRecoveryTemplate.getSubject(),
              emailRecoveryTemplate.getPlainText(),
              emailRecoveryTemplate.getHtmlText());
    }
  }

  public void set(Site site, String address, Password password) throws SQLException, EMailAddressNotExistException {
    try (Connection connection = Database.getConnection(Configuration.getInstance().getDatabaseName())) {
      set(connection, site, address, password);
    }
  }

  public User set(Connection connection, Site site, String address, Password password) throws SQLException, EMailAddressNotExistException {
    EMail eMail = EMailManager.getInstance().get(connection, address);
    if (eMail == null) {
      throw new EMailAddressNotExistException("Can't find the e-mail with address " + address + ".", address);
    }

    deactivateAllPasswords(connection, eMail);

    String query = "INSERT INTO " + UsersTable.NAME + " (site, eMail, password, activated) VALUES (?, ?, ?, TRUE)";
    PreparedStatement ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
    ps.setInt(1, site.getId());
    ps.setInt(2, eMail.getId());
    ps.setBytes(3, password.getBytes());
    Logger.fine(ps);
    ps.executeUpdate();
    connection.setAutoCommit(true);
    ResultSet rs = ps.getGeneratedKeys();
    if (rs.next()) {
      int userId = rs.getInt(1);
      boolean activated = true;
      return new User(userId, site.getId(), eMail.getId(), null, activated, null, null);
    }
    throw new SQLException("Can't get the generated key");
  }

  private void deactivateAllPasswords(Connection connection, EMail eMail) throws SQLException {
    String query = "UPDATE " + UsersTable.NAME + " SET activated = false WHERE eMail = ?";
    PreparedStatement ps = connection.prepareStatement(query);
    ps.setLong(1, eMail.getId());
    Logger.fine(ps);
    ps.executeUpdate();
  }

  public User getByEMail(String address, Site site) throws SQLException {

    try (Connection connection = Database.getConnection(Configuration.getInstance().getDatabaseName())) {
      String query
              = "SELECT `id`, `site`, `eMail`, `creationDate`, `activated`, `passwordRecoveryUUID`, `passwordRecoveryDate` "
              + "FROM " + UsersTable.NAME + " AS u "
              + "LEFT JOIN " + EMailsTable.NAME + " AS e ON u.eMail = e.id "
              + "WHERE address = ? AND site = ?";
      PreparedStatement ps = connection.prepareStatement(query);
      ps.setString(1, address);
      ps.setInt(2, site.getId());
      Logger.fine(ps);
      ResultSet rs = ps.executeQuery();

      if (rs.next()) {
        int id = rs.getInt("id");
        int siteId = rs.getInt("site");
        int eMailId = rs.getInt("eMailId");
        Date creationDate = rs.getDate("creationDate");
        boolean activated = rs.getBoolean("activated");
        String passwordRecoveryUUID = rs.getString("passwordRecoveryUUID");
        Date passwordRecoveryDate = rs.getDate("passwordRecoveryDate");

        User user = new User(id, siteId, eMailId, creationDate, activated, passwordRecoveryUUID, passwordRecoveryDate);
        return user;
      }
      return null;
    }
  }

  public User getById(int personId) throws SQLException {
    try (Connection connection = Database.getConnection(Configuration.getInstance().getDatabaseName())) {
      return getById(connection, personId);
    }
  }

  public User getById(Connection connection, int personId) throws SQLException {
    String query
            = "SELECT u.id, `site`, `eMail`, `creationDate`, `activated`, `passwordRecoveryUUID`, `passwordRecoveryDate` "
            + "FROM " + UsersTable.NAME + " AS u "
            + "LEFT JOIN " + EMailsTable.NAME + " AS e ON u.eMail = e.id "
            + "WHERE personId = ?";
    PreparedStatement ps = connection.prepareStatement(query);
    ps.setInt(1, personId);
    Logger.fine(ps);
    ResultSet rs = ps.executeQuery();

    if (rs.next()) {
      int id = rs.getInt("id");
      int siteId = rs.getInt("site");
      int eMailId = rs.getInt("eMail");
      Date creationDate = rs.getDate("creationDate");
      boolean activated = rs.getBoolean("activated");
      String passwordRecoveryUUID = rs.getString("passwordRecoveryUUID");
      Date passwordRecoveryDate = rs.getDate("passwordRecoveryDate");

      User user = new User(id, siteId, eMailId, creationDate, activated, passwordRecoveryUUID, passwordRecoveryDate);
      return user;
    }
    return null;
  }

  public User getByHash(Connection connection, Hash hash) throws SQLException {
    String query
            = "SELECT `eMailId`, `creationDate`, `activated`, `passwordRecoveryUUID`, `passwordRecoveryDate`, `personId` "
            + "FROM " + UsersTable.NAME + " AS u "
            + "LEFT JOIN " + EMailsTable.NAME + " AS e ON u.eMailId = e.id "
            + "WHERE passwordRecoveryUUID = ?";
    PreparedStatement ps = connection.prepareStatement(query);
    ps.setString(1, hash.toString());
    Logger.fine(ps);
    ResultSet rs = ps.executeQuery();

    if (rs.next()) {
      int id = rs.getInt("id");
      int siteId = rs.getInt("site");
      int eMailId = rs.getInt("eMail");
      Date creationDate = rs.getDate("creationDate");
      boolean activated = rs.getBoolean("activated");
      String passwordRecoveryUUID = rs.getString("passwordRecoveryUUID");
      Date passwordRecoveryDate = rs.getDate("passwordRecoveryDate");

      User user = new User(id, siteId, eMailId, creationDate, activated, passwordRecoveryUUID, passwordRecoveryDate);
      return user;
    }
    return null;
  }

  public void changePassword(Site site, Hash hash, Password password) throws SQLException, ChangePasswordException, MailServerException, IOException, EMailNotExistException {
    try (Connection connection = Database.getConnection(Configuration.getInstance().getDatabaseName())) {
      User user = UserManager.getInstance().getByHash(connection, hash);
      if (user == null) {
        throw new ChangePasswordException("change.password.user.not.found.by.hash");
      }
      if (user.getPasswordRecoveryHash() == null) {
        throw new ChangePasswordException("change.password.hash.null");
      }
      long now = new Date().getTime() / 1000;
      long mailMaxAge = Configuration.getInstance().getInteger("password.change.hash.time");
      long hashAge = user.getPasswordRecoveryDate().getTime();
      long mailAge = now - hashAge;
      if (mailAge > mailMaxAge) {
        throw new ChangePasswordException("change.password.hash.old");
      }

      String query = "UPDATE " + UsersTable.NAME + " SET passwordRecoveryUUID = ?, password = ? WHERE site = ? AND passwordRecoveryUUID = ?";
      PreparedStatement ps = connection.prepareStatement(query);
      ps.setString(1, hash.toString());
      ps.setBytes(2, password.getBytes());
      ps.setInt(3, site.getId());
      ps.setString(4, hash.toString());
      Logger.fine(ps);
      ps.executeUpdate();

      EMail email = EMailManager.getInstance().get(connection, user.getMail().getId());
      CustomerService.sendPasswordChangedEMail(site, email.getAddress());
    }
  }

  public PeopleList list(User owner) throws SQLException, UserNotExistException {
    Logger.fine("Users list");

    try (Connection connection = Database.getConnection(Configuration.getInstance().getDatabaseName())) {
      String query = "SELECT count(p.id) AS total "
              + "FROM " + PeopleTable.NAME + " AS p "
              + "LEFT JOIN " + EMailsTable.NAME + " AS e ON p.id = personId "
              + "LEFT JOIN " + UsersTable.NAME + " AS u ON e.id = eMailId "
              + "WHERE password IS NOT NULL AND p.owner = ?";
      PreparedStatement ps = connection.prepareStatement(query);
      ps.setInt(1, owner.getId());
      Logger.fine(ps);
      ResultSet rs = ps.executeQuery();

      while (!rs.next()) {
        throw new RuntimeException("The select to count the number of users fail.");
      }

      long total = rs.getInt("total");

      query = "SELECT p.id AS personId, name, lastName, p.owner, e.id AS eMailId, address "
              + "FROM " + PeopleTable.NAME + " AS p "
              + "LEFT JOIN " + EMailsTable.NAME + " AS e ON p.id = personId "
              + "LEFT JOIN " + UsersTable.NAME + " AS u ON e.id = eMailId "
              + "WHERE password IS NOT NULL";
      ps = connection.prepareStatement(query);
      Logger.fine(ps);
      rs = ps.executeQuery();

      PeopleList list = new PeopleList();
      while (rs.next()) {
        int personId = rs.getInt("personId");
        String name = rs.getString("name");
        String lastName = rs.getString("lastName");
        int eMailId = rs.getInt("eMailId");
        String address = rs.getString("address");
        EMail eMail = new EMail(eMailId, personId, address);
        EMails eMails = new EMails(eMail);
        Person person = new Person(personId, name, lastName, eMails, owner);
        list.add(person);
      }
      return list;
    }
  }

  public Message getRegistrationRetryAlertEMailData(Site site, String address) throws SQLException, IOException {
    try (Connection connection = Database.getConnection(Configuration.getInstance().getDatabaseName())) {

      Person person = PeopleManager.getInstance().getByEMailAddress(connection, site, address);

      EMailTemplate emailRegistrationRetryAlertTemplate = TemplatesManager.getInstance().getEMailRegistrationRetryAlertTemplate(person.getLocale());

      emailRegistrationRetryAlertTemplate.set("name", person.getName());
      emailRegistrationRetryAlertTemplate.set("password.recover.page.url", Configuration.getInstance().get("password.recover.page.url"));
      emailRegistrationRetryAlertTemplate.set("site.name", Configuration.getInstance().get("site.name"));
      emailRegistrationRetryAlertTemplate.set("site.uri", Configuration.getInstance().get("site.uri"));

      EMail from = EMailManager.getInstance().get(connection, Configuration.getInstance().get("no.reply.email"));
      EMail to = EMailManager.getInstance().get(connection, address);

      return new Message(
              Configuration.getInstance().get("no.reply.name"),
              from,
              person.getName() + ' ' + person.getLastName(),
              to,
              emailRegistrationRetryAlertTemplate.getSubject(),
              emailRegistrationRetryAlertTemplate.getPlainText(),
              emailRegistrationRetryAlertTemplate.getHtmlText());
    }
  }

  public Profiles getProfiles(User user) throws SQLException {
    Logger.fine("User profile list for %s.", user);

    try (Connection connection = Database.getConnection(Configuration.getInstance().getDatabaseName())) {
      String query = "SELECT p.id, p.name, p.site FROM usersProfiles AS up LEFT JOIN profiles AS p ON up.profile = p.id WHERE up.user = ?";
      PreparedStatement ps = connection.prepareStatement(query);
      ps.setInt(1, user.getId());
      Logger.fine(ps);
      ResultSet rs = ps.executeQuery();

      Profiles list = new Profiles();
      while (rs.next()) {
        int id = rs.getInt("id");
        String name = rs.getString("name");
        int site = rs.getInt("site");
        Profile profile = new Profile(id, name, site);
        list.add(profile);
      }
      return list;
    }
  }

  public boolean isAdministratorSet() throws SQLException {
    return getById(1) != null;
  }

  private void add(Connection connection, User user, Profile profile, int ownerId) throws SQLException {
    String query = "INSERT INTO " + UsersProfilesTable.NAME + " (user, profile, owner) VALUES (?, ?, ?)";
    PreparedStatement ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
    ps.setInt(1, user.getId());
    ps.setInt(2, profile.getId());
    ps.setInt(3, ownerId);
    Logger.fine(ps);
    ps.executeUpdate();
  }
}
