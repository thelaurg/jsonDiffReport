package no.inspera;

import lombok.SneakyThrows;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import no.inspera.model.*;
import no.inspera.model.report.CandidateIdReport;
import no.inspera.model.report.CandidatesDifferenceReport;
import no.inspera.model.report.DifferenceReport;
import no.inspera.model.report.MetaFieldDifferenceReport;
import org.apache.commons.io.IOUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class ParserTest {

    Parser parser = new Parser();

    JSONObject beforeJson;
    JSONObject afterJson;
    JSONObject diffJson;

    @Before
    public void setup() {
        // TODO Load in test data from before.json and after.json
        String beforeJsonFilename = "/before.json";
        String afterJsonFilename = "/after.json";
        String diffJsonFilename = "/diff.json";
        beforeJson = loadJSONFromFile(beforeJsonFilename);
        afterJson = loadJSONFromFile(afterJsonFilename);
        diffJson = loadJSONFromFile(diffJsonFilename);
    }

    @Test
    public void jsonDifferenceReportMustBeGeneratedCorrectly() {
        JSONObject actualJSONReport = parser.parse(beforeJson, afterJson);
        Map<String, Class> classMap =Map.of(
                "meta", MetaFieldDifferenceReport.class,
                "edited", CandidateIdReport.class,
                "removed", CandidateIdReport.class,
                "added", CandidateIdReport.class
        );
        DifferenceReport actualCDR =
                (DifferenceReport)JSONObject.toBean(actualJSONReport, DifferenceReport.class, classMap);
        DifferenceReport expectedCDR =
                (DifferenceReport)JSONObject.toBean(diffJson, DifferenceReport.class, classMap);

        MatcherAssert.assertThat(expectedCDR.getMeta(),  Matchers.containsInAnyOrder(actualCDR.getMeta().toArray()));
        MatcherAssert.assertThat(expectedCDR.getCandidates().getAdded(),
                Matchers.containsInAnyOrder(actualCDR.getCandidates().getAdded().toArray()));
        MatcherAssert.assertThat(expectedCDR.getCandidates().getEdited(),
                Matchers.containsInAnyOrder(actualCDR.getCandidates().getEdited().toArray()));
        MatcherAssert.assertThat(expectedCDR.getCandidates().getRemoved(),
                Matchers.containsInAnyOrder(actualCDR.getCandidates().getRemoved().toArray()));
    }

    @Test
    public void jsonObjectsAreCorrectlyTransformedToBeans() {
        Main main = parser.jsonToBean(beforeJson);
        Main expectedMain = constructBean();

        Assert.assertEquals(expectedMain, main);
    }

    @Test
    public void candidateListDifferencesMustBeProcessedCorrectly() {
        List<Candidate> candidatesBefore = generateBeforeCandidates();
        List<Candidate> candidatesAfter = generateAfterCandidates();
        CandidatesDifferenceReport candidatesDifferenceReportExpected =
                generateCandidatesDiffReport();

        CandidatesDifferenceReport candidatesDifferenceReportActual =
                parser.generateCandidatesDiffReport(candidatesBefore, candidatesAfter);

        Assert.assertEquals(candidatesDifferenceReportExpected, candidatesDifferenceReportActual);
    }

    @Test
    public void metaFieldsDifferencesMustBeProcessedCorrectly() {
        Map<String, String> fieldsBefore =
                Map.of(
                        "field1", "value1",
                        "field2", "value2",
                        "field3", "value3",
                        "field4", "value4"
                );
        Map<String, String> fieldsAfter =
                Map.of(
                        "field1", "value1",
                        "field2", "new value2",
                        "field3", "value3",
                        "field4", "new value4"
                );

        List<MetaFieldDifferenceReport> expected = List.of(
                new MetaFieldDifferenceReport("field2","value2","new value2"),
                new MetaFieldDifferenceReport("field4","value4","new value4")
        );
        List<MetaFieldDifferenceReport> actualMetaReport = parser.generateMetaDiffReport(fieldsBefore, fieldsAfter);

        MatcherAssert.assertThat(expected,
                Matchers.containsInAnyOrder(actualMetaReport.toArray()));
    }

    private CandidatesDifferenceReport generateCandidatesDiffReport() {
        List<CandidateIdReport> added = List.of(new CandidateIdReport(15L));
        List<CandidateIdReport> removed = List.of(new CandidateIdReport(14L));
        List<CandidateIdReport> edited = List.of(new CandidateIdReport(12L), new CandidateIdReport(13L));
        return new CandidatesDifferenceReport(edited, added, removed);
    }

    private List<Candidate> generateBeforeCandidates() {
        return List.of(
                new Candidate(10L, "C1", 0),
                new Candidate(11L, "C2", 10),
                new Candidate(12L, "C22", 30),
                new Candidate(13L, "C3", 20),
                new Candidate(14L, "C4", 40)
        );
    }

    private List<Candidate> generateAfterCandidates() {
        return List.of(
                new Candidate(10L, "C1", 0),
                new Candidate(11L, "C2", 10),
                new Candidate(12L, "C2", 30),
                new Candidate(13L, "C3", 25),
                new Candidate(15L, "C5", 50)
        );
    }

    private Main constructBean() {
        Map<String, String> meta = Map.of(
                        "title", "Title",
                        "startTime", "2016-01-20T10:00:00Z",
                        "endTime", "2016-01-20T16:00:00Z");
        List<Candidate> candidates = List.of(
                new Candidate(10L, "C1", 0),
                new Candidate(11L, "C2", 10),
                new Candidate(12L, "C3", 20)
        );

        return new Main(1L, meta, candidates);
    }

    @SneakyThrows
    private JSONObject loadJSONFromFile(String fileName) {
        String jsonTxt = IOUtils.toString(
                this.getClass().getResourceAsStream(fileName),
                "UTF-8");
        return (JSONObject)JSONSerializer.toJSON(jsonTxt);
    }

}
