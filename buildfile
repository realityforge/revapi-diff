require 'buildr/git_auto_version'
require 'buildr/gpg'

Buildr::MavenCentral.define_publish_tasks(:profile_name => 'org.realityforge', :username => 'realityforge')

desc 'revapi-diff: Report differences between Java APIs'
define 'revapi-diff' do
  project.group = 'org.realityforge.revapi.diff'
  compile.options.source = '1.8'
  compile.options.target = '1.8'
  compile.options.lint = 'all'

  project.version = ENV['PRODUCT_VERSION'] if ENV['PRODUCT_VERSION']

  pom.add_apache_v2_license
  pom.add_github_project('realityforge/revapi-diff')
  pom.add_developer('realityforge', 'Peter Donald')

  manifest['Main-Class'] = 'org.realityforge.revapi.diff.Main'

  compile.with :javax_annotation,
               :getopt4j,
               :javax_json,
               :revapi,
               :revapi_basic_features,
               :revapi_java_spi,
               :revapi_java,
               :dmr,
               :cookcc,
               :slf4j_api,
               :slf4j_jdk14

  package(:jar)
  package(:sources)
  package(:javadoc)
  package(:jar, :classifier => 'all').tap do |jar|
    compile.dependencies.each do |d|
      jar.merge(d)
    end
  end
end
