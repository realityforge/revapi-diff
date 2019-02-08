package org.realityforge.revapi.diff;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import org.realityforge.getopt4j.CLArgsParser;
import org.realityforge.getopt4j.CLOption;
import org.realityforge.getopt4j.CLOptionDescriptor;
import org.realityforge.getopt4j.CLUtil;
import org.revapi.API;
import org.revapi.AnalysisContext;
import org.revapi.AnalysisResult;
import org.revapi.Reporter;
import org.revapi.Revapi;
import org.revapi.simple.FileArchive;

/**
 * The entry point in which to run the tool.
 */
public class Main
{
  private static final int HELP_OPT = 'h';
  private static final int QUIET_OPT = 'q';
  private static final int VERBOSE_OPT = 'v';
  private static final int CONFIG_OPT = 2;
  private static final int OLD_API_OPT = 3;
  private static final int OLD_API_SUPPORT_OPT = 4;
  private static final int NEW_API_OPT = 5;
  private static final int NEW_API_SUPPORT_OPT = 6;

  private static final CLOptionDescriptor[] OPTIONS = new CLOptionDescriptor[]{
    new CLOptionDescriptor( "help",
                            CLOptionDescriptor.ARGUMENT_DISALLOWED,
                            HELP_OPT,
                            "print this message and exit" ),
    new CLOptionDescriptor( "quiet",
                            CLOptionDescriptor.ARGUMENT_DISALLOWED,
                            QUIET_OPT,
                            "Do not output unless an error occurs, just return 0 on no difference.",
                            new int[]{ VERBOSE_OPT } ),
    new CLOptionDescriptor( "verbose",
                            CLOptionDescriptor.ARGUMENT_DISALLOWED,
                            VERBOSE_OPT,
                            "Verbose output of differences.",
                            new int[]{ QUIET_OPT } ),

    new CLOptionDescriptor( "config-file",
                            CLOptionDescriptor.ARGUMENT_REQUIRED,
                            CONFIG_OPT,
                            "The json config file passed to Revapi." ),
    new CLOptionDescriptor( "old-api",
                            CLOptionDescriptor.ARGUMENT_REQUIRED | CLOptionDescriptor.DUPLICATES_ALLOWED,
                            OLD_API_OPT,
                            "Specify the path to a jar to compare against. May be specified multiple times." ),
    new CLOptionDescriptor( "old-api-support",
                            CLOptionDescriptor.ARGUMENT_REQUIRED | CLOptionDescriptor.DUPLICATES_ALLOWED,
                            OLD_API_SUPPORT_OPT,
                            "Specify the path to a jar to referenced by the old-api but not part of the analysis. May be specified multiple times." ),
    new CLOptionDescriptor( "new-api",
                            CLOptionDescriptor.ARGUMENT_REQUIRED | CLOptionDescriptor.DUPLICATES_ALLOWED,
                            NEW_API_OPT,
                            "Specify the path to a jar compared by the tool. May be specified multiple times." ),
    new CLOptionDescriptor( "new-api-support",
                            CLOptionDescriptor.ARGUMENT_REQUIRED | CLOptionDescriptor.DUPLICATES_ALLOWED,
                            NEW_API_SUPPORT_OPT,
                            "Specify the path to a jar to referenced by the new-api but not part of the analysis. May be specified multiple times." )
  };

  private static final int NO_DIFFERENCE_EXIT_CODE = 0;
  private static final int DIFFERENCE_EXIT_CODE = 1;
  private static final int ERROR_PARSING_ARGS_EXIT_CODE = 2;
  private static final int ERROR_OTHER_EXIT_CODE = 3;
  private static final String DEFAULT_CONFIG =
    "[\n" +
    "  {\n" +
    "    \"extension\": \"revapi.java\",\n" +
    "    \"configuration\": {\n" +
    "      \"missing-classes\": {\n" +
    "        \"behavior\": \"ignore\",\n" +
    "        \"ignoreMissingAnnotations\": true\n" +
    "      },\n" +
    "      \"reportUsesFor\": \"all-differences\"\n" +
    "    }\n" +
    "  },\n" +
    "  {\n" +
    "    \"extension\": \"revapi.ignore\",\n" +
    "    \"configuration\": [\n" +
    "      {\n" +
    "        \"code\": \"java.annotation.attributeValueChanged\",\n" +
    "        \"annotationType\": \"scala.reflect.ScalaSignature\"\n" +
    "      }\n" +
    "    ]\n" +
    "  }\n" +
    "]";

  private static final Logger c_logger = Logger.getGlobal();
  private static Revapi c_revapi;
  private static AnalysisContext.Builder c_builder;
  private static API.Builder c_oldAPI;
  private static API.Builder c_newAPI;
  private static File c_configFile;

  public static void main( final String[] args )
  {
    setupLogger();
    setupRevapi();
    if ( !processOptions( args ) )
    {
      System.exit( ERROR_PARSING_ARGS_EXIT_CODE );
      return;
    }

    boolean difference;
    try
    {
      final AnalysisContext analysisContext = buildAnalysisContext();
      final AnalysisResult analyze = c_revapi.analyze( analysisContext );
      analyze.throwIfFailed();
      final Map<Reporter, AnalysisContext> reporters = analyze.getExtensions().getReporters();
      final CollectorReporter reporter = (CollectorReporter) reporters.keySet().iterator().next();

      difference = reporter.getReports().stream().anyMatch( r -> !r.getDifferences().isEmpty() );
    }
    catch ( final Throwable t )
    {
      c_logger.log( Level.SEVERE, "Error: Error performing analysis: " + t, t );
      t.printStackTrace();
      System.exit( ERROR_OTHER_EXIT_CODE );
      return;
    }

    if ( difference )
    {
      if ( c_logger.isLoggable( Level.INFO ) )
      {
        c_logger.log( Level.SEVERE, "Error: Difference found between APIs" );
      }
      System.exit( DIFFERENCE_EXIT_CODE );
    }
    else
    {
      if ( c_logger.isLoggable( Level.INFO ) )
      {
        c_logger.log( Level.INFO, "No difference found between APIs" );
      }
      System.exit( NO_DIFFERENCE_EXIT_CODE );
    }
  }

  @Nonnull
  private static AnalysisContext buildAnalysisContext()
    throws IOException
  {
    c_builder.withOldAPI( c_oldAPI.build() );
    c_builder.withNewAPI( c_newAPI.build() );

    if ( null != c_configFile )
    {
      c_builder.withConfigurationFromJSONStream( new FileInputStream( c_configFile ) );

    }
    else
    {
      c_builder.withConfigurationFromJSON( DEFAULT_CONFIG );
    }

    return c_builder.build();
  }

  private static void setupRevapi()
  {
    c_revapi = Revapi.builder()
      .withReporters( CollectorReporter.class )
      .withAllExtensionsFromThreadContextClassLoader()
      .build();

    c_builder = AnalysisContext.builder();
    c_oldAPI = API.builder();
    c_newAPI = API.builder();
  }

  private static void setupLogger()
  {
    c_logger.setUseParentHandlers( false );
    final ConsoleHandler handler = new ConsoleHandler();
    handler.setFormatter( new RawFormatter() );
    c_logger.addHandler( handler );
  }

  private static boolean processOptions( final String[] args )
  {
    // Parse the arguments
    final CLArgsParser parser = new CLArgsParser( args, OPTIONS );

    //Make sure that there was no errors parsing arguments
    if ( null != parser.getErrorString() )
    {
      c_logger.log( Level.SEVERE, "Error: " + parser.getErrorString() );
      return false;
    }
    boolean newApiAdded = false;
    boolean oldApiAdded = false;
    // Get a list of parsed options
    final List<CLOption> options = parser.getArguments();
    for ( final CLOption option : options )
    {
      switch ( option.getId() )
      {
        case CLOption.TEXT_ARGUMENT:
        {
          c_logger.log( Level.SEVERE, "Error: Unexpected argument: " + option.getArgument() );
          return false;
        }
        case CONFIG_OPT:
        {
          final String argument = option.getArgument();
          final File file = new File( argument );
          if ( !file.exists() )
          {
            c_logger.log( Level.SEVERE, "Error: Specified config file does not exist: " + argument );
            return false;
          }
          c_configFile = file;
          break;
        }
        case OLD_API_OPT:
        {
          oldApiAdded = true;
          final String argument = option.getArgument();
          final File file = new File( argument );
          if ( !file.exists() )
          {
            c_logger.log( Level.SEVERE, "Error: Specified old api does not exist: " + argument );
            return false;
          }
          c_oldAPI.addArchive( new FileArchive( file ) );
          break;
        }
        case OLD_API_SUPPORT_OPT:
        {
          final String argument = option.getArgument();
          final File file = new File( argument );
          if ( !file.exists() )
          {
            c_logger.log( Level.SEVERE, "Error: Specified old api support archive does not exist: " + argument );
            return false;
          }
          c_oldAPI.addSupportArchive( new FileArchive( file ) );
          break;
        }
        case NEW_API_OPT:
        {
          newApiAdded = true;
          final String argument = option.getArgument();
          final File file = new File( argument );
          if ( !file.exists() )
          {
            c_logger.log( Level.SEVERE, "Error: Specified new api does not exist: " + argument );
            return false;
          }
          c_newAPI.addArchive( new FileArchive( file ) );
          break;
        }
        case NEW_API_SUPPORT_OPT:
        {
          final String argument = option.getArgument();
          final File file = new File( argument );
          if ( !file.exists() )
          {
            c_logger.log( Level.SEVERE, "Error: Specified new api support archive does not exist: " + argument );
            return false;
          }
          c_newAPI.addSupportArchive( new FileArchive( file ) );
          break;
        }
        case VERBOSE_OPT:
        {
          c_logger.setLevel( Level.ALL );
          break;
        }
        case QUIET_OPT:
        {
          c_logger.setLevel( Level.WARNING );
          break;
        }
        case HELP_OPT:
        {
          printUsage();
          return false;
        }
      }
    }
    if ( !newApiAdded )
    {
      c_logger.log( Level.SEVERE, "Error: --new-api not specified" );
      return false;
    }
    else if ( !oldApiAdded )
    {
      c_logger.log( Level.SEVERE, "Error: --old-api not specified" );
      return false;
    }

    return true;
  }

  /**
   * Print out a usage statement
   */
  private static void printUsage()
  {
    final String lineSeparator = System.getProperty( "line.separator" );
    c_logger.log( Level.INFO,
                  "java " + Main.class.getName() + " [options]" + lineSeparator + "Options: " + lineSeparator +
                  CLUtil.describeOptions( OPTIONS ) );
  }
}