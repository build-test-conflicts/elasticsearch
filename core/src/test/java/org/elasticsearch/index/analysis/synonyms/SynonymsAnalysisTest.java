package org.elasticsearch.index.analysis.synonyms;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.elasticsearch.Version;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.inject.Injector;
import org.elasticsearch.common.inject.ModulesBuilder;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.lucene.all.AllEntries;
import org.elasticsearch.common.lucene.all.AllTokenStream;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.SettingsModule;
import org.elasticsearch.env.Environment;
import org.elasticsearch.env.EnvironmentModule;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.IndexNameModule;
import org.elasticsearch.index.analysis.AnalysisModule;
import org.elasticsearch.index.analysis.AnalysisService;
import org.elasticsearch.index.settings.IndexSettingsModule;
import org.elasticsearch.indices.analysis.IndicesAnalysisModule;
import org.elasticsearch.indices.analysis.IndicesAnalysisService;
import org.elasticsearch.test.ESTestCase;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import java.io.IOException;
import static org.elasticsearch.common.settings.Settings.settingsBuilder;
import static org.hamcrest.Matchers.equalTo;
/** 
 */
public class SynonymsAnalysisTest extends ESTestCase {
  ESLogger logger=Loggers.getLogger(getClass());
  private AnalysisService analysisService;
  @Test public void testSynonymsAnalysis() throws IOException {
    String json="/org/elasticsearch/index/analysis/synonyms/synonyms.json";
    Settings settings=settingsBuilder().loadFromStream(json,getClass().getResourceAsStream(json)).put("path.home",createTempDir().toString()).put(IndexMetaData.SETTING_VERSION_CREATED,Version.CURRENT).build();
    Index index=new Index("test");
    Injector parentInjector=new ModulesBuilder().add(new SettingsModule(settings),new EnvironmentModule(new Environment(settings)),new IndicesAnalysisModule()).createInjector();
    Injector injector=new ModulesBuilder().add(new IndexSettingsModule(index,settings),new IndexNameModule(index),new AnalysisModule(settings,parentInjector.getInstance(IndicesAnalysisService.class))).createChildInjector(parentInjector);
    analysisService=injector.getInstance(AnalysisService.class);
    match("synonymAnalyzer","kimchy is the dude abides","shay is the elasticsearch man!");
    match("synonymAnalyzer_file","kimchy is the dude abides","shay is the elasticsearch man!");
    match("synonymAnalyzerWordnet","abstain","abstain refrain desist");
    match("synonymAnalyzerWordnet_file","abstain","abstain refrain desist");
    match("synonymAnalyzerWithsettings","kimchy","sha hay");
  }
  public void match(  String analyzerName,  String source,  String target) throws IOException {
    Analyzer analyzer=analysisService.analyzer(analyzerName).analyzer();
    AllEntries allEntries=new AllEntries();
    allEntries.addText("field",source,1.0f);
    allEntries.reset();
    TokenStream stream=AllTokenStream.allTokenStream("_all",allEntries,analyzer);
    stream.reset();
    CharTermAttribute termAtt=stream.addAttribute(CharTermAttribute.class);
    StringBuilder sb=new StringBuilder();
    while (stream.incrementToken()) {
      sb.append(termAtt.toString()).append(" ");
    }
    MatcherAssert.assertThat(target,equalTo(sb.toString().trim()));
  }
  public SynonymsAnalysisTest(){
  }
}
