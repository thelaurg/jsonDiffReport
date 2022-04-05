package no.inspera.model.report;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
public class CandidatesDifferenceReport {
    List<CandidateIdReport> edited;
    List<CandidateIdReport> added;
    List<CandidateIdReport> removed;

    public CandidatesDifferenceReport() {
        edited = new ArrayList<>();
        added = new ArrayList<>();
        removed = new ArrayList<>();
    }

    public void addDifference(StateAction stateAction, Long candidateId) {
        assert added != null;
        assert removed != null;
        assert edited != null;

        switch (stateAction) {
            case ADDED:
                added.add(new CandidateIdReport(candidateId));
                break;
            case EDITED:
                edited.add(new CandidateIdReport(candidateId));
                break;
            case REMOVED:
                removed.add(new CandidateIdReport(candidateId));
        }
    }
}
