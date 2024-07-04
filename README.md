# Decentralized Multi-Drone Coordination for Wildlife Video Acquisition

In this repository, we provide the code and data to reproduce the experiments
for the ACSOS 2024 paper "Decentralized Multi-Drone Coordination for Wildlife Video Acquisition".

## Reproduce the entire experiment

**WARNING**: re-running the whole experiment may take a very long time on a normal computer.
However, in the [data](./data) folder (~500MB of data, a good internet connection is recommended), we provided the generated data from the experiment,
so that a more lightweight process can be used to generate the charts.

### Reproduce with containers (recommended)

1. Install [docker](https://docs.docker.com/engine/install/) and [docker-compose](https://docs.docker.com/compose/install/linux/)
2. Run `docker-compose up`
3. The charts will be available in the `charts` folder.

### Reproduce natively

1. Install a Gradle-compatible version of Java.
  Use the [Gradle/Java compatibility matrix](https://docs.gradle.org/current/userguide/compatibility.html)
  to learn which is the compatible version range.
  The version of Gradle used in this experiment can be found in the `gradle-wrapper.properties` file
  located in the `gradle/wrapper` folder.
2. Install the version of Python indicated in `.python-version` (or use [pyenv](https://github.com/pyenv/pyenv)).
3. Launch either:
    - `./gradlew runAllBatch` on Linux, MacOS, or Windows if a bash-compatible shell is available;
    - `gradlew.bat runAllBatch` on Windows cmd or Powershell;
4. LaTeX is required to generate the charts since special symbols are used in the charts. You can skip this step if you already have LaTeX installed.
    - On Ubuntu, run `sudo apt-get install texlive-latex-base texlive-fonts-recommended texlive-fonts-extra texlive-latex-extra`
    - On Arch Linux, run `sudo pacman -S texlive`
    - On MacOS, install via [brew](https://formulae.brew.sh/) `brew install texlive`, or install MacTeX from [https://www.tug.org/mactex/](https://www.tug.org/mactex/)
    - On Windows, install MiKTeX from [https://miktex.org/](https://miktex.org/)
5. the results will be available in the `data` folder once the experiment is finished. Run:
    - `pip install --upgrade pip`
    - `pip install -r requirements.txt`
    - `python process.py`
6. The charts will be available in the `charts` folder.

## Inspect a single experiment

Follow the instructions for reproducing the entire experiment natively, but instead of running `runAllBatch`,
run `runEXPERIMENTGraphics`, replacing `EXPERIMENT` with the name of the experiment you want to run
(namely, with the name of the YAML simulation file).

If in doubt, run `./gradlew tasks` to see the list of available tasks.

To make changes to existing experiments and explore/reuse,
we recommend using the IntelliJ Idea IDE.
Opening the project in IntelliJ Idea will automatically import the project, download the dependencies,
and allow for a smooth development experience.

## Regenerate the charts

We keep a copy of the data in this repository,
so that the charts can be regenerated without having to run the experiment again.
To regenerate the charts, run `docker compose run --no-deps charts`.
Alternatively, follow the steps or the "reproduce natively" section,
starting after the part describing how to re-launch the simulations.
