package org.realityforge.revapi.diff;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.json.Json;
import javax.json.stream.JsonGenerator;
import org.realityforge.getopt4j.CLArgsParser;
import org.realityforge.getopt4j.CLOption;
import org.realityforge.getopt4j.CLOptionDescriptor;
import org.realityforge.getopt4j.CLUtil;
import org.revapi.API;
import org.revapi.AnalysisContext;
import org.revapi.AnalysisResult;
import org.revapi.CompatibilityType;
import org.revapi.Difference;
import org.revapi.Element;
import org.revapi.Report;
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
  private static final int EXPECT_NO_DIFFERENCES_OPT = 7;
  private static final int OUTPUT_OPT = 'o';

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
                            "Specify the path to a jar to compare against. May be specified multiple times. May also be prefixed with <label>:: so that report is generated with using <label> for archive." ),
    new CLOptionDescriptor( "old-api-support",
                            CLOptionDescriptor.ARGUMENT_REQUIRED | CLOptionDescriptor.DUPLICATES_ALLOWED,
                            OLD_API_SUPPORT_OPT,
                            "Specify the path to a jar to referenced by the old-api but not part of the analysis. May be specified multiple times." ),
    new CLOptionDescriptor( "new-api",
                            CLOptionDescriptor.ARGUMENT_REQUIRED | CLOptionDescriptor.DUPLICATES_ALLOWED,
                            NEW_API_OPT,
                            "Specify the path to a jar compared by the tool. May be specified multiple times. May also be prefixed with <label>:: so that report is generated with using <label> for archive." ),
    new CLOptionDescriptor( "new-api-support",
                            CLOptionDescriptor.ARGUMENT_REQUIRED | CLOptionDescriptor.DUPLICATES_ALLOWED,
                            NEW_API_SUPPORT_OPT,
                            "Specify the path to a jar to referenced by the new-api but not part of the analysis. May be specified multiple times." ),
    new CLOptionDescriptor( "output-file",
                            CLOptionDescriptor.ARGUMENT_REQUIRED,
                            OUTPUT_OPT,
                            "The output file reporting the API differences." ),
    new CLOptionDescriptor( "expect-no-differences",
                            CLOptionDescriptor.ARGUMENT_DISALLOWED,
                            EXPECT_NO_DIFFERENCES_OPT,
                            "Return exit code of 1 if API differences are detected." )
  };

  private static final int SUCCESS_EXIT_CODE = 0;
  private static final int DIFFERENCE_EXIT_CODE = 1;
  private static final int ERROR_PARSING_ARGS_EXIT_CODE = 2;
  private static final int ERROR_OTHER_EXIT_CODE = 3;
  private static final Set<String> ATTACHMENT_EXCLUDES =
    Collections.unmodifiableSet( new HashSet<>( Arrays.asList( "exampleUseChainInNewApi",
                                                               "exampleUseChainInOldApi",
                                                               "newArchive",
                                                               "oldArchive" ) ) );
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
    "  }\n" +
    "]";

  private static final Logger c_logger = Logger.getGlobal();
  private static Revapi c_revapi;
  private static AnalysisContext.Builder c_builder;
  private static API.Builder c_oldAPI;
  private static API.Builder c_newAPI;
  private static File c_configFile;
  private static File c_outputFile;
  private static boolean c_errorOnDifferences;

  public static void main( final String[] args )
  {
    setupLogger();
    setupRevapi();
    if ( !processOptions( args ) )
    {
      System.exit( ERROR_PARSING_ARGS_EXIT_CODE );
      return;
    }

    final int differenceCount;
    try
    {
      final AnalysisContext analysisContext = buildAnalysisContext();
      final AnalysisResult analyze = c_revapi.analyze( analysisContext );
      analyze.throwIfFailed();
      final Map<Reporter, AnalysisContext> reporters = analyze.getExtensions().getReporters();
      final CollectorReporter reporter = (CollectorReporter) reporters.keySet().iterator().next();

      differenceCount = emitReport( reporter );
    }
    catch ( final Throwable t )
    {
      c_logger.log( Level.SEVERE, "Error performing analysis: " + t );
      t.printStackTrace();
      System.exit( ERROR_OTHER_EXIT_CODE );
      return;
    }

    if ( 0 != differenceCount )
    {
      if ( c_logger.isLoggable( Level.INFO ) )
      {
        c_logger.log( Level.SEVERE, differenceCount + " differences found between APIs" );
      }
      if ( c_errorOnDifferences )
      {
        System.exit( DIFFERENCE_EXIT_CODE );
      }
      else
      {
        System.exit( SUCCESS_EXIT_CODE );
      }
    }
    else
    {
      if ( c_logger.isLoggable( Level.INFO ) )
      {
        c_logger.log( Level.INFO, "No difference found between APIs" );
      }
      System.exit( SUCCESS_EXIT_CODE );
    }
  }

  private static int emitReport( @Nonnull final CollectorReporter reporter )
    throws IOException
  {
    int differenceCount = 0;
    final List<Report> reports = reporter.getReports();
    try ( final FileOutputStream output = new FileOutputStream( c_outputFile ) )
    {
      final HashMap<String, Object> config = new HashMap<>();
      config.put( JsonGenerator.PRETTY_PRINTING, true );
      final JsonGenerator g = Json.createGeneratorFactory( config ).createGenerator( output );
      differenceCount += emitReports( g, reports );
      g.flush();
      g.close();
    }
    formatJson( c_outputFile );
    return differenceCount;
  }

  /**
   * Format the json file.
   * This is horribly inefficient but it is not called very often so ... meh.
   */
  private static void formatJson( @Nonnull final File file )
    throws IOException
  {
    final byte[] data = Files.readAllBytes( file.toPath() );
    final Charset charset = Charset.forName( "UTF-8" );
    final String jsonData = new String( data, charset );

    final String output =
      jsonData
        .replaceAll( "(?m)^ {4}\\{", "  {" )
        .replaceAll( "(?m)^ {4}}", "  }" )
        .replaceAll( "(?m)^ {8}\"", "    \"" )
        .replaceAll( "(?m)^ {8}}", "    }" )
        .replaceAll( "(?m)^ {12}\"", "      \"" )
        .replaceAll( "(?m)^\n\\[\n", "[\n" ) +
      "\n";
    Files.write( file.toPath(), output.getBytes( charset ) );
  }

  private static int emitReports( @Nonnull final JsonGenerator g, @Nonnull final List<Report> reports )
  {
    int differenceCount = 0;
    g.writeStartArray();
    for ( final Report report : reports )
    {
      final List<Difference> differences = sort( report );
      for ( final Difference difference : differences )
      {
        emitDifference( g, report, difference );
        differenceCount++;
      }
    }
    g.writeEnd();
    return differenceCount;
  }

  /**
   * Sort differences. Order does not matter - just that it is stable so difference reports remain stable
   * and will not churn version control.
   */
  @Nonnull
  private static List<Difference> sort( @Nonnull final Report report )
  {
    return report.getDifferences().stream().sorted( Main::compareDiff ).collect( Collectors.toList() );
  }

  private static int compareDiff( @Nonnull final Difference d1, @Nonnull final Difference d2 )
  {
    return toDescriptor( d1 ).compareTo( toDescriptor( d2 ) );
  }

  /**
   * Produce a uniqueish stable string that should be stable between success runs.
   */
  @Nonnull
  private static String toDescriptor( @Nonnull final Difference d )
  {
    return d.code + "-" +
           sortKeys( d.classification )
             .stream()
             .map( k -> k + "=" + d.classification.get( k ) )
             .collect( Collectors.joining( "," ) ) +
           "-" +
           sortKeys( d.attachments )
             .stream()
             .map( k -> k + "=" + d.attachments.get( k ) )
             .collect( Collectors.joining( "," ) );
  }

  @Nonnull
  private static <K, V> List<K> sortKeys( @Nonnull final Map<K, V> map )
  {
    return map.keySet()
      .stream()
      .sorted()
      .collect( Collectors.toList() );
  }

  private static void emitDifference( @Nonnull final JsonGenerator g,
                                      @Nonnull final Report report,
                                      @Nonnull final Difference difference )
  {
    g.writeStartObject();
    g.write( "code", difference.code );
    g.write( "description", difference.description );
    final Element newElement = report.getNewElement();
    final Element oldElement = report.getOldElement();
    if ( null != oldElement &&
         null != newElement &&
         newElement.getFullHumanReadableString().equals( oldElement.getFullHumanReadableString() ) )
    {
      g.write( "element", newElement.getFullHumanReadableString() );
    }
    else
    {
      if ( null != newElement )
      {
        g.write( "newElement", newElement.getFullHumanReadableString() );
      }
      else
      {
        g.writeNull( "newElement" );
      }
      if ( null != oldElement )
      {
        g.write( "oldElement", oldElement.getFullHumanReadableString() );
      }
      else
      {
        g.writeNull( "oldElement" );
      }
    }
    g.writeStartObject( "classification" );
    for ( final CompatibilityType key : sortKeys( difference.classification ) )
    {
      g.write( key.name(), difference.classification.get( key ).name() );
    }
    g.writeEnd();

    g.writeStartObject( "attachments" );
    for ( final String key : sortKeys( difference.attachments ) )
    {
      if ( !ATTACHMENT_EXCLUDES.contains( key ) )
      {
        g.write( key, difference.attachments.get( key ) );
      }
    }
    g.writeEnd();

    g.writeEnd();
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
          final LabeledFileArchive archive = parseArchive( argument );
          if ( !archive.getFile().exists() )
          {
            c_logger.log( Level.SEVERE, "Error: Specified old api does not exist: " + argument );
            return false;
          }
          c_oldAPI.addArchive( archive );
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
          final LabeledFileArchive archive = parseArchive( argument );
          if ( !archive.getFile().exists() )
          {
            c_logger.log( Level.SEVERE, "Error: Specified new api does not exist: " + argument );
            return false;
          }
          c_newAPI.addArchive( archive );
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
        case OUTPUT_OPT:
        {
          final String argument = option.getArgument();
          final File file = new File( argument );
          if ( !file.getAbsoluteFile().getParentFile().exists() )
          {
            c_logger.log( Level.SEVERE,
                          "Error: Directory containing output file does not exist: " + file.getParentFile() );
            return false;
          }
          c_outputFile = file;
          break;
        }
        case EXPECT_NO_DIFFERENCES_OPT:
        {
          c_errorOnDifferences = true;
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
    else if ( null == c_outputFile )
    {
      c_logger.log( Level.SEVERE, "Error: --output-file not specified" );
      return false;
    }

    return true;
  }

  private static LabeledFileArchive parseArchive( final String argument )
  {
    final String name;
    final File file;
    final int separatorIndex = argument.indexOf( "::" );
    if ( -1 != separatorIndex )
    {
      name = argument.substring( 0, separatorIndex );
      file = new File( argument.substring( separatorIndex + 2 ) );
    }
    else
    {
      file = new File( argument );
      name = file.getName();
    }
    return new LabeledFileArchive( name, file );
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
