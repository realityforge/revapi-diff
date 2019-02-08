package org.realityforge.revapi.diff;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.revapi.AnalysisContext;
import org.revapi.Report;
import org.revapi.Reporter;

public final class CollectorReporter
  implements Reporter
{
  private final List<Report> _reports = new ArrayList<>();

  List<Report> getReports()
  {
    return _reports;
  }

  @Nullable
  @Override
  public String getExtensionId()
  {
    return null;
  }

  @Nullable
  @Override
  public Reader getJSONSchema()
  {
    return null;
  }

  @Override
  public void initialize( @Nonnull AnalysisContext properties )
  {
  }

  @Override
  public void report( @Nonnull Report report )
  {
    if ( !report.getDifferences().isEmpty() )
    {
      _reports.add( report );
    }
  }

  @Override
  public void close()
  {
  }
}
