## Implementation (using Jackson library)

### Assumptions
* structure of JSON is fixed

### Implementation
 * Transformed and worked with JSONs as java beans for strong type and better semantics
 * Using `@JsonFormat` and `DateTimeFormatter` to shape the date format accordingly to JSON definition
   * One remark on `diff.json`: need to change date in order to follow the UTC+2 format in respect to DST.
     * changed month from `Jan` to `Apr`

#### TO BE CONSIDERED
 * treat invalid json structure
 * duplicate candidate IDs

