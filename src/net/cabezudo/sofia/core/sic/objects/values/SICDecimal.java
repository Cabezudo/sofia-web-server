package net.cabezudo.sofia.core.sic.objects.values;

import java.math.BigDecimal;
import net.cabezudo.sofia.core.sic.tokens.Token;

/**
 * @author <a href="http://cabezudo.net">Esteban Cabezudo</a>
 * @version 0.01.00, 2020.06.14
 */
public class SICDecimal extends SICNumber<BigDecimal> {

  public SICDecimal(Token token, BigDecimal bd) {
    super(token, bd);
  }

  @Override
  public String getTypeName() {
    return "number";
  }

  @Override
  public String toString() {
    return getValue() + "%";
  }

  @Override
  public boolean isDecimal() {
    return true;
  }

  @Override
  public boolean isZero() {
    return getValue().intValue() == 0;
  }
}
