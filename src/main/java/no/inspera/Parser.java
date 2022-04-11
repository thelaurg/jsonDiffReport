package no.inspera;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.SneakyThrows;
import no.inspera.model.Candidate;
import no.inspera.model.Main;
import no.inspera.model.MetaData;
import no.inspera.model.report.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 */
public class Parser {

    private final ObjectMapper objectMapper;
    private final DateTimeFormatter dateTimeFormatter;

    public Parser() {
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.registerModule(new JavaTimeModule());
        dateTimeFormatter = new DateTimeFormatterBuilder().appendPattern("yyyy-MM-dd'T'HH:mm:ss")
                .parseLenient()
                .appendOffset("+HH:MM", "UTC")
                .toFormatter();

    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    /**
     * Generate difference report for elements of two fixed known structure JsonNode.
     *
     * @param before - before Object state
     * @param after - after Object state
     * @return difference report as JSON
     */
    public JsonNode parse(JsonNode before, JsonNode after) {

        Main beforeBean = jsonToBean(before);
        Main afterBean = jsonToBean(after);

        CandidatesDifferenceReport candidatesDifferenceReport =
                generateCandidatesDiffReport(beforeBean.getCandidates(), afterBean.getCandidates());

        List<MetaFieldDifferenceReport> metaFieldsDifferenceReport =
                generateMetaDiffReport(beforeBean.getMeta(), afterBean.getMeta());

        return objectMapper.convertValue(new DifferenceReport(metaFieldsDifferenceReport, candidatesDifferenceReport), JsonNode.class);
    }

    List<MetaFieldDifferenceReport> generateMetaDiffReport(
            MetaData beforeMeta, MetaData afterMeta) {
        List<MetaFieldDifferenceReport> metaFieldDifferenceReport = new ArrayList<>();

        if(!beforeMeta.getTitle().equals(afterMeta.getTitle())) {
            metaFieldDifferenceReport.add(
                    new MetaFieldDifferenceReport("title", beforeMeta.getTitle(), afterMeta.getTitle()));
        }
        if(beforeMeta.getStartTime().compareTo(afterMeta.getStartTime()) != 0) {
            metaFieldDifferenceReport.add(
                    new MetaFieldDifferenceReport("startTime", formatDateToOsloTimezone(beforeMeta.getStartTime()),
                            formatDateToOsloTimezone(afterMeta.getStartTime())));
        }
        if(beforeMeta.getEndTime().compareTo(afterMeta.getEndTime()) != 0) {
            metaFieldDifferenceReport.add(
                    new MetaFieldDifferenceReport("endTime", formatDateToOsloTimezone(beforeMeta.getEndTime()),
                            formatDateToOsloTimezone(afterMeta.getEndTime())));
        }

        return metaFieldDifferenceReport;
    }

    private String formatDateToOsloTimezone(LocalDateTime date) {
        ZonedDateTime zdt = date.atZone(ZoneId.of("UTC"));

        return dateTimeFormatter.format(zdt.withZoneSameInstant(ZoneId.of("Europe/Oslo")));

    }

    CandidatesDifferenceReport generateCandidatesDiffReport(List<Candidate> beforeCandidates,
                                                            List<Candidate> afterCandidates) {
        List<CandidateProcessingWrapper> beforeCandidatesPW =
                beforeCandidates.stream()
                        .map(c -> new CandidateProcessingWrapper(c, CandidateState.BEFORE))
                        .collect(Collectors.toList());
        List<CandidateProcessingWrapper> afterCandidatesPW =
                afterCandidates.stream()
                        .map(c -> new CandidateProcessingWrapper(c, CandidateState.AFTER))
                        .collect(Collectors.toList());

        List<CandidateProcessingWrapper> allCandidatesDifferences =
                Stream.concat(beforeCandidatesPW.stream(), afterCandidatesPW.stream())
                .collect(Collectors.toList());
        Map<Long, Optional<StateAction>> candidatesPossibleDifferences =
                allCandidatesDifferences.stream().collect(Collectors.groupingBy(t -> t.getCandidate().getId(),
                Collectors.collectingAndThen(Collectors.toList(),
                        this::processCandidateDifference
                        )));

        CandidatesDifferenceReport candidatesDifferencesReport = new CandidatesDifferenceReport();
        candidatesPossibleDifferences.entrySet().stream().filter(e -> e.getValue().isPresent()).forEach(
                cf -> candidatesDifferencesReport.addDifference(cf.getValue().get(), cf.getKey())
        );

        return candidatesDifferencesReport;
    }


    private Optional<StateAction> processCandidateDifference(List<CandidateProcessingWrapper> candidateStates) {
        if (candidateStates.size() == 1) {
            StateAction action =
                    candidateStates.get(0).getState() == CandidateState.BEFORE ? StateAction.REMOVED : StateAction.ADDED;
            return Optional.of(action);
        }
        // we might have an edit
        if (candidateStates.size() == 2) {
            Candidate cState1 = candidateStates.get(0).getCandidate();
            Candidate cState2 = candidateStates.get(1).getCandidate();
            if (cState1.equals(cState2)) {
                return Optional.empty();
            } else {
                return Optional.of(StateAction.EDITED);
            }
        }
        return Optional.empty();
    }


    @SneakyThrows
    Main jsonToBean(JsonNode jsonRoot) {
        return objectMapper.treeToValue(jsonRoot, Main.class);
    }

}
