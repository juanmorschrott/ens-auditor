# ENS Auditor

Command-line tool to evaluate AWS infrastructure compliance with the Spanish National Security Scheme (ENS) and CCN-STIC 887.

**[Documentation and downloads](https://juanmorschrott.github.io/ens-auditor/) · [GitHub Releases](https://github.com/juanmorschrott/ens-auditor/releases)**

## Requirements

- Java 25+ (or GraalVM 25+ for native image)
- Maven 3.9+
- AWS credentials configured via the standard SDK chain

## Quick start

```bash
# Build
./mvnw clean install

# Run an audit (table output)
java -jar target/ens-auditor-0.1.0-SNAPSHOT.jar audit

# Run with JSON output saved to file
java -jar target/ens-auditor-0.1.0-SNAPSHOT.jar audit --output json --output-file report.json

# List available controls
java -jar target/ens-auditor-0.1.0-SNAPSHOT.jar list-controls
```

### Build native executable (GraalVM)

```bash
./mvnw -Pnative package -DskipTests

./target/ens-auditor audit --output table
./target/ens-auditor --help
```

## Downloads

Pre-built native binaries are available on [GitHub Releases](https://github.com/juanmorschrott/ens-auditor/releases):

| Platform              | Binary                          |
|-----------------------|---------------------------------|
| Linux (amd64)         | `ens-auditor-linux-amd64`       |
| Linux (arm64)         | `ens-auditor-linux-arm64`       |
| macOS (Apple Silicon) | `ens-auditor-macos-arm64`       |
| macOS (Intel)         | `ens-auditor-macos-amd64`       |
| Windows (amd64)       | `ens-auditor-windows-amd64.exe` |

## Commands

| Command               | Description                                 |
|-----------------------|---------------------------------------------|
| `audit`               | Run compliance audit on AWS infrastructure  |
| `list-controls`       | List available ENS controls                 |
| `status`              | Show audit status and compliance summary    |
| `generate-completion` | Generate shell completion script (bash/zsh) |

### `audit` options

| Option                     | Description                                                       |
|----------------------------|-------------------------------------------------------------------|
| `-o, --output <format>`    | Output format: `json`, `table`, `html`, `csv` (default: `table`) |
| `-c, --control <id>`       | Evaluate a specific control ID only                               |
| `-s, --severity <level>`   | Filter by minimum severity: `CRITICAL`, `HIGH`, `MEDIUM`, `LOW`  |
| `-f, --output-file <path>` | Write report to file instead of stdout                            |

### Exit codes

| Code | Meaning                      |
|------|------------------------------|
| `0`  | All controls compliant       |
| `1`  | One or more controls failed  |
| `10` | Error fetching AWS resources |
| `11` | Error evaluating controls    |
| `12` | AWS SDK / cloud error        |

## AWS setup

Configure credentials using any of the standard AWS SDK methods:

- Environment variables: `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_REGION`
- Credentials file: `~/.aws/credentials` / `~/.aws/config`
- IAM role, SSO, or federated login

## Pipeline integration

### GitHub Actions

```yaml
- name: Run ENS Audit
  run: ./ens-auditor audit --output json --output-file ens-report.json

- name: Upload audit report
  if: always()
  uses: actions/upload-artifact@v5
  with:
    name: ens-compliance-report
    path: ens-report.json
```

### GitLab CI

```yaml
ens-auditor:
  script:
    - ./ens-auditor audit --output html --output-file report.html
  artifacts:
    paths:
      - report.html
    when: always
```

## Tab completion

```bash
# bash
ens-auditor generate-completion > /etc/bash_completion.d/ens-auditor

# zsh (add to .zshrc)
eval "$(ens-auditor generate-completion)"
```

## Deploy

All CI and release logic lives in a single workflow: `.github/workflows/ci.yml`.

| Event                    | What happens                                                          |
|--------------------------|-----------------------------------------------------------------------|
| Pull request to `main`   | Build JAR + run tests on all 5 platforms                              |
| Push of a `v*` tag       | Build JAR + tests + native image on all 5 platforms + GitHub Release  |
| Manual `workflow_dispatch` | Same as tag push                                                    |

To publish a new release:

```bash
git tag v0.2.0
git push origin v0.2.0
```

The workflow builds the native image for each platform and creates a GitHub Release with all binaries attached.

## ENS control coverage

Controls are defined in `src/main/resources/ens-controls.yaml`. Each control specifies an `id`, `severity`, `affected_resources`, and the evaluator responsible for it.

Current automated coverage:

| Evaluator      | Controls                                                           |
|----------------|--------------------------------------------------------------------|
| `S3Evaluator`  | Encryption, public access block, versioning, access logging        |
| `RdsEvaluator` | Encryption, audit logging, IAM auth, multi-AZ, backups, KMS key   |
| `IamEvaluator` | MFA enforcement, least-privilege checks, unused credentials        |

Only controls automatable via the AWS API are included. Non-automatable requirements (governance, risk assessments, incident workflows) must be tracked as manual evidence.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for setup instructions and pull request guidelines.

## License

MIT — see [LICENSE](LICENSE).
