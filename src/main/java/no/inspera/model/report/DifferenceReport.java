package no.inspera.model.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DifferenceReport {
    List<MetaFieldDifferenceReport> meta;
    CandidatesDifferenceReport candidates;
}
