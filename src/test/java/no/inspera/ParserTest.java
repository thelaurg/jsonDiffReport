package no.inspera;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import no.inspera.model.Candidate;
import no.inspera.model.Main;
import no.inspera.model.MetaData;
import no.inspera.model.report.CandidateIdReport;
import no.inspera.model.report.CandidatesDifferenceReport;
import no.inspera.model.report.DifferenceReport;
import no.inspera.model.report.MetaFieldDifferenceReport;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class ParserTest {

    Parser parser = new Parser();

    JsonNode beforeJson;
    JsonNode afterJson;
    JsonNode diffJson;

    @Before
    public void setup() {
        String beforeJsonFilename = "/before.json";
        String afterJsonFilename = "/after.json";
        String diffJsonFilename = "/diff.json";
        beforeJson = loadJSONFromFile(beforeJsonFilename);
        afterJson = loadJSONFromFile(afterJsonFilename);
        diffJson = loadJSONFromFile(diffJsonFilename);
    }

    @Test
    public void jsonDifferenceReportMustBeGeneratedCorrectly() {
        JsonNode actualJSONReport = parser.parse(beforeJson, afterJson);
        ObjectMapper objectMapper = parser.getObjectMapper();

        DifferenceReport actualCDR =
                objectMapper.convertValue(actualJSONReport, DifferenceReport.class);
        DifferenceReport expectedCDR =
                objectMapper.convertValue(diffJson, DifferenceReport.class);

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
   //     List<MetaFieldDifferenceReport> actualMetaReport = parser.generateMetaDiffReport(fieldsBefore, fieldsAfter);

      //          Matchers.containsInAnyOrder(actualMetaReport.toArray()));
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

    @SneakyThrows
    private Main constructBean() {
        DateTimeFormatter sdf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
        MetaData meta = new MetaData(
                        "Title",
                         LocalDateTime.parse("2016-04-20T10:00:00Z", sdf),
                LocalDateTime.parse("2016-04-20T16:00:00Z", sdf));
        List<Candidate> candidates = List.of(
                new Candidate(10L, "C1", 0),
                new Candidate(11L, "C2", 10),
                new Candidate(12L, "C3", 20)
        );

        return new Main(1L, meta, candidates);
    }

    @SneakyThrows
    private JsonNode loadJSONFromFile(String fileName) {
        return parser.getObjectMapper().readTree(new URL("file:src/test/resources/" + fileName));
    }

}
