package no.inspera.model.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MetaFieldDifferenceReport {
    String field;
    String before;
    String after;
}
