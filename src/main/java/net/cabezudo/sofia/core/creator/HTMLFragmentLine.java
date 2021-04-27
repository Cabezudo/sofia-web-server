package net.cabezudo.sofia.core.creator;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import net.cabezudo.json.exceptions.JSONParseException;
import net.cabezudo.sofia.core.configuration.Configuration;
import net.cabezudo.sofia.core.html.Tag;
import net.cabezudo.sofia.core.sites.Site;
import net.cabezudo.sofia.logger.Logger;

/**
 * @author <a href="http://cabezudo.net">Esteban Cabezudo</a>
 * @version 0.01.00, 2020.06.08
 */
public class HTMLFragmentLine extends HTMLFileLine {

  public HTMLFragmentLine(Site site, Path basePath, Path parentPath, TemplateVariables templateVariables, TextsFile textsFile, Tag tag, int lineNumber, Caller caller)
          throws IOException, SiteCreationException, LocatedSiteCreationException, InvalidFragmentTag, LibraryVersionConflictException, JSONParseException {
    super(site, basePath, parentPath, templateVariables, textsFile, tag, lineNumber, caller);
  }

  @Override
  Path getConfigurationFilePath(Caller caller) {
    Path templatesBasePath = Configuration.getInstance().getCommonsComponentsTemplatesPath();
    String fileName = getFilePath().toString();
    int i = fileName.lastIndexOf(".");
    String partialFile = fileName.substring(0, i);
    return templatesBasePath.resolve(partialFile + ".json");
  }

  @Override
  Path getTextsFilePath() {
    String fileName = getFilePath().toString();
    String textsFileName = fileName.replace(".html", ".texts.json");
    return getBasePath().resolve(textsFileName);
  }

  @Override
  Path getFilePath() {
    String fileName = getTag().getValue("file");
    return Paths.get(fileName);
  }

  @Override
  HTMLSourceFile getHTMLSourceFile(Caller caller)
          throws IOException, SiteCreationException, LocatedSiteCreationException, InvalidFragmentTag, LibraryVersionConflictException, JSONParseException {
    Logger.debug("[HTMLFragmentLine:getHTMLSourceFile]");
    Path fullFileBasePath;
    String partialFilePathString = getFilePath().toString();
    // FIX This fail with relative paths like file="this.html". Take the root site path
    if (partialFilePathString.startsWith("/") || caller == null) {
      fullFileBasePath = getSite().getVersionedSourcesPath().resolve(partialFilePathString.substring(1));
    } else {
      fullFileBasePath = caller.getBasePath().resolve(partialFilePathString);
    }
    return new HTMLFragmentSourceFile(getSite(), fullFileBasePath, getFilePath(), null, getTemplateVariables(), getTextsFile(), caller);
  }
}
