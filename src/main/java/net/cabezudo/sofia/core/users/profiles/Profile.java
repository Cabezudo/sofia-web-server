package net.cabezudo.sofia.core.users.profiles;

import net.cabezudo.sofia.core.cluster.ClusterException;
import net.cabezudo.sofia.core.sites.Site;
import net.cabezudo.sofia.core.sites.SiteManager;
import net.cabezudo.sofia.core.users.User;

/**
 * @author <a href="http://cabezudo.net">Esteban Cabezudo</a>
 * @version 0.01.00, 2019.05.16
 */
public class Profile {

  public static final Profile ADMINISTRATOR = new Profile(1, "administrator", 1);

  private final int id;
  private final String name;
  private final int siteId;
  private Site site;

  public Profile(int id, String name, int siteId) {
    this.id = id;
    this.name = name;
    this.siteId = siteId;
  }

  public Profile(int id, String name, Site site) {
    this.id = id;
    this.name = name;
    this.site = site;
    this.siteId = site.getId();
  }

  public int getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public int getSiteId() {
    return siteId;
  }

  public Site getSite(User owner) throws ClusterException {
    if (site == null) {
      site = SiteManager.getInstance().getById(siteId);
    }
    return site;
  }

  @Override
  public String toString() {
    return "[ id: " + id + ", name: " + name + ", site: " + site + " ]";
  }
}
