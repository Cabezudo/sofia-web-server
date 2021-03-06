package net.cabezudo.sofia.core.creator;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import net.cabezudo.json.JSON;
import net.cabezudo.json.exceptions.JSONParseException;
import net.cabezudo.json.values.JSONObject;
import net.cabezudo.sofia.core.Position;
import net.cabezudo.sofia.core.cluster.ClusterException;
import net.cabezudo.sofia.core.configuration.Configuration;
import net.cabezudo.sofia.core.exceptions.SofiaRuntimeException;
import net.cabezudo.sofia.core.files.FileHelper;
import net.cabezudo.sofia.core.html.HTMLTagFactory;
import net.cabezudo.sofia.core.html.Tag;
import net.cabezudo.sofia.core.sites.Site;
import net.cabezudo.sofia.core.users.profiles.Profiles;
import net.cabezudo.sofia.logger.Logger;

/**
 * @author <a href="http://cabezudo.net">Esteban Cabezudo</a>
 * @version 0.01.00, 2019.12.03
 */
abstract class HTMLSourceFile implements SofiaSource {

  private final Site site;
  private final Path basePath;
  private final Path partialPath;
  private final Path partialFilePath;
  private final TemplateVariables templateVariables;
  private final TextsFile textsFile;
  private final Caller caller;
  private final Lines lines;
  protected final CSSImports cssImports;
  protected final Libraries libraries;
  private final CSSSourceFile css;
  private final JSSourceFile js;
  private Profiles profiles = new Profiles();
  private final String id;
  private final String cssPartialFileName;

  HTMLSourceFile(Site site, Path basePath, Path partialPath, String id, TemplateVariables templateVariables, TextsFile textsFile, Caller caller)
          throws IOException, LocatedSiteCreationException, SiteCreationException, InvalidFragmentTag {
    this.site = site;
    this.basePath = basePath;
    this.partialFilePath = partialPath;
    this.partialPath = partialPath.getParent();
    this.templateVariables = templateVariables;
    this.textsFile = textsFile;
    this.caller = caller;
    this.lines = new HTMLLines();
    this.cssImports = new CSSImports();
    this.libraries = new Libraries();
    this.id = id;

    this.cssPartialFileName = getVoidPartialPathName() + ".css";
    Path cssPartialPath = Paths.get(cssPartialFileName);
    Path jsPartialPath = Paths.get(getVoidPartialPathName() + ".js");

    css = new CSSSourceFile(site, basePath, cssPartialPath, templateVariables, caller);
    js = new JSSourceFile(site, basePath, jsPartialPath, templateVariables, caller);
  }

  Site getSite() {
    return site;
  }

  Path getBasePath() {
    return basePath;
  }

  Path getPartialFilePath() {
    return partialFilePath;
  }

  Path getPartialPath() {
    return partialPath;
  }

  TemplateVariables getTemplateVariables() {
    return templateVariables;
  }

  TextsFile getTextsFile() {
    return textsFile;
  }

  Caller getCaller() {
    return caller;
  }

  abstract Path getSourceFilePath(Caller caller) throws SiteCreationException;

  void loadHTMLFile()
          throws IOException, LocatedSiteCreationException, InvalidFragmentTag, SiteCreationException, LibraryVersionConflictException, JSONParseException, ClusterException {

    if (caller == null) {
      Logger.debug("Load HTML %s requested.", partialFilePath);
    } else {
      Logger.debug("Load HTML %s called from %s.", partialFilePath, caller);
    }
    SofiaSource actual = this;

    // Search for texts file using the name of the page
    String partialTextsFilePathName = FileHelper.removeExtension(partialFilePath) + ".texts.json";
    Path textsFilePath = getBasePath().resolve(partialTextsFilePathName);
    Logger.debug("Search for texts file %s.", textsFilePath);
    if (Files.exists(textsFilePath)) {
      JSONObject jsonTexts = JSON.parse(textsFilePath, Configuration.getDefaultCharset()).toJSONObject();
      Logger.debug("Texts file FOUND.");
      textsFile.add(jsonTexts);
    } else {
      Logger.debug("Texts file NOT FOUND.");
    }

    // Search for a configuration file using the name of the page
    JSONConfiguration jsonSourceConfiguration = new JSONConfiguration();
    JSONObject jsonConfiguration = jsonSourceConfiguration.load(this);
    Path jsonPartialPath = jsonSourceConfiguration.getJSONPartialPath();

    String templateReference = null;
    String pageReference = null;
    if (jsonConfiguration != null) {
      templateReference = jsonConfiguration.getNullString("template");
      jsonConfiguration.remove("template");

      pageReference = jsonConfiguration.getNullString("page");
      jsonConfiguration.remove("page");

      if (templateReference != null && pageReference != null) {
        throw new SiteCreationException("You can't set a template and a page at the same time.");
      }
//      getTemplateVariables().merge(jsonConfiguration);
    }

    Logger.debug("Search in %s for a reference to another file.", jsonPartialPath);
    do {
      // Search for template property and if exist read the template file
      if (templateReference != null) {
        Logger.debug("The configuration file has a template property. Load template %s.", templateReference);
        Path commonsHTMLTemplatePath = Configuration.getInstance().getCommonsHTMLTemplatesPath();
        Path voidTemplatePath = Paths.get(templateReference);

        Logger.debug("Load template %s from file %s in HTML source file.", voidTemplatePath, jsonPartialPath);

        Caller templateCaller = new Caller(getBasePath(), getPartialFilePath(), 0, caller);
        HTMLSourceFile templateFile = new JSONTemplateHTMLSourceFile(getSite(), commonsHTMLTemplatePath, voidTemplatePath, getTemplateVariables(), textsFile, templateCaller);
        templateFile.loadHTMLFile();
        profiles.add(templateFile.getProfiles());
        libraries.add(templateFile.getLibraries());
        css.add(templateFile.getCascadingStyleSheetImports());
        css.add(templateFile.getCascadingStyleSheet().getLines());
        js.add(templateFile.getJavaScript().getLines());
        this.lines.add(templateFile.getLines());
        break;
      }

      // Search for page property and if exist read the page file
      if (pageReference != null) {
        Logger.debug("The configuration file has a page property. Load page %s.", pageReference);
        Path commonsHTMLTemplatesPath = Configuration.getInstance().getCommonsHTMLTemplatesPath();
        Path voidPagePath = Paths.get(pageReference);

        Logger.debug("Load page %s from file %s in HTML source file.", voidPagePath, jsonPartialPath);

        Caller pageCaller = new Caller(getBasePath(), getPartialFilePath(), 0, caller);
        HTMLSourceFile pageSourceFile = new HTMLPageSourceFile(getSite(), commonsHTMLTemplatesPath, voidPagePath, getTemplateVariables(), textsFile, pageCaller);
        pageSourceFile.loadHTMLFile();
        pageSourceFile.loadCSSFile();
        pageSourceFile.loadJSFile();
        profiles.add(pageSourceFile.getProfiles());
        libraries.add(pageSourceFile.getLibraries());
        css.add(pageSourceFile.getCascadingStyleSheetImports());
        css.add(pageSourceFile.getCascadingStyleSheet().getLines());
        js.add(pageSourceFile.getJavaScript().getLines());
        this.lines.add(pageSourceFile.getLines());
        break;
      }
      Logger.debug("No references to templates or pages found in configuration file %s. An attempt is made to read the html file %s.", jsonPartialPath, partialFilePath);

      final Path htmlSourceFilePath = getSourceFilePath(caller);

      Logger.debug("Full path to HTML file to load %s.", htmlSourceFilePath);
      List<String> linesFromFile;
      try {
        linesFromFile = Files.readAllLines(htmlSourceFilePath);
      } catch (NoSuchFileException e) {
        throw new NoSuchFileException("No such file: " + htmlSourceFilePath);
      }

      int lineNumber = 1;
      for (String line : linesFromFile) {
        String newLine = replaceTemplateVariables(line, lineNumber, htmlSourceFilePath);
        String trimmedNewLine = newLine.trim();

        do {
          if (searchHTMLTag(actual, trimmedNewLine, htmlSourceFilePath, lineNumber)) {
            actual = this;
            break;
          }

          // Search for HTML tags with JavaScript libraries references
          if (trimmedNewLine.startsWith("<script lib=\"") && trimmedNewLine.endsWith("\"></script>")) {
            String libraryReference = trimmedNewLine.substring(13, trimmedNewLine.length() - 11);
            Logger.debug("Library reference name found: %s.", libraryReference);

            Caller newCaller = new Caller(getBasePath(), getPartialFilePath(), lineNumber, getCaller());
            Library library = new Library(getSite(), libraryReference, getTemplateVariables(), newCaller);
            libraries.add(library);
            break;
          }
          if (trimmedNewLine.startsWith("<script file=\"") && trimmedNewLine.endsWith("\"></script>")) {
            String fileReference = trimmedNewLine.substring(14, trimmedNewLine.length() - 11);
            Logger.debug("File reference name found: %s.", fileReference);
            Caller newCaller = new Caller(getBasePath(), getPartialFilePath(), lineNumber, getCaller());
            js.load(null, fileReference, newCaller);
            break;
          }

          // Search for HTML tags with CSS file references
          if (trimmedNewLine.startsWith("<style file=\"") && trimmedNewLine.endsWith("\"></style>")) {
            String styleFilePartialFileName = trimmedNewLine.substring(13, trimmedNewLine.length() - 10);
            Logger.debug("Found independent style file call: %s.", styleFilePartialFileName);
            Caller newCaller = new Caller(getBasePath(), getPartialFilePath(), lineNumber, getCaller());
            css.load(null, styleFilePartialFileName, newCaller);
            break;
          }

          switch (trimmedNewLine) {
            // Search for embeded CSS code
            case "<style>":
              actual = css;
              if (getCaller() == null) {
                actual.add(new CodeLine("/* created by system using content from " + getPartialFilePath() + ":" + lineNumber + " */", lineNumber));
              } else {
                CodeLine codeLine = new CodeLine("/* created by system using " + getPartialFilePath() + ":" + lineNumber + " called from " + getCaller() + " */", lineNumber);
                actual.add(codeLine);
              }
              break;
            case "<script>":
            case "<script class=\"test\">":
              actual = js;
              if (getCaller() == null) {
                actual.add(new CodeLine("// created by system using " + getPartialFilePath() + ":" + lineNumber + ".", lineNumber));
              } else {
                actual.add(new CodeLine("// created by system using " + getPartialFilePath() + ":" + lineNumber + " called from " + getCaller(), lineNumber));
              }
              break;
            case "</html>":
              if (getCaller() == null) {
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
                Line processedLine = getProcessedLine(htmlSourceFilePath, newLine, lineNumber);
                if (processedLine != null) {
                  actual.add(processedLine);
                  libraries.add(processedLine.getLibraries());
                }
                break;
              }
              CodeLine codeLine = new CodeLine(newLine, lineNumber);
              actual.add(codeLine);
              break;
          }
        } while (false);
        lineNumber++;
      }
    } while (false);
  }
//

  void loadCSSFile() throws IOException, LocatedSiteCreationException {
    css.load(basePath, cssPartialFileName, caller);
  }

  void loadCSSFile(Path basePath, String cssPartialFileName, Caller newCaller) throws IOException, LocatedSiteCreationException {
    css.load(basePath, cssPartialFileName, newCaller);
  }

  void loadJSFile() throws IOException, LocatedSiteCreationException {
    js.loadFile();
  }

  abstract String replaceTemplateVariables(String line, int lineNumber, Path htmlSourceFilePath) throws LocatedSiteCreationException;

  Line getProcessedLine(Path htmlSourceFilePath, String line, int lineNumber)
          throws IOException, SiteCreationException, LocatedSiteCreationException, InvalidFragmentTag, LibraryVersionConflictException, JSONParseException, ClusterException {
    Tag tag = HTMLTagFactory.get(line);
    Path actualPath = htmlSourceFilePath.getParent();

    // If the tag is a section we search for a file or template in order to load the file
    if (tag != null && tag.isSection()) {
      if (tag.getValue("file") != null) {
        Caller newCaller = new Caller(this, lineNumber);
        HTMLFragmentFileLine fragmentLine = new HTMLFragmentFileLine(getSite(), getBasePath(), getPartialFilePath(), getTemplateVariables(), textsFile, tag, lineNumber, newCaller);
        fragmentLine.load();
        // TODO Add custom configuration for a file.
        return fragmentLine;
      }
      if (tag.getValue("template") != null) {
        String tagId = tag.getId();
        if (tagId == null) {
          throw new LocatedSiteCreationException("A template call must have an id", getPartialFilePath(), new Position(lineNumber, 0));
        }
        Path templateBasePath;
        String configurationFile;

        String configurationFileAttribute = tag.getValue("configurationFile");
        if (configurationFileAttribute == null) {
          if (getPartialPath() == null) {
            configurationFile = tagId + ".json";
          } else {
            configurationFile = getPartialPath().resolve(tagId + ".json").toString();
          }
        } else {
          configurationFile = configurationFileAttribute;
        }
        if (configurationFile.startsWith("/")) {
          configurationFile = configurationFile.substring(1);
          templateBasePath = site.getVersionedSourcesPath();
        } else {
          templateBasePath = actualPath;
        }
        Logger.info("Load configuration file %s used for template with id %s.", configurationFile, tagId);
        try {
          getTemplateVariables().add(templateBasePath, configurationFile, tagId);
        } catch (FileNotFoundException | JSONParseException | UndefinedLiteralException e) {
          throw new SiteCreationException(e.getMessage());
        }

        Caller newCaller = new Caller(this, lineNumber);
        HTMLTemplateLine templateLine = new HTMLTemplateLine(getSite(), getBasePath(), getPartialFilePath(), getTemplateVariables(), textsFile, tag, lineNumber, newCaller);
        templateLine.load();

        return templateLine;
      }
    }

    return new CodeLine(line, lineNumber);
  }

  @Override
  public abstract boolean searchHTMLTag(SofiaSource actual, String line, Path filePath, int lineNumber) throws ClusterException, InvalidFragmentTag;

  @Override
  public Lines getLines() {
    return lines;
  }

  String getCode() {
    return lines.getCode();
  }

  void save(Path filePath) throws IOException {
    Logger.debug("Creating the html file %s.", filePath);
    StringBuilder code = new StringBuilder();
    for (Line line : lines) {
      String lineCode = line.getCode();
      code.append(lineCode).append('\n');
    }

    Files.write(filePath, code.toString().getBytes(Configuration.getInstance().getEncoding()));
  }

  Libraries getLibraries() {
    return libraries;
  }

  @Override
  public void add(CSSImport cssImport) {
    if (cssImport == null) {
      throw new SofiaRuntimeException("Null parameter");
    }
    lines.add(cssImport);
  }

  @Override
  public void add(CSSImports newCSSImports) {
    for (CSSImport cssImport : newCSSImports) {
      cssImports.add(cssImport);
    }
  }

  @Override
  public void add(Line line) {
    if (line == null) {
      return;
    }
    lines.add(line);
  }

  @Override
  public void add(Lines ls) {
    lines.add(ls);
  }

  @Override
  public final String getVoidPartialPathName() {
    String partialPathName = getPartialFilePath().toString();
    return FileHelper.removeExtension(partialPathName);
  }

  void setProfiles(Profiles profiles) {
    this.profiles = profiles;
  }

  Profiles getProfiles() {
    return profiles;
  }

  private JSSourceFile getJavaScript() {
    return js;
  }

  @Override
  public Lines getJavaScriptLines() {
    Lines codeLines = new JSLines();
    codeLines.add(js.getJavaScriptLines());
    for (Line line : getLines()) {
      codeLines.add(line.getJavaScriptLines());
    }
    return codeLines;
  }

  @Override
  public CSSImports getCascadingStyleSheetImports() {
    CSSImports imports = new CSSImports();
    imports.add(css.getCascadingStyleSheetImports());
    for (Line line : getLines()) {
      imports.add(line.getCascadingStyleSheetImports());
    }
    return imports;
  }

  private CSSSourceFile getCascadingStyleSheet() {
    return css;
  }

  @Override
  public Lines getCascadingStyleSheetLines() {
    Lines codeLines = new CSSLines();
    codeLines.add(css.getCascadingStyleSheetLines());
    for (Line line : getLines()) {
      Lines cssLines = line.getCascadingStyleSheetLines();
      codeLines.add(cssLines);
    }
    return codeLines;
  }

  String getId() {
    return id;
  }
}
