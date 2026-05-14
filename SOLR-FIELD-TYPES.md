# Solr Field Types Reference: Java Type Mappings for SolrJ

> Research compiled from Apache Solr 9.x and 10.x source code, official reference guide,
> and SolrJ serialization layer (`JavaBinCodec`, `DocsStreamer`, `DocumentObjectBinder`).

## How SolrJ Determines Java Types (The Critical Path)

When Solr returns stored field values, the type you receive in `SolrDocument.getFieldValue()`
depends on a dispatch chain in `DocsStreamer.getValue()`:

1. If the field type's class is in the **`KNOWN_TYPES`** set, Solr calls `fieldType.toObject(field)`
   which returns a **typed Java object** (Integer, Boolean, Date, etc.).
2. If the field type implements **`ExternalizeStoredValuesAsObjects`** (marker interface introduced
   for future field types), same behaviour as above.
3. **Otherwise**, Solr calls `fieldType.toExternal(field)` which returns a **String**.

This means field types NOT in `KNOWN_TYPES` and NOT implementing the marker interface will always
return `String` through SolrJ, regardless of their internal storage format. This is a common source
of confusion.

### KNOWN_TYPES (Solr 9 and 10)

```
BoolField, StrField, TextField, BinaryField, DenseVectorField,
IntPointField, LongPointField, FloatPointField, DoublePointField, DatePointField,
TrieField, TrieIntField, TrieLongField, TrieFloatField, TrieDoubleField, TrieDateField
```

> **Note from Solr source:** "We do not add UUIDField because UUID object is not a supported type
> in JavaBinCodec and if we write UUIDField.toObject, we wouldn't know how to handle it on the
> client side."

### JavaBin Wire Format Types

SolrJ's default binary protocol (`JavaBinCodec`) supports these native Java types on the wire:

| Tag              | Java Type        | Notes                                                   |
|------------------|------------------|---------------------------------------------------------|
| NULL             | `null`           |                                                         |
| BOOL_TRUE/FALSE  | `Boolean`        |                                                         |
| BYTE             | `Byte`           |                                                         |
| SHORT            | `Short`          |                                                         |
| INT              | `Integer`        |                                                         |
| LONG             | `Long`           |                                                         |
| FLOAT            | `Float`          |                                                         |
| DOUBLE           | `Double`         |                                                         |
| DATE             | `java.util.Date` | Stored as epoch millis (long). NOT `java.time.Instant`. |
| STR              | `String`         | UTF-8 encoded                                           |
| BYTEARR          | `byte[]`         |                                                         |
| ARR              | `ArrayList`      | Used for multiValued fields                             |
| MAP              | `LinkedHashMap`  |                                                         |
| SOLRDOC          | `SolrDocument`   | For nested/child documents                              |
| ENUM_FIELD_VALUE | `EnumFieldValue` | For EnumFieldType                                       |

Any Java type NOT in this list cannot survive the SolrJ wire protocol natively, which is why
field types like `UUIDField` return `String` even though they could theoretically return `UUID`.

---

## Complete Field Type Reference

### Numeric Field Types

| Solr Class              | Common Schema Name | Java Read Type | Java Write Type                  | multiValued                                   | Write/Read Asymmetry | Solr 9/10 |
|-------------------------|--------------------|----------------|----------------------------------|-----------------------------------------------|----------------------|-----------|
| `solr.IntPointField`    | `pint`             | `Integer`      | `Integer`, `int`, any `Number`   | No (returns `List<Integer>` when multiValued) | None                 | Both      |
| `solr.LongPointField`   | `plong`            | `Long`         | `Long`, `long`, any `Number`     | No                                            | None                 | Both      |
| `solr.FloatPointField`  | `pfloat`           | `Float`        | `Float`, `float`, any `Number`   | No                                            | None                 | Both      |
| `solr.DoublePointField` | `pdouble`          | `Double`       | `Double`, `double`, any `Number` | No                                            | None                 | Both      |

**Notes:**

- All PointField types use Lucene's dimensional points for indexing, enabling efficient range
  queries.
- Requires `docValues="true"` for sorting on single-valued fields.
- `toNativeType()` calls `((Number) val).intValue()` (etc.), so any `Number` subclass is accepted on
  write.

### Deprecated Numeric Field Types (Trie-based)

| Solr Class             | Common Schema Name | Java Read Type   | Java Write Type                     | Replacement        | Solr 9/10                               |
|------------------------|--------------------|------------------|-------------------------------------|--------------------|-----------------------------------------|
| `solr.TrieIntField`    | `tint`             | `Integer`        | `Integer`                           | `IntPointField`    | Solr 9 only (deprecated); removed in 10 |
| `solr.TrieLongField`   | `tlong`            | `Long`           | `Long`                              | `LongPointField`   | Solr 9 only (deprecated); removed in 10 |
| `solr.TrieFloatField`  | `tfloat`           | `Float`          | `Float`                             | `FloatPointField`  | Solr 9 only (deprecated); removed in 10 |
| `solr.TrieDoubleField` | `tdouble`          | `Double`         | `Double`                            | `DoublePointField` | Solr 9 only (deprecated); removed in 10 |
| `solr.TrieDateField`   | `tdate`            | `java.util.Date` | `java.util.Date`, ISO-8601 `String` | `DatePointField`   | Solr 9 only (deprecated); removed in 10 |
| `solr.TrieField`       | (base class)       | varies           | varies                              | Point variants     | Solr 9 only (deprecated); removed in 10 |

### Date Field Types

| Solr Class            | Common Schema Name | Java Read Type   | Java Write Type                                         | multiValued | Write/Read Asymmetry                                                                                                                                                                            | Solr 9/10 |
|-----------------------|--------------------|------------------|---------------------------------------------------------|-------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------|
| `solr.DatePointField` | `pdate`            | `java.util.Date` | `java.util.Date`, ISO-8601 `String`                     | No          | **Yes**: Write accepts ISO-8601 strings like `"2024-01-15T10:30:00Z"` or `Date` objects. Read always returns `java.util.Date`. SolrJ wire format stores as epoch millis.                        | Both      |
| `solr.DateRangeField` | `daterange`        | `String`         | `String` (ISO-8601 or range syntax `[NOW-1DAY TO NOW]`) | No          | **Yes**: NOT in KNOWN_TYPES, so always returns `String` via `toExternal()`. Input and output are both strings, but the read format is always ISO-8601 with `Z` suffix for point-in-time values. | Both      |

**Critical asymmetry:** `DatePointField` returns `java.util.Date`; `DateRangeField` returns
`String`.
Despite both representing dates, they have completely different Java return types. If you switch a
schema field from `DatePointField` to `DateRangeField`, your Java code will break.

### String & Text Field Types

| Solr Class               | Common Schema Name              | Java Read Type | Java Write Type | multiValued | Write/Read Asymmetry                                                                                             | Solr 9/10 |
|--------------------------|---------------------------------|----------------|-----------------|-------------|------------------------------------------------------------------------------------------------------------------|-----------|
| `solr.StrField`          | `string`                        | `String`       | `String`        | No          | None                                                                                                             | Both      |
| `solr.TextField`         | `text_general`, `text_en`, etc. | `String`       | `String`        | No          | None. Analyzers (tokenizers, filters) only affect indexing; stored value is the original string.                 | Both      |
| `solr.SortableTextField` | `text_gen_sort`                 | `String`       | `String`        | No          | None. Extends `TextField`; adds docValues support on first 1024 chars (configurable via `maxCharsForDocValues`). | Both      |
| `solr.CollationField`    | `collatedstring`                | `String`       | `String`        | No          | None                                                                                                             | Both      |
| `solr.ICUCollationField` | `collatedstring_icu`            | `String`       | `String`        | No          | None. Requires `analysis-extras` contrib/module.                                                                 | Both      |

### Boolean Field Type

| Solr Class       | Common Schema Name | Java Read Type | Java Write Type                                                                   | multiValued | Write/Read Asymmetry                                                                           | Solr 9/10 |
|------------------|--------------------|----------------|-----------------------------------------------------------------------------------|-------------|------------------------------------------------------------------------------------------------|-----------|
| `solr.BoolField` | `boolean`          | `Boolean`      | `Boolean`, `String` (`"true"`, `"1"`, `"t"`, `"T"` = true; anything else = false) | No          | **Minor**: Write accepts various truthy string representations; read always returns `Boolean`. | Both      |

### UUID Field Type

| Solr Class       | Common Schema Name | Java Read Type | Java Write Type                        | multiValued | Write/Read Asymmetry                                                                                                                                                                                                                                                                                                | Solr 9/10 |
|------------------|--------------------|----------------|----------------------------------------|-------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------|
| `solr.UUIDField` | `uuid`             | `String`       | `String` (or `"NEW"` to auto-generate) | No          | **Yes**: Extends `StrField` and is NOT in KNOWN_TYPES as a separate entry, but inherits `StrField`'s `toObject()` returning `String`. You write a UUID string, you get a UUID string back. The value is lowercased internally. `java.util.UUID` objects are NOT returned because `UUID` is not a JavaBin wire type. | Both      |

### Binary Field Type

| Solr Class         | Common Schema Name | Java Read Type | Java Write Type                         | multiValued | Write/Read Asymmetry                                                                                                                                                                                                                                                                                                                                           | Solr 9/10 |
|--------------------|--------------------|----------------|-----------------------------------------|-------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------|
| `solr.BinaryField` | `binary`           | `byte[]`       | `byte[]`, `ByteBuffer`, Base64 `String` | No          | **Yes**: `toObject()` returns `ByteBuffer` wrapping the bytes. `DocumentObjectBinder` has special handling: if the target field is `ByteBuffer`, it wraps `byte[]` into `ByteBuffer`. JavaBin wire sends as `byte[]` (tag BYTEARR). Actual type received depends on response format: JavaBin = `byte[]`, then `DocumentObjectBinder` may wrap to `ByteBuffer`. | Both      |

### Enum Field Type

| Solr Class           | Common Schema Name | Java Read Type   | Java Write Type                                      | multiValued | Write/Read Asymmetry                                                                                                                                                                                                                                                          | Solr 9/10                  |
|----------------------|--------------------|------------------|------------------------------------------------------|-------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------|
| `solr.EnumFieldType` | `enum`             | `EnumFieldValue` | `String` (the enum label) or `Integer` (the ordinal) | No          | **Yes, significant**: Write a string label like `"Critical"`. Read back an `EnumFieldValue` object containing both `intValue` (sort ordinal) and `stringValue` (label). `EnumFieldValue.toString()` returns the string label. JavaBin has a dedicated `ENUM_FIELD_VALUE` tag. | Both                       |
| `solr.EnumField`     | (deprecated)       | `EnumFieldValue` | `String`                                             | No          | Same as above                                                                                                                                                                                                                                                                 | Solr 9 only; removed in 10 |

**`EnumFieldValue` structure:**

```java
public class EnumFieldValue implements Comparable<EnumFieldValue>, Serializable {

  private Integer intValue;   // sort ordinal
  private String stringValue; // display label

  public String toString() {
	return stringValue;
  }
}
```

### Spatial Field Types

| Solr Class                                 | Common Schema Name  | Java Read Type                | Java Write Type                                   | multiValued                 | Write/Read Asymmetry                                                                                                                                                                             | Solr 9/10 |
|--------------------------------------------|---------------------|-------------------------------|---------------------------------------------------|-----------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------|
| `solr.LatLonPointSpatialField`             | `location`          | `String` (`"lat,lon"`)        | `String` (`"lat,lon"`)                            | Yes (supports multi-valued) | **Subtle**: NOT in KNOWN_TYPES, returns `String` via `toExternal()`. Precision is 7 decimal places (~1.4cm). Input format `"lat,lon"`, output format `"lat,lon"` (may differ in trailing zeros). | Both      |
| `solr.SpatialRecursivePrefixTreeFieldType` | `location_rpt`      | `String` (WKT or `"lat lon"`) | `String` (WKT format or `"lat,lon"`)              | Yes                         | NOT in KNOWN_TYPES; returns `String` via `toExternal()`. Stored value is the original WKT/coordinate string.                                                                                     | Both      |
| `solr.RptWithGeometrySpatialField`         | `location_rpt_geom` | `String`                      | `String` (WKT)                                    | Yes                         | Same as RPT but also stores original geometry for retrieval. Returns `String`.                                                                                                                   | Both      |
| `solr.BBoxField`                           | `bbox`              | `String`                      | `String` (`"minX minY maxX maxY"`)                | No                          | NOT in KNOWN_TYPES. Uses sub-fields internally (4 doubles + 1 boolean for dateline crossing). Returns `String` via `toExternal()`.                                                               | Both      |
| `solr.PointType`                           | `point`             | `String`                      | `String` (`"x,y"` or `"x,y,z"` for n-dimensional) | No                          | NOT in KNOWN_TYPES; returns comma-separated coordinate `String`.                                                                                                                                 | Both      |

**Important:** All spatial types return `String` through SolrJ. None of them are in KNOWN_TYPES,
so they always go through `toExternal()`. If you need structured lat/lon values, you must parse the
returned string yourself.

### Dense Vector Field Type

| Solr Class              | Common Schema Name | Java Read Type                                                              | Java Write Type                                 | multiValued            | Write/Read Asymmetry                                                                                                                                                                                  | Solr 9/10                       |
|-------------------------|--------------------|-----------------------------------------------------------------------------|-------------------------------------------------|------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------|
| `solr.DenseVectorField` | `knn_vector`       | FLOAT32: `List<Float>` / BYTE: `List<Number>` (containing `Integer` values) | `List<Float>`, `float[]`, JSON array of numbers | **Never** multi-valued | **Yes**: Write accepts `float[]` or `List<Float>`. FLOAT32 read returns `List<Float>` (not `float[]`). BYTE encoding reads return `List<Number>` where each element is `Integer` (widened from byte). | Both, with Solr 10 enhancements |

**Solr 10 changes for DenseVectorField:**

- Added support for scalar and binary quantized vectors (reduced memory)
- Can index primitive `float[]` directly via JavaBin (SOLR-17948)
- New query types: `SeededKnnVectorQuery`, `PatienceKnnVectorQuery`
- HNSW parameter rename: `hnswMaxConnections` -> `hnswM`, `hnswBeamWidth` -> `hnswEfConstruction`

### Currency Field Type

| Solr Class               | Common Schema Name | Java Read Type             | Java Write Type           | multiValued | Write/Read Asymmetry                                                                                                                                                                                                                                           | Solr 9/10                  |
|--------------------------|--------------------|----------------------------|---------------------------|-------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------|
| `solr.CurrencyFieldType` | `currency`         | `String` (`"amount,CODE"`) | `String` (`"100.50,USD"`) | No          | NOT in KNOWN_TYPES. Stored as string `"amount,currencyCode"`. Internally uses two sub-fields (amount as `Long`, code as `String`), but the stored parent field is a `String`. If no currency code provided on write, appends the configured `defaultCurrency`. | Both                       |
| `solr.CurrencyField`     | (deprecated)       | `String`                   | `String`                  | No          | Same as above                                                                                                                                                                                                                                                  | Solr 9 only; removed in 10 |

### Special-Purpose Field Types

| Solr Class               | Common Schema Name | Java Read Type          | Java Write Type                                            | multiValued | Notes                                                                                                                              | Solr 9/10                      |
|--------------------------|--------------------|-------------------------|------------------------------------------------------------|-------------|------------------------------------------------------------------------------------------------------------------------------------|--------------------------------|
| `solr.RandomSortField`   | `random`           | N/A (not stored)        | N/A (not indexed with values)                              | No          | Never stores values. Used only for random sort ordering. Generates hash-based sort keys at query time.                             | Both                           |
| `solr.ExternalFileField` | N/A                | N/A (not stored)        | N/A (values from external file)                            | No          | Values come from an external file, not the index. Not stored, not retrievable via getFieldValue(). Used for boosting.              | **Solr 9 only; removed in 10** |
| `solr.PreAnalyzedField`  | N/A                | `String`                | Serialized token stream (JSON or SimplePreAnalyzed format) | No          | NOT in KNOWN_TYPES; stored part (if any) returns `String`.                                                                         | **Solr 9 only; removed in 10** |
| `solr.NestPathField`     | `_nest_path_`      | `String`                | `String`                                                   | No          | Extends `SortableTextField`. Stores hierarchy path for nested documents. Returns `String`.                                         | Both                           |
| `solr.RankField`         | `rank`             | Not typically retrieved | `Float` (as string `"0.5"`)                                | No          | Stores scoring factors in Lucene `FeatureField`. Not designed for retrieval via `getFieldValue()`. Used at query time for ranking. | Both                           |

---

## multiValued Behaviour Summary

No Solr field type is "multiValued by nature." The `multiValued` attribute is a schema-level
property
set on individual field definitions, not intrinsic to field type classes.

When `multiValued="true"` is set on a field:

- **SolrJ read:** `getFieldValue()` returns a `Collection<Object>` (typically `ArrayList`)
  containing
  multiple typed values. Each element has the same Java type as the single-valued case.
- **SolrJ read (single value in multiValued field):** Still returns the single object directly (not
  wrapped in a list), unless `getFieldValues()` is called which always returns a `Collection`.
- **SolrJ write:** `SolrInputDocument.addField()` can be called multiple times for the same field,
  or you can pass a `Collection`.
- **DocumentObjectBinder:** Maps to `List<T>` or `Collection<T>` in Java beans. If the bean field
  is a `List`, it receives the collection. If the bean field is a scalar type and the Solr field has
  multiple values, only the first value is used.

**Exception:** `LatLonPointSpatialField` documents itself as "possibly multi-valued" because
geospatial use cases commonly need multiple coordinate pairs per document. But it still follows the
standard `multiValued` schema property mechanics.

---

## Solr 9 vs Solr 10 Differences Summary

### Field Types Removed in Solr 10

| Removed Type             | Replacement              | Notes                                   |
|--------------------------|--------------------------|-----------------------------------------|
| `solr.TrieIntField`      | `solr.IntPointField`     | All Trie types deprecated since Solr 7  |
| `solr.TrieLongField`     | `solr.LongPointField`    |                                         |
| `solr.TrieFloatField`    | `solr.FloatPointField`   |                                         |
| `solr.TrieDoubleField`   | `solr.DoublePointField`  |                                         |
| `solr.TrieDateField`     | `solr.DatePointField`    |                                         |
| `solr.TrieField`         | Appropriate PointField   |                                         |
| `solr.CurrencyField`     | `solr.CurrencyFieldType` |                                         |
| `solr.EnumField`         | `solr.EnumFieldType`     |                                         |
| `solr.ExternalFileField` | None (removed entirely)  | SOLR-17655                              |
| `solr.PreAnalyzedField`  | None (removed entirely)  | SOLR-17839; incompatible with Lucene 10 |

### Field Types Added/Enhanced in Solr 10

| Type                    | Change                                                                                          |
|-------------------------|-------------------------------------------------------------------------------------------------|
| `solr.DenseVectorField` | Scalar and binary quantized vectors; native `float[]` indexing via JavaBin; HNSW params renamed |

### SolrJ API Changes in Solr 10

| Change                                                                | Impact                                           |
|-----------------------------------------------------------------------|--------------------------------------------------|
| `SolrQuery` moved to `org.apache.solr.client.solrj.request.SolrQuery` | Import change required                           |
| Response `version` parameter removed                                  | SolrJ no longer auto-appends version to requests |
| Trie types removed from `KNOWN_TYPES`                                 | N/A if you already migrated to Point types       |

### Behaviour Unchanged Between Solr 9 and 10

- All Point field types (`IntPointField`, `LongPointField`, etc.) return the same Java types
- `DatePointField` still returns `java.util.Date` (not `java.time.Instant`)
- `BoolField` still returns `Boolean`
- `StrField`/`TextField` still return `String`
- `BinaryField` still returns `byte[]`/`ByteBuffer`
- `EnumFieldType` still returns `EnumFieldValue`
- All spatial types still return `String`
- `UUIDField` still returns `String`
- JavaBin wire format type tags are unchanged
- `DocumentObjectBinder` type coercion logic is unchanged

---

## DocumentObjectBinder Type Coercion

The `DocumentObjectBinder` performs minimal type coercion when mapping `SolrDocument` fields to
Java bean properties annotated with `@Field`:

1. **No automatic numeric widening/narrowing.** If Solr returns `Long` but your bean has `int`, you
   get a `ClassCastException`. The value from `SolrDocument` is assigned directly.
2. **`byte[]` to `ByteBuffer`** is the only explicit conversion (for `BinaryField`).
3. **multiValued to List/Array**: If the bean field is `List<T>` or `T[]`, the collection from
   `SolrDocument` is assigned or converted to an array. If the bean field is scalar and the document
   has multiple values, the first value is used.
4. **No date conversion.** `java.util.Date` from SolrJ is NOT automatically converted to
   `java.time.Instant`, `LocalDateTime`, or any `java.time` type.
5. **No String-to-UUID conversion.** `UUIDField` returns `String`; if your bean has `UUID`, you need
   a custom converter.
6. **Child documents** (nested) are recursively bound to beans.

---

## Quick Reference: Java Type by Solr Field Type

For rapid lookup, sorted by the Java type you receive:

### Returns `String`

- `StrField`, `TextField`, `SortableTextField`
- `UUIDField`
- `CollationField`, `ICUCollationField`
- `DateRangeField` (NOT `java.util.Date`)
- `LatLonPointSpatialField`
- `SpatialRecursivePrefixTreeFieldType`
- `RptWithGeometrySpatialField`
- `BBoxField`
- `PointType`
- `CurrencyFieldType`
- `NestPathField`
- `PreAnalyzedField` (Solr 9 only)

### Returns `Integer`

- `IntPointField`

### Returns `Long`

- `LongPointField`

### Returns `Float`

- `FloatPointField`

### Returns `Double`

- `DoublePointField`

### Returns `Boolean`

- `BoolField`

### Returns `java.util.Date`

- `DatePointField`

### Returns `byte[]` (via JavaBin) / `ByteBuffer` (via `toObject()`)

- `BinaryField`

### Returns `EnumFieldValue`

- `EnumFieldType`

### Returns `List<Float>` or `List<Number>`

- `DenseVectorField` (FLOAT32 encoding: `List<Float>`; BYTE encoding: `List<Number>` of `Integer`)

### Returns nothing (not retrievable)

- `RandomSortField`
- `ExternalFileField` (Solr 9 only)
- `RankField` (not designed for retrieval)

---

## Implications for Spring Data Solr

When building a repository/template layer over SolrJ, these asymmetries matter:

1. **Date handling**: `DatePointField` gives you `java.util.Date`, but modern Java code wants
   `java.time.Instant`. Your mapping layer should convert.

2. **UUID handling**: `UUIDField` gives you `String`, not `java.util.UUID`. Your mapping layer
   should convert if the entity field is `UUID`.

3. **Spatial types**: All return `String`. Parse `"lat,lon"` yourself if you need structured
   coordinates.

4. **Enum types**: Returns `EnumFieldValue`, not `String` or Java enum. Need conversion.

5. **DateRangeField vs DatePointField**: Completely different return types for date-like fields.
   The mapping layer must handle both.

6. **BinaryField**: May arrive as `byte[]` or `ByteBuffer` depending on the response path.
   Handle both.

7. **DenseVectorField**: Returns `List<Float>`, not `float[]`. If your entity uses `float[]`,
   you need conversion.

8. **multiValued fields**: A field configured as `multiValued="true"` returns `Collection<T>` when
   it has multiple values but may return a bare `T` when it has exactly one value. Defensive coding
   required.
