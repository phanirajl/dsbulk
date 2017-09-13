# DataStax Bulk Loader Options

*NOTE:* The long options described here can be persisted in `conf/application.conf` and thus permanently override defaults and avoid specifying options on the command line.

## Sections

<a href="#Common">Common Settings</a><br>
<a href="#connector">Connector Settings</a><br>
<a href="#schema">Schema Settings</a><br>
<a href="#batch">Batch Settings</a><br>
<a href="#codec">Codec Settings</a><br>
<a href="#driver">Driver Settings</a><br>
&nbsp;&nbsp;&nbsp;<a href="#driver.auth">Driver Auth Settings</a><br>
&nbsp;&nbsp;&nbsp;<a href="#driver.policy">Driver Policy Settings</a><br>
&nbsp;&nbsp;&nbsp;<a href="#driver.pooling">Driver Pooling Settings</a><br>
&nbsp;&nbsp;&nbsp;<a href="#driver.protocol">Driver Protocol Settings</a><br>
&nbsp;&nbsp;&nbsp;<a href="#driver.query">Driver Query Settings</a><br>
&nbsp;&nbsp;&nbsp;<a href="#driver.socket">Driver Socket Settings</a><br>
&nbsp;&nbsp;&nbsp;<a href="#driver.ssl">Driver Ssl Settings</a><br>
<a href="#engine">Engine Settings</a><br>
<a href="#executor">Executor Settings</a><br>
<a href="#log">Log Settings</a><br>
<a href="#monitoring">Monitoring Settings</a><br>
<a name="Common"></a>
## Common Settings

#### -f _&lt;string&gt;_

Load settings from the given file rather than `conf/application.conf`.

#### -url,--connector.csv.url _&lt;string&gt;_

The URL or path of the resource(s) to read from or write to.

Which URL protocols are available depend on which URL stream handlers have been installed, but at least the following are guaranteed to be supported:

- **stdin**:  the stdin protocol can be used to read from standard input; the only valid URL for this protocol is: `stdin:/`.

  This protocol cannot be used for writing.

- **stdout**: the stdout protocol can be used to write to standard output; the only valid URL for this protocol is: `stdout:/`.

  This protocol cannot be used for reading.

- **file**: the file protocol can be used with all supported file systems, local or not.
    - **When reading**: the URL can point to a single file, or to an existing directory; in case of a directory, the *fileNamePattern* setting can be used to filter files to read, and the *recursive* setting can be used to control whether or not the connector should look for files in subdirectories as well.
    - **When writing**: the URL will be treated as a directory; if it doesn't exist, the loader will attempt to create it; CSV files will be created inside this directory, and their names can be controlled with the *fileNameFormat* setting.

Note that if the value specified here does not have a protocol, then it is assumed to be a file protocol.

Examples:

    url = /path/to/dir/or/file           # without protocol
    url = "file:///path/to/dir/or/file"  # with protocol
    url = "stdin:/"                      # to read csv data from stdin
    url = "stdout:/"                     # to write csv data to stdout

For other URLs: the URL will be read or written directly; settings like *fileNamePattern*, *recursive*, and *fileNameFormat* will have no effect.

This setting has no default value and must be supplied by the user.

Defaults to **""**.

#### -c,--connector.name _&lt;string&gt;_

The name of the connector to use.

It is used in two places:

1. The path to the group of settings for the connector are located under `connector.<name>`.
2. The connector class name must start with `name`, case-insensitively. It is permitted for `name` to be the fully-qualified class name of the connector. That simply implies that the settings root will be at that fully-qualified location.

Example: `csv` for class `CSVConnector`, with settings located under `connector.csv`.

Defaults to **"csv"**.

#### -delim,--connector.csv.delimiter _&lt;string&gt;_

The character to use as field delimiter.

Only one character can be specified. Note that this setting applies to all files to be read or written.

Defaults to **","**.

#### -header,--connector.csv.header _&lt;boolean&gt;_

Whether the files to read or write begin with a header line or not.

When reading:

 - if set to true, the first non-empty line in every file is discarded, even if the *skipLines* setting is set to zero (see below). However, that line will be used to assign field names to each record, thus allowing mappings by field name such as `{myFieldName1 = myColumnName1, myFieldName2 = myColumnName2}`.
 - if set to false, records will not contain field names, only (zero-based) field indexes; in this case, the mapping should be index-based, such as in `{0 = myColumnName1, 1 = myColumnName2}`.

When writing:

 - if set to true: each file will begin with a header line;
 - if set to false, files will not contain header lines.

Note that this setting applies to all files to be read or written.

Defaults to **true**.

#### -skipLines,--connector.csv.skipLines _&lt;number&gt;_

Defines a number of lines to skip from each input file before the parser can begin to execute.

Ignored when writing.

Defaults to **0**.

#### -maxLines,--connector.csv.maxLines _&lt;number&gt;_

Defines the maximum number of lines to read from or write to each file.

When reading, all lines past this number will be discarded.

When writing, a file will contain at most this number of lines; if more records remain to be written, a new file will be created using the *fileNameFormat* setting.

Note that when writing to anything other than a directory, this setting is ignored.

This setting takes into account the *header* setting: if a file begins with a header line, that line is counted.

This feature is disabled by default (indicated by its `-1` value).

Defaults to **-1**.

#### -k,--schema.keyspace _&lt;string&gt;_

The keyspace to connect to. Optional.

If not specified, then the *schema.query* setting must be specified.

Defaults to **""**.

#### -t,--schema.table _&lt;string&gt;_

The destination table. Optional.

If not specified, then the *schema.query* setting must be specified.

Defaults to **""**.

#### -m,--schema.mapping _&lt;string&gt;_

The field-to-column mapping to use.

Applies to both load and unload workflows.

If not specified, the loader will apply a strict one-to-one mapping between the source fields and the database table. If that is not what you want, then you must supply an explicit mapping.

Mappings should be specified as a HOCON map of the following form:

- Indexed data sources: `0 = col1, 1 = col2, 2 = col3`, where `0`, `1`, `2`, etc. are the zero-based indices of fields in the source data; and `col1`, `col2`, `col3` are bound variable names in the insert statement.
- Mapped data sources: `fieldA = col1, fieldB = col2, fieldC = col3`, where `fieldA`, `fieldB`, `fieldC`, etc. are field names in the source data; and `col1`, `col2`, `col3` are bound variable names in the insert statement.

The exact type of mapping to use depends on the connector being used. Some connectors can only produce indexed records; others can only produce mapped ones, while others are capable of producing both indexed and mapped records at the same time. Refer to the connector's documentation to know which kinds of mapping it supports.

Defaults to **""**.

#### -h,--driver.hosts _&lt;string&gt;_

The contact points to use for the initial connection to the cluster.

This must be a comma-separated list of hosts, each specified by a host-name or ip address. If the host is a DNS name that resolves to multiple A-records, all the corresponding addresses will be used. Do not use `localhost` as a host-name (since it resolves to both IPv4 and IPv6 addresses on some platforms).

Note that each host entry may optionally be followed by `:port` to specify the port to connect to. When not specified, this value falls back to the *port* setting.

Defaults to **"127.0.0.1"**.

#### -port,--driver.port _&lt;number&gt;_

The port to connect to at initial contact points.

Note that all nodes in a cluster must accept connections on the same port number.

Defaults to **9042**.

#### -p,--driver.auth.password _&lt;string&gt;_

The password to use. Required.

Providers that accept this setting:
 - `PlainTextAuthProvider`
 - `DsePlainTextAuthProvider`

Defaults to **"cassandra"**.

#### -u,--driver.auth.username _&lt;string&gt;_

The username to use. Required.

Providers that accept this setting:
 - `PlainTextAuthProvider`
 - `DsePlainTextAuthProvider`

Defaults to **"cassandra"**.

#### -cl,--driver.query.consistency _&lt;string&gt;_

The consistency level to use for both loads and unloads.

Valid values are: `ANY`, `LOCAL_ONE`, `ONE`, `TWO`, `THREE`, `LOCAL_QUORUM`, `QUORUM`, `EACH_QUORUM`, `ALL`.

Defaults to **"LOCAL_ONE"**.

#### ---executor.maxPerSecond _&lt;number&gt;_

The maximum number of concurrent requests per second. This acts as a safeguard against workflows that could overwhelm the cluster with more requests than it can handle. Batch statements count for as many requests as their number of inner statements.

Setting this option to any negative value will disable it.

Defaults to **100000**.

#### -maxErrors,--log.maxErrors _&lt;number&gt;_

The maximum number of errors to tolerate before aborting the entire operation.

Setting this value to `-1` disables this feature (not recommended).

Defaults to **100**.

#### -logDir,--log.directory _&lt;string&gt;_

The directory where all log files will be stored.

Note that this must be a path pointing to a writable directory.

Log files for a specific run will be located in a sub-directory inside the directory specified here. Each run generates a sub-directory identified by an "operation ID', which is basically a timestamp in the format: `yyyy_MM_dd_HH_mm_ss_nnnnnnnnn`.

Setting this value to `.` denotes the current working directory.

Defaults to **"."**.

#### -reportRate,--monitoring.reportRate _&lt;string&gt;_

The report interval for the console reporter.

The console reporter will print useful metrics about the ongoing operation at this rate.

Durations lesser than one second will be rounded up to 1 second.

Defaults to **"5 seconds"**.

<a name="connector"></a>
## Connector Settings

Connector-specific settings. This section contains settings for the connector to use; it also contains sub-sections, one for each available connector.

#### -url,--connector.csv.url _&lt;string&gt;_

The URL or path of the resource(s) to read from or write to.

Which URL protocols are available depend on which URL stream handlers have been installed, but at least the following are guaranteed to be supported:

- **stdin**:  the stdin protocol can be used to read from standard input; the only valid URL for this protocol is: `stdin:/`.

  This protocol cannot be used for writing.

- **stdout**: the stdout protocol can be used to write to standard output; the only valid URL for this protocol is: `stdout:/`.

  This protocol cannot be used for reading.

- **file**: the file protocol can be used with all supported file systems, local or not.
    - **When reading**: the URL can point to a single file, or to an existing directory; in case of a directory, the *fileNamePattern* setting can be used to filter files to read, and the *recursive* setting can be used to control whether or not the connector should look for files in subdirectories as well.
    - **When writing**: the URL will be treated as a directory; if it doesn't exist, the loader will attempt to create it; CSV files will be created inside this directory, and their names can be controlled with the *fileNameFormat* setting.

Note that if the value specified here does not have a protocol, then it is assumed to be a file protocol.

Examples:

    url = /path/to/dir/or/file           # without protocol
    url = "file:///path/to/dir/or/file"  # with protocol
    url = "stdin:/"                      # to read csv data from stdin
    url = "stdout:/"                     # to write csv data to stdout

For other URLs: the URL will be read or written directly; settings like *fileNamePattern*, *recursive*, and *fileNameFormat* will have no effect.

This setting has no default value and must be supplied by the user.

Defaults to **""**.

#### -c,--connector.name _&lt;string&gt;_

The name of the connector to use.

It is used in two places:

1. The path to the group of settings for the connector are located under `connector.<name>`.
2. The connector class name must start with `name`, case-insensitively. It is permitted for `name` to be the fully-qualified class name of the connector. That simply implies that the settings root will be at that fully-qualified location.

Example: `csv` for class `CSVConnector`, with settings located under `connector.csv`.

Defaults to **"csv"**.

#### -delim,--connector.csv.delimiter _&lt;string&gt;_

The character to use as field delimiter.

Only one character can be specified. Note that this setting applies to all files to be read or written.

Defaults to **","**.

#### -header,--connector.csv.header _&lt;boolean&gt;_

Whether the files to read or write begin with a header line or not.

When reading:

 - if set to true, the first non-empty line in every file is discarded, even if the *skipLines* setting is set to zero (see below). However, that line will be used to assign field names to each record, thus allowing mappings by field name such as `{myFieldName1 = myColumnName1, myFieldName2 = myColumnName2}`.
 - if set to false, records will not contain field names, only (zero-based) field indexes; in this case, the mapping should be index-based, such as in `{0 = myColumnName1, 1 = myColumnName2}`.

When writing:

 - if set to true: each file will begin with a header line;
 - if set to false, files will not contain header lines.

Note that this setting applies to all files to be read or written.

Defaults to **true**.

#### -skipLines,--connector.csv.skipLines _&lt;number&gt;_

Defines a number of lines to skip from each input file before the parser can begin to execute.

Ignored when writing.

Defaults to **0**.

#### -maxLines,--connector.csv.maxLines _&lt;number&gt;_

Defines the maximum number of lines to read from or write to each file.

When reading, all lines past this number will be discarded.

When writing, a file will contain at most this number of lines; if more records remain to be written, a new file will be created using the *fileNameFormat* setting.

Note that when writing to anything other than a directory, this setting is ignored.

This setting takes into account the *header* setting: if a file begins with a header line, that line is counted.

This feature is disabled by default (indicated by its `-1` value).

Defaults to **-1**.

#### -comment,--connector.csv.comment _&lt;string&gt;_

The character that represents a line comment when found in the beginning of a line of text.

Only one character can be specified. Note that this setting applies to all files to be read or written.

This feature is disabled by default (indicated by its `null` character value).

Defaults to **"\u0000"**.

#### -encoding,--connector.csv.encoding _&lt;string&gt;_

The file encoding to use.

Note that this setting applies to all files to be read or written.

Defaults to **"UTF-8"**.

#### -escape,--connector.csv.escape _&lt;string&gt;_

The character used for escaping quotes inside an already quoted value.

Only one character can be specified. Note that this setting applies to all files to be read or written.

Defaults to **"\\"**.

#### ---connector.csv.fileNameFormat _&lt;string&gt;_

The file name format to use when writing.

Ignored when reading. Ignored for non-file URLs.

The file name must comply with the formatting rules of `String.format()`, and must contain a `%d` format specifier that will be used to increment file name counters.

Defaults to **"output-%0,6d.csv"**.

#### ---connector.csv.fileNamePattern _&lt;string&gt;_

The glob pattern to use when searching for files to read. The syntax to use is the glob syntax, as described in `java.nio.file.FileSystem.getPathMatcher()`.

Ignored when writing. Ignored for non-file URLs.

Only applicable when the *url* setting points to a directory on a known filesystem, ignored otherwise.

Defaults to **"\*\*/\*.csv"**.

#### -maxThreads,--connector.csv.maxThreads _&lt;number&gt;_

The maximum number of reading or writing threads. In other words, this setting controls how many files can be read or written simultaneously.

Defaults to **4**.

#### -quote,--connector.csv.quote _&lt;string&gt;_

The character used for quoting fields when the field delimiter is part of the field value.

Only one character can be specified. Note that this setting applies to all files to be read or written.

Defaults to **"\""**.

#### ---connector.csv.recursive _&lt;boolean&gt;_

Whether to scan for files in subdirectories of the root directory.

Only applicable when the *url* setting points to a directory on a known filesystem, ignored otherwise. Ignored when writing.

Defaults to **false**.

<a name="schema"></a>
## Schema Settings

Schema-specific settings.

#### -k,--schema.keyspace _&lt;string&gt;_

The keyspace to connect to. Optional.

If not specified, then the *schema.query* setting must be specified.

Defaults to **""**.

#### -t,--schema.table _&lt;string&gt;_

The destination table. Optional.

If not specified, then the *schema.query* setting must be specified.

Defaults to **""**.

#### -m,--schema.mapping _&lt;string&gt;_

The field-to-column mapping to use.

Applies to both load and unload workflows.

If not specified, the loader will apply a strict one-to-one mapping between the source fields and the database table. If that is not what you want, then you must supply an explicit mapping.

Mappings should be specified as a HOCON map of the following form:

- Indexed data sources: `0 = col1, 1 = col2, 2 = col3`, where `0`, `1`, `2`, etc. are the zero-based indices of fields in the source data; and `col1`, `col2`, `col3` are bound variable names in the insert statement.
- Mapped data sources: `fieldA = col1, fieldB = col2, fieldC = col3`, where `fieldA`, `fieldB`, `fieldC`, etc. are field names in the source data; and `col1`, `col2`, `col3` are bound variable names in the insert statement.

The exact type of mapping to use depends on the connector being used. Some connectors can only produce indexed records; others can only produce mapped ones, while others are capable of producing both indexed and mapped records at the same time. Refer to the connector's documentation to know which kinds of mapping it supports.

Defaults to **""**.

#### -nullStrings,--schema.nullStrings _&lt;list&lt;string&gt;&gt;_

Case-sensitive strings that should be mapped to `null`.

In load workflows, when a record field value matches one of these words, then that value is replaced with a `null` and forwarded to DSE as such.

This setting only applies for fields of type String.

Note that this setting is applied before the *schema.nullToUnset* setting, hence any `null`s produced by a null-string can still be left unset if required.

In unload workflows, only the first string specified here will be used: when a row cell contains a `null` value, then it will be replaced with that word and forwarded as such to the connector.

The default value – `[""]` – means that in load workflows, empty strings are converted to `null`s, and in unload workflows, `null`s are converted to empty strings.

Defaults to **[""]**.

#### ---schema.nullToUnset _&lt;boolean&gt;_

Whether or not to map `null` input values to "unset" in the database, meaning don't modify a potentially pre-existing value of this field for this row.

This is only valid for load scenarios; it is ignored otherwise.

Note that this setting is applied after the *schema.nullStrings* setting, and may intercept `null`s produced by that setting.

Note that setting this to false leads to tombstones being created in the database to represent `null`.

Defaults to **true**.

#### -query,--schema.query _&lt;string&gt;_

The query to use. Optional.

If not specified, then *schema.keyspace* and *schema.table* must be specified, and dsbulk will infer the appropriate statement based on the table's metadata, using all available columns.

In load worflows, the statement can be any `INSERT` or `UPDATE` statement, but **must** use named bound variables exclusively; positional bound variables will not work.

Bound variable names usually match those of the columns in the destination table, but this is not a strict requirement; it is, however, required that their names match those specified in the mapping.

See *schema.mapping* setting for more information.

In unload worflows, the statement can be any regular `SELECT` statement; it can optionally contain a token range restriction clause of the form: `token(...) > :start and token(...) <= :end.`

If such a clause is present, the engine will generate as many statements as there are token ranges in the cluster, thus allowing parallelization of reads while at the same time targeting coordinators that are also replicas.

The column names in the SELECT clause will be used to match column names specified in the mapping. See "mapping" setting for more information.

Defaults to **""**.

#### ---schema.recordMetadata _&lt;string&gt;_

Record metadata.

Applies within both load and unload workflows to records being respectively read from or written to the connector.

This information is optional, and rarely needed.

If not specified:

- If the connector is capable of reporting the record metadata accurately (for example, some database connectors might be able to inspect the target table's metadata), then this section is only required if you want to override some field types as reported by the connector.
- If the connector is not capable of reporting the record metadata accurately (for example, file connectors usually cannot report such information), then all fields are assumed to be of type `String`. If this is not correct, then you need to provide the correct type information here.

Field metadata should be specified as a HOCON map of the following form:

- Indexed data sources: `0 = java.lang.String, 1 = java.lang.Double`, where `0`, `1`, etc. are the zero-based indices of fields in the source data; and the values are the expected types for each field.
- Mapped data sources: `fieldA = java.lang.String, fieldB = java.lang.Double`, where `fieldA`, `fieldB`, etc. are field names in the source data; and the values are the expected types for each field.

Defaults to **""**.

<a name="batch"></a>
## Batch Settings

Batch-specific settings.

These settings control how the workflow engine groups together statements before writing them.

Only applicable for load workflows, ignored otherwise.

See `com.datastax.dsbulk.executor.api.batch.StatementBatcher` for more information.

#### ---batch.bufferSize _&lt;number&gt;_

The buffer size to use when batching.

Defaults to **10000**.

#### ---batch.maxBatchSize _&lt;number&gt;_

The maximum batch size.

Defaults to **100**.

#### ---batch.mode _&lt;string&gt;_

The grouping mode. Valid values are:
- **PARTITION_KEY**: Groups together statements that share the same partition key. This is the default mode, and the preferred one.
- **REPLICA_SET**: Groups together statements that share the same replica set. This mode might yield better results for small clusters and lower replication factors, but tends to perform equally well or worse than `PARTITION_KEY` for larger clusters or high replication factors.

Defaults to **"PARTITION_KEY"**.

<a name="codec"></a>
## Codec Settings

Conversion-specific settings. These settings apply for both load and unload workflows.

When writing, these settings determine how record fields emitted by connectors are parsed.

When unloading, these settings determine how row cells emitted by DSE are formatted.

#### ---codec.booleanWords _&lt;list&lt;string&gt;&gt;_

All representations of true and false supported by dsbulk. Each representation is of the form `true-value:false-value`, case-insensitive.

In load workflows, all representations are taken into account.

In unload workflows, the first true-false pair will be used to format booleans; all other pairs will be ignored.

Defaults to **["1:0","Y:N","T:F","YES:NO","TRUE:FALSE"]**.

#### ---codec.date _&lt;string&gt;_

The temporal pattern to use for `String` to CQL date conversions. This can be either:

- A date-time pattern
- A pre-defined formatter such as `ISO_LOCAL_DATE`

For more information on patterns and pre-defined formatters, see https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html.

Defaults to **"ISO_LOCAL_DATE"**.

#### ---codec.itemDelimiter _&lt;string&gt;_

The delimiter for tokenized fields. This setting will be used when mapping a `String` field to CQL columns that require special tokenization (collections, tuples, maps, and UDTs), to isolate elements in the string.

For maps and udt's, this setting delimits entries, while *keyValueSeparator* separates the key from the value within an entry.

Note that this is not the same as CSV parsing. This setting will be applied to record fields produced by any connector, as long as the target CQL type requires tokenization.

Defaults to **","**.

#### ---codec.keyValueSeparator _&lt;string&gt;_

The key-value separator to use to split keys and values in a tokenized field.

This setting will be used when mapping a `String` field to CQL columns of type map or UDT, to isolate keys/fields from values, after key-value pairs have been tokenized using the *codec.itemDelimiter* setting.

This setting will be applied to record fields produced by any connector, as long as the the target CQL type requires key-value tokenization.

Defaults to **":"**.

#### -locale,--codec.locale _&lt;string&gt;_

The locale to use for locale-sensitive conversions.

Defaults to **"en_US"**.

#### ---codec.number _&lt;string&gt;_

The `DecimalFormat` pattern to use for `String` to `Number` conversions. See `java.text.DecimalFormat` for details about the pattern syntax to use.

Defaults to **"#,###.##"**.

#### ---codec.time _&lt;string&gt;_

The temporal pattern to use for `String` to CQL time conversions. This can be either:

- A date-time pattern
- A pre-defined formatter such as `ISO_LOCAL_TIME`

For more information on patterns and pre-defined formatters, see https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html.

Defaults to **"ISO_LOCAL_TIME"**.

#### -timeZone,--codec.timeZone _&lt;string&gt;_

The time zone to use for temporal conversions that do not convey any explicit time zone information.

Defaults to **"UTC"**.

#### ---codec.timestamp _&lt;string&gt;_

The temporal pattern to use for `String` to CQL timestamp conversions. This can be either:

- A date-time pattern
- A pre-defined formatter such as `ISO_DATE_TIME`
- The special value `CQL_DATE_TIME`, which is a special parser that accepts all valid CQL literal formats for the `timestamp` type

For more information on patterns and pre-defined formatters, see https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html.

Defaults to **"CQL_DATE_TIME"**.

<a name="driver"></a>
## Driver Settings

Driver-specific configuration.

#### -h,--driver.hosts _&lt;string&gt;_

The contact points to use for the initial connection to the cluster.

This must be a comma-separated list of hosts, each specified by a host-name or ip address. If the host is a DNS name that resolves to multiple A-records, all the corresponding addresses will be used. Do not use `localhost` as a host-name (since it resolves to both IPv4 and IPv6 addresses on some platforms).

Note that each host entry may optionally be followed by `:port` to specify the port to connect to. When not specified, this value falls back to the *port* setting.

Defaults to **"127.0.0.1"**.

#### -port,--driver.port _&lt;number&gt;_

The port to connect to at initial contact points.

Note that all nodes in a cluster must accept connections on the same port number.

Defaults to **9042**.

#### ---driver.addressTranslator _&lt;string&gt;_

The simple or fully-qualified class name of the address translator to use.

This is only needed if the nodes are not directly reachable from the driver (for example, the driver is in a different network region and needs to use a public IP, or it connects through a proxy).

Defaults to **"IdentityTranslator"**.

#### ---driver.timestampGenerator _&lt;string&gt;_

The simple or fully-qualified class name of the timestamp generator to use. Built-in options are:

- **AtomicTimestampGenerator**: timestamps are guaranteed to be unique across all client threads.
- **ThreadLocalTimestampGenerator**: timestamps are guaranteed to be unique within each thread only.
- **ServerSideTimestampGenerator**: do not generate timestamps, let the server assign them.

Defaults to **"AtomicMonotonicTimestampGenerator"**.

<a name="driver.auth"></a>
### Driver Auth Settings

Authentication settings.

#### -p,--driver.auth.password _&lt;string&gt;_

The password to use. Required.

Providers that accept this setting:
 - `PlainTextAuthProvider`
 - `DsePlainTextAuthProvider`

Defaults to **"cassandra"**.

#### -u,--driver.auth.username _&lt;string&gt;_

The username to use. Required.

Providers that accept this setting:
 - `PlainTextAuthProvider`
 - `DsePlainTextAuthProvider`

Defaults to **"cassandra"**.

#### ---driver.auth.provider _&lt;string&gt;_

The name of the AuthProvider to use.
 - **None**: no authentication.
 - **PlainTextAuthProvider**:
   Uses `com.datastax.driver.core.PlainTextAuthProvider` for authentication. Supports SASL authentication using the `PLAIN` mechanism (plain text authentication).
 - **DsePlainTextAuthProvider**:
   Uses `com.datastax.driver.dse.auth.DsePlainTextAuthProvider` for authentication. Supports SASL authentication to DSE clusters using the `PLAIN` mechanism (plain text authentication), and also supports optional proxy authentication; should be preferred to `PlainTextAuthProvider` when connecting to secured DSE clusters.
 - **DseGSSAPIAuthProvider**:
   Uses `com.datastax.driver.dse.auth.DseGSSAPIAuthProvider` for authentication. Supports SASL authentication to DSE clusters using the `GSSAPI` mechanism (Kerberos authentication), and also supports optional proxy authentication.

Defaults to **"None"**.

#### ---driver.auth.authorizationId _&lt;string&gt;_

The authorization ID to use. Optional.

An authorization ID allows the currently authenticated user to act as a different user (a.k.a. proxy authentication).

Providers that accept this setting:
 - `DsePlainTextAuthProvider`
 - `DseGSSAPIAuthProvider`

Defaults to **""**.

#### ---driver.auth.keyTab _&lt;string&gt;_

The path of the Kerberos keytab file to use for authentication. Optional.

If left unspecified, it is assumed that authentication will be done with a ticket cache instead.

Providers that accept this setting:
 - `DseGSSAPIAuthProvider`

Defaults to **""**.

#### ---driver.auth.principal _&lt;string&gt;_

The Kerberos principal to use. Required.

Providers that accept this setting:
 - `DseGSSAPIAuthProvider`

Defaults to **"user@DATASTAX.COM"**.

#### ---driver.auth.saslProtocol _&lt;string&gt;_

The SASL protocol name to use. Required.

This value should match the username of the Kerberos service principal used by the DSE server. This information is specified in the `dse.yaml` file by the *service_principal* option under the *kerberos_options* section, and may vary from one DSE installation to another – especially if you installed DSE with an automated package installer.

Providers that accept this setting:
 - `DseGSSAPIAuthProvider`

Defaults to **"dse"**.

<a name="driver.policy"></a>
### Driver Policy Settings

Settings for various driver policies.

#### -lbp,--driver.policy.lbp _&lt;string&gt;_

The simple or fully-qualified class name of the `LoadBalancingPolicy` implementation to use.

If not specified, defaults to the driver's default load balancing policy (`com.datastax.driver.dse.DseLoadBalancingPolicy`).

Defaults to **""**.

#### -retry,--driver.policy.retry _&lt;string&gt;_

The simple or fully-qualified class name of the `RetryPolicy` implementation to use.

If not specified, defaults to the driver's default retry policy (`com.datastax.driver.core.policies.DefaultRetryPolicy`).

Defaults to **""**.

#### ---driver.policy.specexec _&lt;string&gt;_

The simple or fully-qualified class name of the `SpeculativeExecutionPolicy` implementation to use.

If not specified, defaults to the driver's default speculative execution policy (`com.datastax.driver.core.policies.NoSpeculativeExecutionPolicy`).

Defaults to **""**.

<a name="driver.pooling"></a>
### Driver Pooling Settings

Pooling-specific settings.

The driver maintains a connection pool to each node, according to the distance assigned to it by the load balancing policy. If the distance is `IGNORED`, no connections are maintained.

#### ---driver.pooling.heartbeat _&lt;string&gt;_

The heartbeat interval. If a connection stays idle for that duration (no reads), the driver sends a dummy message on it to make sure it's still alive. If not, the connection is trashed and replaced.

Defaults to **"30 seconds"**.

#### ---driver.pooling.local.connections _&lt;number&gt;_

The number of connections in the pool for nodes at "local" distance.

Defaults to **1**.

#### ---driver.pooling.local.requests _&lt;number&gt;_

The maximum number of requests that can be executed concurrently on a connection.

This must be between 1 and 32768.

Defaults to **32768**.

#### ---driver.pooling.remote.connections _&lt;number&gt;_

The number of connections in the pool for nodes at "remote" distance.

Defaults to **1**.

#### ---driver.pooling.remote.requests _&lt;number&gt;_

The maximum number of requests that can be executed concurrently on a connection.

This must be between 1 and 32768.

Defaults to **1024**.

<a name="driver.protocol"></a>
### Driver Protocol Settings

Native Protocol-specific settings.

#### ---driver.protocol.compression _&lt;string&gt;_

The compression algorithm to use.
Valid values are: `NONE`, `LZ4`, `SNAPPY`.

Defaults to **"LZ4"**.

<a name="driver.query"></a>
### Driver Query Settings

Query-related settings.

#### -cl,--driver.query.consistency _&lt;string&gt;_

The consistency level to use for both loads and unloads.

Valid values are: `ANY`, `LOCAL_ONE`, `ONE`, `TWO`, `THREE`, `LOCAL_QUORUM`, `QUORUM`, `EACH_QUORUM`, `ALL`.

Defaults to **"LOCAL_ONE"**.

#### ---driver.query.fetchSize _&lt;number&gt;_

The page size. This controls how many rows will be retrieved simultaneously in a single network round trip (the goal being to avoid loading too many results in memory at the same time).

Only applicable in unload scenarios, ignored otherwise.

Defaults to **5000**.

#### ---driver.query.idempotence _&lt;boolean&gt;_

The default idempotence of statements generated by the loader.

Defaults to **true**.

#### ---driver.query.serialConsistency _&lt;string&gt;_

The serial consistency level to use for writes.

Valid values are: `SERIAL` and `LOCAL_SERIAL`.

Only applicable if the data is inserted using lightweight transactions, ignored otherwise.

Defaults to **"LOCAL_SERIAL"**.

<a name="driver.socket"></a>
### Driver Socket Settings

Socket-related settings.

#### ---driver.socket.readTimeout _&lt;string&gt;_

How long the driver waits for a request to complete. This is a global limit on the duration of a `session.execute()` call, including any internal retries the driver might do.

Defaults to **"12 seconds"**.

<a name="driver.ssl"></a>
### Driver Ssl Settings

Encryption-specific settings.

For more information about how to configure this section, see the Java Secure Socket Extension (JSSE) Reference Guide: http://docs.oracle.com/javase/6/docs/technotes/guides/security/jsse/JSSERefGuide.html. You can also check the DataStax Java driver documentation on SSL: http://docs.datastax.com/en/developer/java-driver-dse/latest/manual/ssl/

#### ---driver.ssl.cipherSuites _&lt;list&gt;_

The cipher suites to enable.

Example: `cipherSuites = ["TLS_RSA_WITH_AES_128_CBC_SHA", "TLS_RSA_WITH_AES_256_CBC_SHA"]`

This property is optional. If it is not present, the driver won't explicitly enable cipher suites, which according to the JDK documentation results in "a minimum quality of service".

Defaults to **[]**.

#### ---driver.ssl.keystore.algorithm _&lt;string&gt;_

The algorithm to use.

Valid values are: `SunX509`, `NewSunX509`.

Defaults to **"SunX509"**.

#### ---driver.ssl.keystore.password _&lt;string&gt;_

The keystore password.

Defaults to **""**.

#### ---driver.ssl.keystore.path _&lt;string&gt;_

The path of the keystore file.

This setting is optional. If left unspecified, no client authentication will be used.

Defaults to **""**.

#### ---driver.ssl.openssl.keyCertChain _&lt;string&gt;_

The path of the certificate chain file.

This setting is optional. If left unspecified, no client authentication will be used.

Defaults to **""**.

#### ---driver.ssl.openssl.privateKey _&lt;string&gt;_

The path of the private key file.

Defaults to **""**.

#### ---driver.ssl.provider _&lt;string&gt;_

The SSL provider to use.

Valid values are:

- **None**: no SSL.
- **JDK**: uses JDK's SSLContext
- **OpenSSL**: uses Netty's native support for OpenSSL

Using OpenSSL provides better performance and generates less garbage. A disadvantage of using the OpenSSL provider is that, unlike the JDK provider, it requires a platform-specific dependency, named `netty-tcnative`, which must be added manually to the loader's classpath (typically by dropping its jar in the lib subdirectory of the DSBulk archive).

Follow these instructions to find out how to add this dependency: http://netty.io/wiki/forked-tomcat-native.html

Defaults to **"None"**.

#### ---driver.ssl.truststore.algorithm _&lt;string&gt;_

The algorithm to use.

Valid values are: `PKIX`, `SunX509`.

Defaults to **"SunX509"**.

#### ---driver.ssl.truststore.password _&lt;string&gt;_

The truststore password.

Defaults to **""**.

#### ---driver.ssl.truststore.path _&lt;string&gt;_

The path of the truststore file.

This setting is optional. If left unspecified, server certificates will not be validated.

Defaults to **""**.

<a name="engine"></a>
## Engine Settings

Workflow Engine-specific settings.

#### ---engine.maxConcurrentReads _&lt;number&gt;_

The maximum number of range reads that can be issued concurrently for the same table.

Applies only to read workflows; ignored otherwise.

The special syntax `NC` can be used to specify a number of threads that is a multiple of the number of available cores, e.g. if the number of cores is 8, then 4C = 4 * 8 = 32 threads.

Defaults to **4**.

#### ---engine.maxMappingThreads _&lt;string&gt;_

The maximum number of threads to allocate for serializing and deserializing, as well as for mapping records or results.

Applies to both read and write workflows.

The special syntax `NC` can be used to specify a number of threads that is a multiple of the number of available cores, e.g. if the number of cores is 8, then 4C = 4 * 8 = 32 threads.

Defaults to **"1C"**.

<a name="executor"></a>
## Executor Settings

Executor-specific settings.

#### ---executor.maxPerSecond _&lt;number&gt;_

The maximum number of concurrent requests per second. This acts as a safeguard against workflows that could overwhelm the cluster with more requests than it can handle. Batch statements count for as many requests as their number of inner statements.

Setting this option to any negative value will disable it.

Defaults to **100000**.

#### ---executor.continuousPaging.maxPages _&lt;number&gt;_

The maximum number of pages to retrieve.

Setting this value to zero retrieves all pages available.

Defaults to **0**.

#### ---executor.continuousPaging.maxPagesPerSecond _&lt;number&gt;_

The maximum number of pages per second.

Setting this value to zero indicates no limit.

Defaults to **0**.

#### ---executor.continuousPaging.pageSize _&lt;number&gt;_

The size of the page. The unit to use is determined by the *executor.continuousPaging.pageUnit* setting.


Defaults to **5000**.

#### ---executor.continuousPaging.pageUnit _&lt;string&gt;_

The unit to use for the *executor.continuousPaging.pageSize* setting.

Possible values are: `ROWS`, `BYTES`.

Defaults to **"ROWS"**.

#### ---executor.maxInFlight _&lt;number&gt;_

The maximum number of "in-flight" requests. In other words, sets the maximum number of concurrent uncompleted futures waiting for a response from the server. This acts as a safeguard against workflows that generate more requests than they can handle. Batch statements count for as many requests as their number of inner statements.

Setting this option to any negative value will disable it.

Defaults to **1000**.

#### ---executor.maxThreads _&lt;string&gt;_

The maximum number of threads to allocate for the executor. These threads are used to submit requests and process responses.

The special syntax `NC` can be used to specify a number of threads that is a multiple of the number of available cores, e.g. if the number of cores is 8, then 4C = 4 * 8 = 32 threads.

Defaults to **"4C"**.

<a name="log"></a>
## Log Settings

Log and error management settings.

#### -maxErrors,--log.maxErrors _&lt;number&gt;_

The maximum number of errors to tolerate before aborting the entire operation.

Setting this value to `-1` disables this feature (not recommended).

Defaults to **100**.

#### -logDir,--log.directory _&lt;string&gt;_

The directory where all log files will be stored.

Note that this must be a path pointing to a writable directory.

Log files for a specific run will be located in a sub-directory inside the directory specified here. Each run generates a sub-directory identified by an "operation ID', which is basically a timestamp in the format: `yyyy_MM_dd_HH_mm_ss_nnnnnnnnn`.

Setting this value to `.` denotes the current working directory.

Defaults to **"."**.

#### ---log.maxThreads _&lt;string&gt;_

The maximum number of threads to allocate to log files management.

The special syntax `NC` can be used to specify a number of threads that is a multiple of the number of available cores, e.g. if the number of cores is 8, then 4C = 4 * 8 = 32 threads.

Defaults to **"4"**.

#### ---log.stmt.level _&lt;string&gt;_

The desired log level.

Possible values are:
- **ABRIDGED**: only prints basic information in summarized form.
- **NORMAL**: prints basic information in summarized form, and the statement's query string, if available. For batch statements, this verbosity level also prints information about the batch's inner statements.
- **EXTENDED**: prints full information, including the statement's query string, if available, and the statement's bound values, if available. For batch statements, this verbosity level also prints all information available about the batch's inner statements.

Defaults to **"EXTENDED"**.

#### ---log.stmt.maxBoundValueLength _&lt;number&gt;_

The maximum length for a bound value. Bound values longer than this value will be truncated.

Setting this value to `-1` disables this feature (not recommended).

Defaults to **50**.

#### ---log.stmt.maxBoundValues _&lt;number&gt;_

The maximum number of bound values to print. If the statement has more bound values than this limit, the exceeding values will not be printed.

Setting this value to `-1` disables this feature (not recommended).

Defaults to **50**.

#### ---log.stmt.maxInnerStatements _&lt;number&gt;_

The maximum number of inner statements to print for a batch statement.

Only applicable for batch statements, ignored otherwise.

If the batch statement has more children than this value, the exceeding child statements will not be printed.

Setting this value to `-1` disables this feature (not recommended).

Defaults to **10**.

#### ---log.stmt.maxQueryStringLength _&lt;number&gt;_

The maximum length for a query string. Query strings longer than this value will be truncated.

Setting this value to `-1` disables this feature (not recommended).

Defaults to **500**.

<a name="monitoring"></a>
## Monitoring Settings

Monitoring-specific settings.

#### -reportRate,--monitoring.reportRate _&lt;string&gt;_

The report interval for the console reporter.

The console reporter will print useful metrics about the ongoing operation at this rate.

Durations lesser than one second will be rounded up to 1 second.

Defaults to **"5 seconds"**.

#### ---monitoring.durationUnit _&lt;string&gt;_

The time unit to use when printing latency durations.

Valid values: all `TimeUnit` enum constants.

Defaults to **"MILLISECONDS"**.

#### ---monitoring.expectedReads _&lt;number&gt;_

The expected total number of reads.

This information is optional; if present, the console reporter will also print the the overall achievement percentage.

Setting this value to `-1` disables this feature.

Defaults to **-1**.

#### ---monitoring.expectedWrites _&lt;number&gt;_

The expected total number of writes.

This information is optional; if present, the console reporter will also print the the overall achievement percentage.

Setting this value to `-1` disables this feature.

Defaults to **-1**.

#### -jmx,--monitoring.jmx _&lt;boolean&gt;_

Whether or not to enable JMX reporting.

Note that to enable *remote* JMX reporting, several properties must also be set in the JVM during launch. This is accomplished via the `DSBULK_JAVA_OPTS` environment variable.

Defaults to **true**.

#### ---monitoring.rateUnit _&lt;string&gt;_

The time unit to use when printing throughput rates.

Valid values: all `TimeUnit` enum constants.

Defaults to **"SECONDS"**.
