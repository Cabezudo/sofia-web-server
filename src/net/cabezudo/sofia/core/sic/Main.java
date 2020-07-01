package net.cabezudo.sofia.core.sic;

import net.cabezudo.json.exceptions.JSONParseException;
import net.cabezudo.json.exceptions.PropertyNotExistException;
import net.cabezudo.sofia.core.sic.exceptions.EmptyQueueException;
import net.cabezudo.sofia.core.sic.tokens.Token;

/**
 * @author <a href="http://cabezudo.net">Esteban Cabezudo</a>
 * @version 0.01.00, 2020.06.13
 */
public class Main {

  public static void main(String... args) throws EmptyQueueException, JSONParseException, PropertyNotExistException {
//    String code = "main(loadImage(name=/home/esteban/NetBeansProjects/sofia.cabezudo.net/system/sources/sites/manager/1/images/test.jpg),resize(width=300,height=300))";
    String code = "    main(    loadImage(   name=/home/esteban/NetBeansProjects/sofia.cabezudo.net/system/sources/sites/manager/1/images/test.jpg   )  ,  resize( scale   =   0.5   )   ,   resize(    width   =   1200   , height  =  800  )  )";
    //  String code = "\nmain(\nloadImage(name=/home/esteban/NetBeansProjects/sofia.cabezudo.net/system/sources/sites/manager/1/images/test.jpg),resize(scale=.2),resize(\nwidth=1200, height=800))";
    //JSONObject json = JSON.parse("{\"code\":\"resize(\\n  width=1200,\\n  height=800,\\n  resize(\\n    width=200,\\n    height=100\\n  )\\n)\\n\"}").toJSONObject();
//    String code = json.getString("code");
//    String code = "main(loadImage(name=/home/esteban/NetBeansProjects/sofia.cabezudo.net/system/sources/sites/manager/1/images/test.jpg),resize(height=300))";

    SofiaImageCode sofiaImageCode = new SofiaImageCode(code, true);
    Tokens tokens = sofiaImageCode.getTokens();
    for (Token token : tokens) {
      System.out.print(token.getValue());
    }
    System.out.println();
//    System.out.println(sofiaImageCode.getTokens().toJSON());

//    System.out.println(sofiaImageCode.getShortCode());
    sofiaImageCode.compile();
//    System.out.println(sofiaImageCode.getFormatedCode());

    SICCompilerMessages sicCompilerMessages = sofiaImageCode.getCompilerMessages();
//    System.out.println(sicCompilerMessages.toJSON());
  }
}
