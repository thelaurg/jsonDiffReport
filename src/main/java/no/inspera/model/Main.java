package no.inspera.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Main {
    private Long id;
    private MetaData meta;
    private List<Candidate> candidates;
}
