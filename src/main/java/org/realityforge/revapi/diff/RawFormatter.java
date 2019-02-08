package org.realityforge.revapi.diff;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

final class RawFormatter
  extends Formatter
{
  @Override
  public String format( final LogRecord logRecord )
  {
    return logRecord.getMessage() + "\n";
  }
}
