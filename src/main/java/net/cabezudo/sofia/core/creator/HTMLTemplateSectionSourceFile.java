package net.cabezudo.sofia.core.creator;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import net.cabezudo.sofia.core.sites.Site;

/**
 * @author <a href="http://cabezudo.net">Esteban Cabezudo</a>
 * @version 0.01.00, 2020.06.06
 */
public class HTMLTemplateSectionSourceFile extends HTMLTemplateSourceFile {

  public HTMLTemplateSectionSourceFile(Site site, Path templatesBasePath, Path templatePath, String id, TemplateVariables templateVariables, Caller caller)
          throws IOException, SiteCreationException, LocatedSiteCreationException, SQLException, InvalidFragmentTag {
    super(site, templatesBasePath, templatePath, id, templateVariables, caller);
  }
}