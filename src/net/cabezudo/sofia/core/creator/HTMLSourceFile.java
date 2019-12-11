package net.cabezudo.sofia.core.creator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.List;
import net.cabezudo.json.JSON;
import net.cabezudo.json.exceptions.JSONParseException;
import net.cabezudo.json.values.JSONObject;
import net.cabezudo.sofia.core.configuration.Configuration;
import net.cabezudo.sofia.core.html.HTMLTagFactory;
import net.cabezudo.sofia.core.html.Tag;
import net.cabezudo.sofia.core.logger.Logger;
import net.cabezudo.sofia.core.sites.Site;
import net.cabezudo.sofia.core.users.profiles.ProfileManager;
import net.cabezudo.sofia.core.users.profiles.Profiles;

/**
 * @author <a href="http://cabezudo.net">Esteban Cabezudo</a>
 * @version 0.01.00, 2019.12.03
 */
class HTMLSourceFile extends SofiaSourceFile {

  private final Lines lines;
  protected final Libraries libraries;
  private final CSSSourceFile css;
  private final JSSourceFile js;
  private Profiles profiles = new Profiles();

  HTMLSourceFile(Site site, Path basePath, Path partialPath, TemplateVariables templateVariables, Caller caller) throws IOException, LocatedSiteCreationException, SiteCreationException, SQLException, InvalidFragmentTag {
    super(site, basePath, partialPath, templateVariables, caller);
    this.lines = new Lines();
    this.libraries = new Libraries();

    Path cssPartialPath = Paths.get(getVoidPartialPathName() + ".css");
    Path jsPartialPath = Paths.get(getVoidPartialPathName() + ".js");
    css = new CSSSourceFile(site, basePath, cssPartialPath, templateVariables, caller);
    js = new JSSourceFile(site, basePath, jsPartialPath, templateVariables, caller);
  }

  void loadJSONConfigurationFile() throws IOException, LocatedSiteCreationException, SiteCreationException, SQLException, InvalidFragmentTag, LibraryVersionConflictException {
    Path jsonPartialPath = Paths.get(getVoidPartialPathName() + ".json");
    Path jsonSourceFilePath = getBasePath().resolve(jsonPartialPath);
    if (Files.isRegularFile(jsonSourceFilePath)) {
      Logger.debug("FOUND configuration file %s for HTML %s.", jsonPartialPath, getPartialPath());
      List<String> jsonLines = Files.readAllLines(jsonSourceFilePath);
      StringBuilder sb = new StringBuilder();
      int lineNumber = 1;
      for (String line : jsonLines) {
        try {
          sb.append(getTemplateVariables().replace(line, lineNumber, jsonSourceFilePath));
        } catch (UndefinedLiteralException e) {
          Position position = new Position(lineNumber, e.getRow());
          throw new LocatedSiteCreationException(e.getMessage(), jsonPartialPath, position);
        }
        lineNumber++;
      }
      JSONObject jsonObject;
      try {
        jsonObject = JSON.parse(sb.toString()).toJSONObject();
      } catch (JSONParseException e) {
        throw new SiteCreationException("Cant parse " + jsonSourceFilePath + ". " + e.getMessage());
      }
      String templateName = jsonObject.getNullString("template");
      if (templateName != null) {
        // Read template from template files
        Path commonsComponentsTemplatePath = Configuration.getInstance().getCommonsComponentsTemplatesPath();
        Path templatePath = Paths.get(templateName);

        Logger.debug("Load template %s from file %s.", templatePath, jsonPartialPath);
        jsonObject.remove("template");
        HTMLSourceFile templateFile = new HTMLSourceFile(getSite(), commonsComponentsTemplatePath, templatePath, getTemplateVariables(), null);
        templateFile.loadJSONConfigurationFile();
        templateFile.loadHTMLFile();
        libraries.add(templateFile.getLibraries());
        this.lines.add(templateFile.getLines());
      }
      getTemplateVariables().merge(jsonObject);
    }
  }

  void loadHTMLFile() throws IOException, LocatedSiteCreationException, SQLException, InvalidFragmentTag, SiteCreationException, LibraryVersionConflictException {
    SofiaSourceFile actual = this;

    Path htmlSourceFilePath = getBasePath().resolve(getPartialPath());
    Logger.debug("Load HTML source file %s.", getPartialPath());
    List<String> linesFromFile = Files.readAllLines(htmlSourceFilePath);
    int lineNumber = 1;
    for (String l : linesFromFile) {
      try {
        String newLine = getTemplateVariables().replace(l, lineNumber, htmlSourceFilePath);
        String trimmedNewLine = newLine.trim();

        do {
          actual = searchHTMLTag(actual, trimmedNewLine, lineNumber);
          if (trimmedNewLine.startsWith("<script lib=\"") && trimmedNewLine.endsWith("\"></script>")) {
            String libraryReference = trimmedNewLine.substring(13, trimmedNewLine.length() - 11);
            Logger.debug("Library reference name found: %s.", libraryReference);

            Caller newCaller = new Caller(getBasePath(), getPartialPath(), lineNumber, getCaller());
            Library library = new Library(getSite(), libraryReference, getTemplateVariables(), newCaller);
            libraries.add(library);
            break;
          }

          switch (trimmedNewLine) {
            case "<style>":
              actual = css;
              if (getCaller() == null) {
                actual.add(new CodeLine("/* created by system using content from " + getPartialPath() + ":" + lineNumber + " */", lineNumber));
              } else {
                actual.add(new CodeLine("/* created by system using " + getPartialPath() + ":" + lineNumber + " called from " + getCaller() + " */", lineNumber));
              }
              break;
            case "<script>":
              actual = js;
              if (getCaller() == null) {
                actual.add(new CodeLine("// created by system using " + getPartialPath() + ":" + lineNumber + ".", lineNumber));
              } else {
                actual.add(new CodeLine("// created by system using " + getPartialPath() + ":" + lineNumber + " called from " + getCaller(), lineNumber));
              }
              break;
            case "</html>":
              if (getCaller() != null) {
                actual.add(new CodeLine("</html>\n", lineNumber));
              }
              actual = this;
              break;
            case "</style>":
            case "</script>":
              actual = this;
              break;
            default:
              if (actual == this) {
                Line processedLine = getProcessedLine(newLine, lineNumber);
                if (processedLine != null) {
                  actual.add(processedLine);
                  libraries.add(processedLine.getLibraries());
                }
                break;
              }
              actual.add(new CodeLine(newLine, lineNumber));
              break;
          }
        } while (false);
      } catch (UndefinedLiteralException e) {
        Position position = new Position(lineNumber, e.getRow());
        throw new LocatedSiteCreationException(e.getMessage(), getPartialPath(), position);
      }
      lineNumber++;
    }
  }

  private Line getProcessedLine(String line, int lineNumber) throws IOException, SiteCreationException, LocatedSiteCreationException, SQLException, InvalidFragmentTag, LibraryVersionConflictException {
    Tag tag = HTMLTagFactory.get(line);
    if (tag != null && tag.isSection()) {
      if (tag.getValue("file") != null) {
        HTMLFragmentLine fragmentLine = new HTMLFragmentLine(getSite(), getBasePath(), getPartialPath(), getTemplateVariables(), tag, lineNumber, getCaller());
        return fragmentLine;
      }
      if (tag.getValue("template") != null) {
        if (tag.getId() == null) {
          throw new LocatedSiteCreationException("A template call must have an id", getPartialPath(), new Position(lineNumber, 0));
        }
        HTMLTemplateLine fragmentLine = new HTMLTemplateLine(getSite(), getBasePath(), getPartialPath(), getTemplateVariables(), tag, lineNumber, getCaller());
        return fragmentLine;
      }
    }

    return new CodeLine(line, lineNumber);
  }

  protected SofiaSourceFile searchHTMLTag(SofiaSourceFile actual, String line, int lineNumber) throws SQLException, InvalidFragmentTag {
    if (line.startsWith("<html")) {
      if (getCaller() != null) {
        throw new InvalidFragmentTag("A HTML fragment can't have the <html> tag", 0);
      }
      int i = line.indexOf("profiles");
      if (i > 0) {
        String profileString = line.substring(i + 10, line.length() - 2);
        Logger.debug("Profiles: " + profileString);
        String[] ps = profileString.split(",");
        profiles = ProfileManager.getInstance().createFromNames(ps, getSite());
      }
      add(new CodeLine("<html>\n", lineNumber));
      return this;
    }
    return actual;
  }

  Lines getLines() {
    return lines;
  }

  String toHTML() {
    return lines.getCode();
  }

  void save(Path filePath) throws IOException {
    Logger.debug("Creating the html file %s.", filePath);
    StringBuilder code = new StringBuilder();
    for (Line line : lines) {
      code.append(line.getCode()).append('\n');
    }
    Files.write(filePath, code.toString().getBytes(Configuration.getInstance().getEncoding()));
  }

  Libraries getLibraries() {
    return libraries;
  }

  @Override
  public void add(Line line) {
    if (line == null) {
      return;
    }
    lines.add(line);
  }

  @Override
  public void add(Lines lines) {
    lines.add(lines);
  }

  @Override
  public final String getVoidPartialPathName() {
    String partialPathName = getPartialPath().toString();
    return partialPathName.substring(0, partialPathName.length() - 5);
  }

  Profiles getProfiles() {
    return profiles;
  }

  Lines getJavaScriptLines() {
    Lines codeLines = new Lines();
    codeLines.add(js.getJavaScriptLines());
    for (Line line : this.lines) {
      codeLines.add(line.getJavaScriptLines());
    }
    return codeLines;
  }

  Lines getCascadingStyleSheetLines() {
    Lines codeLines = new Lines();
    codeLines.add(css.getCascadingStyleSheetLines());
    for (Line line : this.lines) {
      codeLines.add(line.getCascadingStyleSheetLines());
    }
    return codeLines;
  }
}
