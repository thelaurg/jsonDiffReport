package no.inspera.model.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import no.inspera.model.Candidate;

@Data
@AllArgsConstructor
public class CandidateProcessingWrapper {
    Candidate candidate;
    CandidateState state;
}
