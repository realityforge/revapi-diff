package org.realityforge.revapi.diff;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import org.revapi.Report;
import org.revapi.simple.SimpleReporter;

public final class CollectorReporter
  extends SimpleReporter
{
  private final List<Report> _reports = new ArrayList<>();

  List<Report> getReports()
  {
    return _reports;
  }

  @Override
  public void report( @Nonnull Report report )
  {
    if ( !report.getDifferences().isEmpty() )
    {
      _reports.add( report );
    }
  }
}
