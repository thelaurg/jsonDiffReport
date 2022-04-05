package no.inspera;

import net.sf.json.JSONObject;
import no.inspera.model.*;
import no.inspera.model.report.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 */
public class Parser {

    /**
     * Generate difference report for elements of two fixed known structure JSONObjects.
     *
     * @param before - before Object state
     * @param after - after Object state
     * @return difference report as JSON
     */
    public JSONObject parse(JSONObject before, JSONObject after) {

        Main beforeBean = jsonToBean(before);
        Main afterBean = jsonToBean(after);

        CandidatesDifferenceReport candidatesDifferenceReport =
                generateCandidatesDiffReport(beforeBean.getCandidates(), afterBean.getCandidates());

        List<MetaFieldDifferenceReport> metaFieldsDifferenceReport =
                generateMetaDiffReport(beforeBean.getMeta(), afterBean.getMeta());

        return JSONObject.fromObject(new DifferenceReport(metaFieldsDifferenceReport, candidatesDifferenceReport));
    }

    List<MetaFieldDifferenceReport> generateMetaDiffReport(
            Map<String, String> beforeMeta, Map<String, String> afterMeta) {
        List<MetaFieldDifferenceReport> metaFieldDifferenceReport = new ArrayList<>();
        for (Map.Entry<String, String> afterEntry : afterMeta.entrySet()) {
            String beforeValue = beforeMeta.get(afterEntry.getKey());
            if (beforeValue != null && !beforeValue.equals(afterEntry.getValue())) {
                metaFieldDifferenceReport.add(
                        new MetaFieldDifferenceReport(afterEntry.getKey(), beforeValue, afterEntry.getValue()));
            }
        }
        return metaFieldDifferenceReport;
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


    Main jsonToBean(JSONObject jsonObject) {
        // TODO - treat invalid JSON structure
        // JSONUtils.getMorpherRegistry().registerMorpher(new DateMorpher(new String[] {"yyyy-MM-dd'T'HH:mm:ss'Z'"}));

        Map<String, Class> classMap = Map.of("candidates", Candidate.class);
        return (Main)JSONObject.toBean(jsonObject, Main.class, classMap);
    }

}
