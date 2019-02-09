package org.realityforge.revapi.diff;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.revapi.Archive;

final class LabeledFileArchive
  implements Archive
{
  @Nonnull
  private final String _name;
  @Nonnull
  private final File _file;

  LabeledFileArchive( @Nonnull final String name, @Nonnull final File file )
  {
    _name = Objects.requireNonNull( name );
    _file = Objects.requireNonNull( file );
  }

  @Nonnull
  @Override
  public String getName()
  {
    return _name;
  }

  @Nonnull
  @Override
  public InputStream openStream()
    throws IOException
  {
    return new FileInputStream( _file );
  }

  @Nonnull
  File getFile()
  {
    return _file;
  }
}
