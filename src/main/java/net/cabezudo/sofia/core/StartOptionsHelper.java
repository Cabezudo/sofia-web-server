package net.cabezudo.sofia.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import net.cabezudo.json.JSON;
import net.cabezudo.json.exceptions.JSONParseException;
import net.cabezudo.json.values.JSONObject;
import net.cabezudo.sofia.core.cluster.ClusterException;
import net.cabezudo.sofia.core.configuration.Configuration;
import net.cabezudo.sofia.core.configuration.ConfigurationException;
import net.cabezudo.sofia.core.configuration.DataCreationException;
import net.cabezudo.sofia.core.configuration.DataCreator;
import net.cabezudo.sofia.core.configuration.SofiaDatabaseCreator;
import net.cabezudo.sofia.core.database.sql.Database;
import net.cabezudo.sofia.core.database.sql.DatabaseCreators;
import net.cabezudo.sofia.core.database.sql.DatabaseDataCreator;
import net.cabezudo.sofia.core.exceptions.DataConversionException;
import net.cabezudo.sofia.core.exceptions.SofiaRuntimeException;
import net.cabezudo.sofia.core.mail.MailServerException;
import net.cabezudo.sofia.core.passwords.Password;
import net.cabezudo.sofia.core.sites.Site;
import net.cabezudo.sofia.core.sites.SiteManager;
import net.cabezudo.sofia.core.sites.Sites;
import net.cabezudo.sofia.core.sites.domainname.DomainNameManager;
import net.cabezudo.sofia.core.sites.domainname.DomainNameMaxSizeException;
import net.cabezudo.sofia.core.sites.domainname.DomainNameNotExistsException;
import net.cabezudo.sofia.core.sites.domainname.MissingDotException;
import net.cabezudo.sofia.core.sites.texts.TextManager;
import net.cabezudo.sofia.core.users.HashTooOldException;
import net.cabezudo.sofia.core.users.NullHashException;
import net.cabezudo.sofia.core.users.User;
import net.cabezudo.sofia.core.users.UserManager;
import net.cabezudo.sofia.core.users.UserNotFoundByHashException;
import net.cabezudo.sofia.core.users.profiles.Profile;
import net.cabezudo.sofia.core.users.profiles.ProfileManager;
import net.cabezudo.sofia.core.validation.EmptyValueException;
import net.cabezudo.sofia.core.validation.InvalidCharacterException;
import net.cabezudo.sofia.emails.EMail;
import net.cabezudo.sofia.emails.EMailAddressNotExistException;
import net.cabezudo.sofia.emails.EMailAddressValidationException;
import net.cabezudo.sofia.emails.EMailManager;
import net.cabezudo.sofia.emails.EMailMaxSizeException;
import net.cabezudo.sofia.emails.EMailNotExistException;
import net.cabezudo.sofia.emails.EMailValidator;
import net.cabezudo.sofia.logger.Logger;
import net.cabezudo.sofia.people.PeopleManager;
import net.cabezudo.sofia.people.Person;

/**
 * @author <a href="http://cabezudo.net">Esteban Cabezudo</a>
 * @version 0.01.00, 2020.10.14
 */
class StartOptionsHelper {

  private final StartOptions startOptions;

  StartOptionsHelper(StartOptions startOptions) {
    this.startOptions = startOptions;
  }

  public void createAdministrator() throws ClusterException {
    if (System.console() == null || startOptions.hasIDE()) {
      createSofiaAdministrator("Esteban", "Cabezudo", "test@sofia.academy", Password.createFromPlain("1234"));
    } else {
      Utils.consoleOutLn("Create administrator user.");

      String address = getEMailAddress();

      EMail eMail = EMailManager.getInstance().get(address);
      if (eMail != null) {
        Utils.consoleOut("The user already exist. Set administrador privileges? [y/N]");
        String setPrivileges = System.console().readLine();
        if (setPrivileges.isBlank() || "n".equalsIgnoreCase(setPrivileges)) {
          System.exit(0);
        } else {
          setAdministratorPrivileges(address);
          System.exit(0);
        }
      }

      Utils.consoleOut("Name: ");
      String name = System.console().readLine();
      Utils.consoleOut("Lastname: ");
      String lastName = System.console().readLine();

      Password password = getPassword();
      createSofiaAdministrator(name, lastName, address, password);
    }
  }

  private String getEMailAddress() {
    boolean validAddress = false;
    String address;
    do {
      Utils.consoleOut("e-Mail: ");
      address = System.console().readLine();
      try {
        EMailValidator.validate(address);
        validAddress = true;
      } catch (EMailMaxSizeException | DomainNameMaxSizeException | EMailAddressValidationException e) {
        // FIX The message show the key for the error not the text
        Utils.consoleOut(e.getMessage());
      }
    } while (!validAddress);
    return address;
  }

  private void setAdministratorPrivileges(String address) throws ClusterException {
    Site site = SiteManager.getInstance().getById(1);
    User user = UserManager.getInstance().getByEMail(address, site);
    if (user == null) {
      Password password = getPassword();
      try {
        UserManager.getInstance().set(site, address, password);
      } catch (EMailAddressNotExistException e) {
        throw new SofiaRuntimeException(e);
      }
    }
    setProfile(user, Profile.ADMINISTRATOR);
  }

  private void setProfile(User user, Profile profile) throws ClusterException {
    try (Connection connection = Database.getConnection()) {
      connection.setAutoCommit(false);
      UserManager.getInstance().add(connection, user, profile, 1);
      connection.commit();
    } catch (SQLException e) {
      throw new ClusterException(e);
    }
  }

  private Password getPassword() {
    boolean match;
    String plainPassword;
    do {
      Utils.consoleOut("Password: ");
      char[] charPasswrod = System.console().readPassword();
      plainPassword = new String(charPasswrod);

      Utils.consoleOut("Repeat password: ");
      char[] otherCharPasswrod = System.console().readPassword();
      String otherPlainPassword = new String(otherCharPasswrod);

      match = plainPassword.equals(otherPlainPassword);
      if (!match) {
        Utils.consoleOutLn("The passwords don't match.");
      }
    } while (!match);

    return Password.createFromPlain(plainPassword);
  }

  private List<File> getClassLocationsForCurrentClasspath() {
    List<File> urls = new ArrayList<>();
    String javaClassPath = System.getProperty("java.class.path");
    Logger.debug("Java class path: %s", javaClassPath);
    if (javaClassPath != null) {
      for (String path : javaClassPath.split(File.pathSeparator)) {
        urls.add(new File(path));
      }
    }
    return urls;
  }

  public DatabaseCreators readModuleData() throws IOException, ConfigurationException {
    DatabaseCreators databaseCreators = new DatabaseCreators();

    List<File> files = getClassLocationsForCurrentClasspath();
    Path systemClassesPath = null;
    for (File file : files) {
      Logger.debug("Trying with %s like system class path.", file);
      if (file.isDirectory()) {
        systemClassesPath = file.toPath();
        Logger.debug("Using %s as class target.", systemClassesPath);
        break;
      }
    }

    if (systemClassesPath == null) {
      throw new SofiaRuntimeException("No class found in class paths.");
    }
    Path systemLibsPath = Configuration.getInstance().getSystemLibsPath();
    File systemLibsFile = systemLibsPath.toFile();
    // TODO read all the jar in directory

    FilenameFilter filter = (File file, String name) -> name.endsWith(".jar");
    String[] jarFileNames = systemLibsFile.list(filter);
    for (String jarFileName : jarFileNames) {
      readJAR(databaseCreators, systemClassesPath, Paths.get(jarFileName));
    }
    return databaseCreators;
  }

  private void readJAR(DatabaseCreators databaseCreators, Path systemClassesPath, Path jarFileName) throws IOException, ConfigurationException {
    Logger.debug("Check for %s.", jarFileName);

    DataCreator databaseCreator = null;
    JSONObject jsonAPIDefinition = null;
    try (JarFile jarFile = new JarFile(jarFileName.toFile());) {

      Enumeration<JarEntry> entries = jarFile.entries();
      while (entries.hasMoreElements()) {
        JarEntry jarEntry = entries.nextElement();
        String fileName = jarEntry.getName();
        ZipEntry zipEntry = jarFile.getEntry(fileName);
        if ("META-INF/apiDefinition.json".equals(fileName)) {
          InputStream is = jarFile.getInputStream(zipEntry);
          try (InputStreamReader isr = new InputStreamReader(is)) {
            String apiDefinition = new BufferedReader(isr).lines().collect(Collectors.joining("\n"));
            try {
              jsonAPIDefinition = JSON.parse(apiDefinition).toJSONObject();
            } catch (JSONParseException e) {
              throw new ConfigurationException("Can't parse the JAR entry META-INF/apiDefinition.json. " + e.getMessage(), e);
            }
          }
        }
        if ("META-INF/texts.json".equals(fileName)) {
          Logger.debug("Read texts.json from %s.", jarFileName);
          InputStream is = jarFile.getInputStream(zipEntry);
          try (InputStreamReader isr = new InputStreamReader(is)) {
            String texts = new BufferedReader(isr).lines().collect(Collectors.joining("\n"));
            try {
              JSONObject jsonTexts = JSON.parse(texts).toJSONObject();
              TextManager.add(jsonTexts);
            } catch (JSONParseException e) {
              throw new ConfigurationException("Can't parse the JAR entry META-INF/texts.json. " + e.getMessage(), e);
            }
          }
        }
        if (fileName.endsWith(".class")) {
//          Path classFilePath = systemClassesPath.resolve(fileName);
//          Path directoryName = classFilePath.getParent();
//          if (!Files.exists(directoryName)) {
//            Files.createDirectories(directoryName);
//          }
//          InputStream is = jarFile.getInputStream(zipEntry);
//          try (OutputStream os = new FileOutputStream(classFilePath.toFile());) {
//            long fileSize = zipEntry.getSize();
//            byte[] allBytes = new byte[(int) fileSize];
//
//            int size = is.read(allBytes);
//            if (size != fileSize) {
//              throw new IOException("Read " + size + "bytes from " + fileSize + " bytes.");
//            }
//            Logger.finest("Copy %s to %s.", zipEntry, classFilePath);
//            os.write(allBytes);
//          }
          String className = fileName.replace('/', '.').substring(0, fileName.length() - 6);
          Class<?> clazz;
          ClassLoader cl = ClassLoader.getSystemClassLoader();
          try {
            Logger.finest("Load class %s.", className);
            clazz = cl.loadClass(className);
          } catch (ClassNotFoundException e) {
            throw new SofiaRuntimeException(e);
          }
          if (clazz.isAnnotationPresent(DatabaseDataCreator.class)) {
            Logger.info("Found DatabaseDataCreator class %s.", clazz.getName());
            try {
              databaseCreator = (DataCreator) clazz.getDeclaredConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
              // TODO Explain the error better
              throw new ConfigurationException(e);
            } catch (NoSuchMethodException e) {
              throw new ConfigurationException("The class " + className + " don't have a default public constructor.");
            } catch (SecurityException e) {
              throw new ConfigurationException("The class " + className + " don't have a public constructor.");
            }
            databaseCreators.add(databaseCreator);
          }
        }
      }
      if (databaseCreator != null && jsonAPIDefinition != null) {
        databaseCreator.addAPIConfiguration(jsonAPIDefinition);
      }
    }
  }

  public String getDefaultDomainName() throws DataCreationException {
    try {
      if (System.console() != null && !startOptions.hasIDE()) {
        return askUserForDefaultDomainName();
      } else {
        Logger.info("Default domain name not set. " + (startOptions.hasIDE() ? "IDE mode set." : "No console found to ask."));
        return null;
      }
    } catch (SQLException | IOException e) {
      throw new DataCreationException(e);
    }
  }

  private String askUserForDefaultDomainName() throws SQLException, IOException {
    String baseDomainName;
    Utils.consoleOutLn("Create a site for the server.");
    Utils.consoleOutLn(
            "Enter the name of the host under which you want to manage the site on the network. It must be a valid domain name. "
            + "If you are only working locally you can leave the field blank and use localhost to access the configuration. "
            + "But if you need to access the site remotely you must enter a valid domain name.");
    boolean validDomain = false;
    do {
      Utils.consoleOut("Set the base domain: ");
      baseDomainName = System.console().readLine();
      if (baseDomainName.isEmpty()) {
        break;
      }
      try {
        DomainNameManager.getInstance().validate(baseDomainName);
        validDomain = true;
      } catch (EmptyValueException e) {
        Utils.consoleOutLn("The domain name is empty.");
      } catch (InvalidCharacterException e) {
        Utils.consoleOutLn("Invalid character '" + e.getChar() + "' in domain name");
      } catch (DomainNameNotExistsException e) {
        Utils.consoleOutLn("The domain name doesn't exist. Don't hava a DNS entry.");
      } catch (DomainNameMaxSizeException e) {
        Utils.consoleOutLn("The domain name is too large.");
      } catch (MissingDotException e) {
        Utils.consoleOutLn("A domain name must have a dot in it.");
      }
    } while (!validDomain);
    return baseDomainName;
  }

  public void changeUserPassword() throws ClusterException {
    Utils.consoleOutLn("Change user password.");
    String address = getEMailAddress();
    EMail eMail = EMailManager.getInstance().get(address);
    if (eMail == null) {
      Utils.consoleOut("The email address doesn't exist.");
      System.exit(0);
    }

    Sites sites = SiteManager.getInstance().getByUserEMail(eMail);
    if (sites.size() > 0) {
      for (Site site : sites) {
        Utils.consoleOutLn(site.getId() + " - " + site.getName());
      }
    }
    boolean valid;
    Site selectedSite = null;
    do {
      Utils.consoleOut("Select site for user: ");
      String siteValue = System.console().readLine();
      int siteId;
      try {
        siteId = Integer.parseInt(siteValue);
        valid = true;
      } catch (NumberFormatException e) {
        valid = false;
        continue;
      }
      selectedSite = sites.getById(siteId);
      if (selectedSite == null) {
        valid = false;
      }
    } while (!valid);

    Password password = getPassword();

    User user = UserManager.getInstance().getByEMail(address, selectedSite);
    try {
      UserManager.getInstance().changePassword(user, password);
    } catch (MailServerException | IOException | EMailNotExistException | UserNotFoundByHashException | NullHashException | HashTooOldException e) {
      Utils.consoleOutLn(e.getMessage());
    }
  }

  public void createSofiaAdministrator(String name, String lastName, String address, Password password) throws ClusterException {
    try (Connection connection = Database.getConnection()) {
      connection.setAutoCommit(false);
      Site managerSite = SiteManager.getInstance().getById(1);
      Person person = UserManager.getInstance().add(connection, name, lastName, 1);
      EMail eMail = EMailManager.getInstance().create(connection, person.getId(), address);
      PeopleManager.getInstance().setPrimaryEMail(connection, person, eMail);

      try {
        User user = UserManager.getInstance().set(connection, managerSite, address, password);
        ProfileManager.getInstance().create(connection, "administrator", managerSite);
        UserManager.getInstance().add(connection, user, Profile.ADMINISTRATOR, 1);
      } catch (EMailAddressNotExistException e) {
        connection.rollback();
        throw new SofiaRuntimeException(e);
      }

      Site playgroundSite = SiteManager.getInstance().getById(2);
      try {
        User user = UserManager.getInstance().set(connection, playgroundSite, address, password);
        ProfileManager.getInstance().create(connection, "administrator", playgroundSite);
        UserManager.getInstance().add(connection, user, Profile.ADMINISTRATOR, 1);
      } catch (EMailAddressNotExistException e) {
        connection.rollback();
        throw new SofiaRuntimeException(e);
      }
      connection.commit();
    } catch (SQLException e) {
      throw new ClusterException(e);
    }
  }

  void checkForDropDatabase(SofiaDatabaseCreator mainDefaultDataCreator, DatabaseCreators defaultDataCreators) throws IOException, DataCreationException {
    if (startOptions.hasDropDatabase()) {

      Path path = Configuration.getInstance().getClusterFileLogPath();
      Files.deleteIfExists(path);

      for (DataCreator defaultDataCreator : defaultDataCreators) {
        defaultDataCreator.dropDatabase();
      }
      mainDefaultDataCreator.dropDatabase();
    }
  }

  void createDatabases(SofiaDatabaseCreator mainDefaultDataCreator, DatabaseCreators defaultDataCreators) throws DataCreationException {
    if (!mainDefaultDataCreator.databaseExists()) {
      mainDefaultDataCreator.createDatabase();
      mainDefaultDataCreator.createDatabaseStructure();
      mainDefaultDataCreator.riseDatabaseCreatedFlag();
    }
    for (DataCreator defaultDataCreator : defaultDataCreators) {
      if (!defaultDataCreator.databaseExists()) {
        defaultDataCreator.createDatabase();
        defaultDataCreator.createDatabaseStructure();
        defaultDataCreator.riseDatabaseCreatedFlag();
      }
    }
  }

  void createDefaultData(SofiaDatabaseCreator mainDefaultDataCreator, DatabaseCreators defaultDataCreators)
          throws IOException, ClusterException, DataCreationException, ConfigurationException, DataConversionException {
    if (mainDefaultDataCreator.isDatabaseCreated()) {
      String baseDomainName = getDefaultDomainName();
      Logger.info("Create the default sites.");
      SiteManager.getInstance().create("Manager", Paths.get("manager"), "manager", "localhost", baseDomainName);
      SiteManager.getInstance().create("Playground", Paths.get("playground"), "playground");
      createAdministrator();
      mainDefaultDataCreator.createDefaultData();
    }

    for (DataCreator defaultDataCreator : defaultDataCreators) {
      if (defaultDataCreator.isDatabaseCreated()) {
        defaultDataCreator.createDefaultData();
      }
    }
  }

  void createTestData(SofiaDatabaseCreator mainDefaultDataCreator, DatabaseCreators defaultDataCreators) throws DataCreationException {
    if (startOptions.hasCreateTestData()) {
      if (mainDefaultDataCreator.isDatabaseCreated()) {
        Logger.info("Create test data for sofia.");
        mainDefaultDataCreator.createTestData();
      }
      for (DataCreator defaultDataCreator : defaultDataCreators) {
        if (defaultDataCreator.isDatabaseCreated()) {
          defaultDataCreator.createTestData();
        }
      }
    }
  }
}
