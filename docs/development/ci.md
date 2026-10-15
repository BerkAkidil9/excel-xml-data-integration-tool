# CI and Local Runner Operations

The [Verify workflow](../../.github/workflows/verify.yml) runs directly on the owner's Mac using two macOS ARM64 self-hosted GitHub Actions runners. This setup was installed and verified on 2026-10-14. GitHub schedules jobs and displays their logs and results; compilation, tests, packaging, and CLI processes run on the Mac.

## Triggers and jobs

| Trigger | Behavior |
|---|---|
| Push to `main` | Runs verification for the pushed commit |
| Pull request from a branch in this repository | Runs verification; there is no target-branch filter |
| Pull request from a fork | Jobs are skipped by the repository-origin condition |
| Manual `workflow_dispatch` | Runs verification for the selected ref |

The workflow grants `contents: read`, disables persisted checkout credentials, and cancels an older run for the same pull request or ref when a replacement run starts.

| Job | Work | Dependency |
|---|---|---|
| `Tests` (`tests`) | Java 21 setup and full Maven Surefire suite | None |
| `Package and CLI` (`package`) | Java 21 setup, `-DskipTests verify`, and real jar template/conversion commands | None |
| `verify` | Requires both job results to be `success` | `tests` and `package` |

The first two jobs can run concurrently, one per runner. The final job preserves the `verify` check name and runs after both finish, including when one fails. The first two jobs have a 20-minute execution timeout; the final job has a five-minute timeout. These are execution limits, not promises about queue time.

All jobs use `runs-on: [self-hosted, macOS, ARM64, excel-xml-ci]`. There are two execution slots for this repository. The other projects' runners remain registered to their own repositories and do not accept this workflow's jobs.

## Runner installations

| Runner name | Installation directory | Repository |
|---|---|---|
| `excel-xml-ci-1` | `~/.github-runners/excel-xml-ci-1` | `BerkAkidil9/excel-xml-data-integration-tool` |
| `excel-xml-ci-2` | `~/.github-runners/excel-xml-ci-2` | `BerkAkidil9/excel-xml-data-integration-tool` |

Each runner is installed as a macOS user LaunchAgent using the runner's `svc.sh`. Services start with the user's login session. The Mac must be awake, online, and running the services to accept jobs; queued work waits while runners are offline. No Docker container or GitHub-hosted build machine is used.

Run these commands from the relevant runner directory, for example:

```bash
cd ~/.github-runners/excel-xml-ci-1
./svc.sh status
```

To stop or restart that runner:

```bash
./svc.sh stop
./svc.sh start
```

Repeat in `~/.github-runners/excel-xml-ci-2` when managing the second service. Stopping a runner that is busy can interrupt its job.

## Java, caches, and outputs

`actions/setup-java` supplies Temurin Java 21 to each job. Maven and its version are provided by the checked-in wrapper. The first run downloads the JDK and uncached dependencies; later jobs can reuse the runner's local files.

Both Maven commands set:

```text
-Dmaven.repo.local="$RUNNER_WORKSPACE/.m2/repository"
```

This keeps the Maven dependency repository separate for each runner and avoids using the developer's default `~/.m2/repository`. The workflow does not use the GitHub Actions Maven cache service.

Within the checkout used by a job:

- `target/surefire-reports/` contains JUnit XML and text reports from `Tests`.
- `target/excel-xml-data-integration-tool-0.1.0-SNAPSHOT-app.jar` is the runnable jar from `Package and CLI`.
- `target/ci-smoke/` contains the generated template, converted XML/workbook, and round-trip XML from the packaged CLI checks.

On a runner's standard installation, the checkout is at `~/.github-runners/<runner-name>/_work/excel-xml-data-integration-tool/excel-xml-data-integration-tool/`. A job may run on either runner, so consult its Actions log for the runner name before locating an output.

Checkout cleanup can remove these outputs on the next job. The workflow does not upload artifacts or maintain a separate report archive. GitHub Actions retains the job logs according to the repository's retention settings.

## View or run CI

Use the repository's [Actions page](https://github.com/BerkAkidil9/excel-xml-data-integration-tool/actions/workflows/verify.yml). The [first successful local-runner execution](https://github.com/BerkAkidil9/excel-xml-data-integration-tool/actions/runs/38626983391) shows `Tests` on `excel-xml-ci-2` and `Package and CLI` on `excel-xml-ci-1`, starting at the same time. Runner assignment can change on later runs.

With GitHub CLI authenticated, run from the repository checkout:

```bash
gh workflow run verify.yml --ref main
gh run list --workflow verify.yml
```

For ordinary local verification without the runner service:

```bash
./mvnw verify
```

This runs Maven locally but does not itself create a GitHub Actions run or execute the extra packaged CLI checks. See the [README](../../README.md#run) for standalone jar commands.

## Troubleshooting and scope

- **Queued job:** Check the repository's runner settings and both `svc.sh status` outputs. An online runner can still be busy; labels must match `self-hosted`, `macOS`, `ARM64`, and `excel-xml-ci`.
- **Java/build failure:** Read the failed step's log. Enforcer requires Java 21 and Maven 3.9.0 or later; the wrapper currently selects Maven 3.9.11.
- **Failed `verify` result:** Inspect `Tests` and `Package and CLI`. The final job only aggregates their results; it does not rerun them.
- **Missing local outputs:** Check which runner executed the job and whether another checkout has already cleaned its working directory.

This workflow performs CI verification. It does not publish a release, install or open the Swing application, deploy a service, run dependency vulnerability scanning, or enforce coverage/benchmark thresholds. The [testing strategy](testing-strategy.md) describes what the checks prove and their limits.
