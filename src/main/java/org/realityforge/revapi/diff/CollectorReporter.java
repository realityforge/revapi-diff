package org.realityforge.revapi.diff;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.revapi.Report;
import org.revapi.simple.SimpleReporter;

public final class CollectorReporter
  extends SimpleReporter
{
  private final List<Report> _reports = new ArrayList<>();

  @Nonnull
  List<Report> getReports()
  {
    // Ensure a stable ordering
    return _reports
      .stream()
      .sorted( Comparator.comparing( r -> r.getNewElement() + "-" + r.getOldElement() ) )
      .collect( Collectors.toList() );
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
