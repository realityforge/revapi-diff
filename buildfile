require 'buildr/git_auto_version'

desc 'revapi-diff: Report differences between Java APIs'
define 'revapi-diff' do
  project.group = 'org.realityforge.revapi.diff'
  compile.options.source = '1.8'
  compile.options.target = '1.8'
  compile.options.lint = 'all'

  compile.with :javax_annotation,
               :getopt4j,
               :revapi,
               :revapi_basic_features,
               :revapi_java_spi,
               :revapi_java,
               :dmr,
               :cookcc,
               :slf4j_api,
               :slf4j_jdk14

  test.using :testng

  package(:jar)
  package(:jar, :classifier => 'all').tap do |jar|
    compile.dependencies.each do |d|
      jar.merge(d)
    end
  end
end
