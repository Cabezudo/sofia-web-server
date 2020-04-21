package net.cabezudo.sofia.core.creator;

import java.sql.SQLException;

/**
 * @author <a href="http://cabezudo.net">Esteban Cabezudo</a>
 * @version 0.01.00, 2019.12.18
 */
interface SofiaSource {

  void add(CSSImport cssImport);

  void add(CSSImports cssImports);

  void add(Line line);

  void add(Lines lines);

  Lines getLines();

  Lines getJavaScriptLines();

  CSSImports getCascadingStyleSheetImports();

  Lines getCascadingStyleSheetLines();

  String getVoidPartialPathName();

  boolean searchHTMLTag(SofiaSource actual, String line, int lineNumber) throws SQLException, InvalidFragmentTag;

}
