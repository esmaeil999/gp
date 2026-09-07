# JForex Tick Exporter

Exports historical Dukascopy tick data to CSV using the JForex SDK and runs automatically from GitHub Actions.

Dukascopy's JForex SDK can be used without running the JForex desktop platform. The SDK connects directly to Dukascopy servers.

## Project structure

```text
jforex-tick-exporter/
├── .github/
│   └── workflows/
│       └── export-ticks.yml
├── src/
│   └── main/
│       └── java/
│           └── com/
│               └── esmaeil/
│                   └── jforex/
│                       ├── Main.java
│                       ├── ExportConfig.java
│                       └── TickExporter.java
├── .gitignore
├── pom.xml
└── README.md
```

## 1. Create the GitHub repository

Create a repository, for example:

```text
jforex-tick-exporter
```

Upload the project files.

## 2. Add Dukascopy credentials

Go to:

```text
Repository
→ Settings
→ Secrets and variables
→ Actions
→ New repository secret
```

Create:

```text
JFOREX_USERNAME
JFOREX_PASSWORD
```

Do not put your username/password directly in the source code.

## 3. Run from GitHub Actions

Open:

```text
Actions
→ Export Dukascopy Tick Data
→ Run workflow
```

Example:

```text
instrument: EURUSD
from:       2010-01-01T00:00:00Z
to:         2020-01-01T00:00:00Z
chunk_hours: 6
```

The workflow then:

1. Installs Java 11.
2. Downloads the JForex API dependency with Maven.
3. Builds the application.
4. Connects to Dukascopy.
5. Downloads historical ticks.
6. Writes the CSV.
7. Creates a ZIP.
8. Uploads both files as GitHub Actions artifacts.

## Date format

Use UTC ISO-8601:

```text
2010-01-01T00:00:00Z
```

The exported column is:

```text
GmtTime
```

and is formatted as:

```text
yyyy-MM-dd HH:mm:ss.SSS
```

## Output

Example:

```text
output/EURUSD_ticks.csv
output/EURUSD_ticks.zip
```

## Important memory note

The exporter intentionally keeps the original `getTicks()` approach, but uses relatively small chunks.

Dukascopy documents that `getTicks()` returns a `List<ITick>` and warns that very large tick requests can consume a lot of memory.

If GitHub Actions runs out of memory, reduce:

```text
chunk_hours = 6
```

to:

```text
chunk_hours = 1
```

or even:

```text
chunk_hours = 0.5
```

For very large historical ranges, a streaming/asynchronous implementation is preferable.

## Important file-size note

A 10-year EURUSD tick dataset can become extremely large.

GitHub Actions artifacts are not intended to be a permanent large-data storage system. For very large datasets, use the workflow only for generation and then upload the ZIP to external object/file storage.

## Local build

Requires Java 11 and Maven.

```bash
mvn -B -DskipTests package
```

Set environment variables:

```text
JFOREX_USERNAME
JFOREX_PASSWORD
INSTRUMENT
FROM
TO
CHUNK_HOURS
OUTPUT_FILE
MAX_RUNTIME_MINUTES
```

Then run:

```bash
java -jar target/jforex-tick-exporter-1.0.0.jar
```
