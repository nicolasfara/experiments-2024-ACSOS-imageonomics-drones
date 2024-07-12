import numpy as np
import xarray as xr
import re
import os
from pathlib import Path
import seaborn as sns
import collections


def distance(val, ref):
    return abs(ref - val)


vectDistance = np.vectorize(distance)


def cmap_xmap(function, cmap):
    """ Applies function, on the indices of colormap cmap. Beware, function
    should map the [0, 1] segment to itself, or you are in for surprises.

    See also cmap_xmap.
    """
    cdict = cmap._segmentdata
    function_to_map = lambda x: (function(x[0]), x[1], x[2])
    for key in ('red', 'green', 'blue'):
        cdict[key] = map(function_to_map, cdict[key])
    #        cdict[key].sort()
    #        assert (cdict[key][0]<0 or cdict[key][-1]>1), "Resulting indices extend out of the [0, 1] segment."
    return matplotlib.colors.LinearSegmentedColormap('colormap', cdict, 1024)


def getClosest(sortedMatrix, column, val):
    while len(sortedMatrix) > 3:
        half = int(len(sortedMatrix) / 2)
        sortedMatrix = sortedMatrix[-half - 1:] if sortedMatrix[half, column] < val else sortedMatrix[: half + 1]
    if len(sortedMatrix) == 1:
        result = sortedMatrix[0].copy()
        result[column] = val
        return result
    else:
        safecopy = sortedMatrix.copy()
        safecopy[:, column] = vectDistance(safecopy[:, column], val)
        minidx = np.argmin(safecopy[:, column])
        safecopy = safecopy[minidx, :].A1
        safecopy[column] = val
        return safecopy


def convert(column, samples, matrix):
    return np.matrix([getClosest(matrix, column, t) for t in samples])


def valueOrEmptySet(k, d):
    return (d[k] if isinstance(d[k], set) else {d[k]}) if k in d else set()


def mergeDicts(d1, d2):
    """
    Creates a new dictionary whose keys are the union of the keys of two
    dictionaries, and whose values are the union of values.

    Parameters
    ----------
    d1: dict
        dictionary whose values are sets
    d2: dict
        dictionary whose values are sets

    Returns
    -------
    dict
        A dict whose keys are the union of the keys of two dictionaries,
    and whose values are the union of values

    """
    res = {}
    for k in d1.keys() | d2.keys():
        res[k] = valueOrEmptySet(k, d1) | valueOrEmptySet(k, d2)
    return res


def extractCoordinates(filename):
    """
    Scans the header of an Alchemist file in search of the variables.

    Parameters
    ----------
    filename : str
        path to the target file
    mergewith : dict
        a dictionary whose dimensions will be merged with the returned one

    Returns
    -------
    dict
        A dictionary whose keys are strings (coordinate name) and values are
        lists (set of variable values)

    """
    with open(filename, 'r') as file:
        #        regex = re.compile(' (?P<varName>[a-zA-Z._-]+) = (?P<varValue>[-+]?\d*\.?\d+(?:[eE][-+]?\d+)?),?')
        regex = r"(?P<varName>[a-zA-Z._-]+) = (?P<varValue>[^,]*),?"
        dataBegin = r"\d"
        is_float = r"[-+]?\d*\.?\d+(?:[eE][-+]?\d+)?"
        for line in file:
            match = re.findall(regex, line.replace('Infinity', '1e30000'))
            if match:
                return {
                    var: float(value) if re.match(is_float, value)
                    else bool(re.match(r".*?true.*?", value.lower())) if re.match(r".*?(true|false).*?", value.lower())
                    else value
                    for var, value in match
                }
            elif re.match(dataBegin, line[0]):
                return {}


def extractVariableNames(filename):
    """
    Gets the variable names from the Alchemist data files header.

    Parameters
    ----------
    filename : str
        path to the target file

    Returns
    -------
    list of list
        A matrix with the values of the csv file

    """
    with open(filename, 'r') as file:
        dataBegin = re.compile('\d')
        lastHeaderLine = ''
        for line in file:
            if dataBegin.match(line[0]):
                break
            else:
                lastHeaderLine = line
        if lastHeaderLine:
            regex = re.compile(' (?P<varName>\S+)')
            return regex.findall(lastHeaderLine)
        return []


def openCsv(path):
    """
    Converts an Alchemist export file into a list of lists representing the matrix of values.

    Parameters
    ----------
    path : str
        path to the target file

    Returns
    -------
    list of list
        A matrix with the values of the csv file

    """
    regex = re.compile('\d')
    with open(path, 'r') as file:
        lines = filter(lambda x: regex.match(x[0]), file.readlines())
        return [[float(x) for x in line.split()] for line in lines]


def beautifyValue(v):
    """
    Converts an object to a better version for printing, in particular:
        - if the object converts to float, then its float value is used
        - if the object can be rounded to int, then the int value is preferred

    Parameters
    ----------
    v : object
        the object to try to beautify

    Returns
    -------
    object or float or int
        the beautified value
    """
    try:
        v = float(v)
        if v.is_integer():
            return int(v)
        return v
    except:
        return v


if __name__ == '__main__':
    # CONFIGURE SCRIPT
    # Where to find Alchemist data files
    directory = 'data'
    # Where to save charts
    output_directory = 'charts'
    # How to name the summary of the processed data
    pickleOutput = 'data_summary'
    # Experiment prefixes: one per experiment (root of the file name)
    experiments = ['experiment_export']
    floatPrecision = '{: 0.3f}'
    # Number of time samples 
    timeSamples = 100
    # time management
    minTime = 300
    maxTime = 1800 - minTime
    timeColumnName = 'time'
    logarithmicTime = False
    # One or more variables are considered random and "flattened"
    seedVars = ['Seed']


    # Label mapping
    class Measure:
        def __init__(self, description, unit=None):
            self.__description = description
            self.__unit = unit

        def description(self):
            return self.__description

        def unit(self):
            return '' if self.__unit is None else f'({self.__unit})'

        def derivative(self, new_description=None, new_unit=None):
            def cleanMathMode(s):
                return s[1:-1] if s[0] == '$' and s[-1] == '$' else s

            def deriveString(s):
                return r'$d ' + cleanMathMode(s) + r'/{dt}$'

            def deriveUnit(s):
                return f'${cleanMathMode(s)}' + '/{s}$' if s else None

            result = Measure(
                new_description if new_description else deriveString(self.__description),
                new_unit if new_unit else deriveUnit(self.__unit),
            )
            return result

        def __str__(self):
            return f'{self.description()} {self.unit()}'


    centrality_label = 'H_a(x)'


    def expected(x):
        return r'\mathbf{E}[' + x + ']'


    def stdev_of(x):
        return r'\sigma{}[' + x + ']'


    def mse(x):
        return 'MSE[' + x + ']'


    def cardinality(x):
        return r'\|' + x + r'\|'


    labels = {
        'nodeCount': Measure(r'$n$', 'nodes'),
        'harmonicCentrality[Mean]': Measure(f'${expected("H(x)")}$'),
        'meanNeighbors': Measure(f'${expected(cardinality("N"))}$', 'nodes'),
        'speed': Measure(r'$\|\vec{v}\|$', r'$m/s$'),
        'msqer@harmonicCentrality[Max]': Measure(r'$\max{(' + mse(centrality_label) + ')}$'),
        'msqer@harmonicCentrality[Min]': Measure(r'$\min{(' + mse(centrality_label) + ')}$'),
        'msqer@harmonicCentrality[Mean]': Measure(f'${expected(mse(centrality_label))}$'),
        'msqer@harmonicCentrality[StandardDeviation]': Measure(f'${stdev_of(mse(centrality_label))}$'),
        'org:protelis:tutorial:distanceTo[max]': Measure(r'$m$', 'max distance'),
        'org:protelis:tutorial:distanceTo[mean]': Measure(r'$m$', 'mean distance'),
        'org:protelis:tutorial:distanceTo[min]': Measure(r'$m$', ',min distance'),
    }


    def derivativeOrMeasure(variable_name):
        if variable_name.endswith('dt'):
            return labels.get(variable_name[:-2], Measure(variable_name)).derivative()
        return Measure(variable_name)


    def label_for(variable_name):
        return labels.get(variable_name, derivativeOrMeasure(variable_name)).description()


    def unit_for(variable_name):
        return str(labels.get(variable_name, derivativeOrMeasure(variable_name)))


    # Setup libraries
    np.set_printoptions(formatter={'float': floatPrecision.format})
    # Read the last time the data was processed, reprocess only if new data exists, otherwise just load
    import pickle
    import os

    if os.path.exists(directory):
        newestFileTime = max([os.path.getmtime(directory + '/' + file) for file in os.listdir(directory)], default=0.0)
        try:
            lastTimeProcessed = pickle.load(open('timeprocessed', 'rb'))
        except:
            lastTimeProcessed = -1
        shouldRecompute = not os.path.exists(".skip_data_process") and newestFileTime != lastTimeProcessed
        if not shouldRecompute:
            try:
                means = pickle.load(open(pickleOutput + '_mean', 'rb'))
                stdevs = pickle.load(open(pickleOutput + '_std', 'rb'))
            except:
                shouldRecompute = True
        if shouldRecompute:
            timefun = np.logspace if logarithmicTime else np.linspace
            means = {}
            stdevs = {}
            for experiment in experiments:
                # Collect all files for the experiment of interest
                import fnmatch

                allfiles = filter(lambda file: fnmatch.fnmatch(file, experiment + '_*.csv'), os.listdir(directory))
                allfiles = [directory + '/' + name for name in allfiles]
                allfiles.sort()
                # From the file name, extract the independent variables
                dimensions = {}
                for file in allfiles:
                    dimensions = mergeDicts(dimensions, extractCoordinates(file))
                dimensions = {k: sorted(v) for k, v in dimensions.items()}
                # Add time to the independent variables
                dimensions[timeColumnName] = range(0, timeSamples)
                # Compute the matrix shape
                shape = tuple(len(v) for k, v in dimensions.items())
                # Prepare the Dataset
                dataset = xr.Dataset()
                for k, v in dimensions.items():
                    dataset.coords[k] = v
                if len(allfiles) == 0:
                    print("WARNING: No data for experiment " + experiment)
                    means[experiment] = dataset
                    stdevs[experiment] = xr.Dataset()
                else:
                    varNames = extractVariableNames(allfiles[0])
                    for v in varNames:
                        if v != timeColumnName:
                            novals = np.ndarray(shape)
                            novals.fill(float('nan'))
                            dataset[v] = (dimensions.keys(), novals)
                    # Compute maximum and minimum time, create the resample
                    timeColumn = varNames.index(timeColumnName)
                    allData = {file: np.matrix(openCsv(file)) for file in allfiles}
                    computeMin = minTime is None
                    computeMax = maxTime is None
                    if computeMax:
                        maxTime = float('-inf')
                        for data in allData.values():
                            maxTime = max(maxTime, data[-1, timeColumn])
                    if computeMin:
                        minTime = float('inf')
                        for data in allData.values():
                            minTime = min(minTime, data[0, timeColumn])
                    timeline = timefun(minTime, maxTime, timeSamples)
                    # Resample
                    for file in allData:
                        #                    print(file)
                        allData[file] = convert(timeColumn, timeline, allData[file])
                    # Populate the dataset
                    for file, data in allData.items():
                        dataset[timeColumnName] = timeline
                        for idx, v in enumerate(varNames):
                            if v != timeColumnName:
                                darray = dataset[v]
                                experimentVars = extractCoordinates(file)
                                darray.loc[experimentVars] = data[:, idx].A1
                    # Fold the dataset along the seed variables, producing the mean and stdev datasets
                    mergingVariables = [seed for seed in seedVars if seed in dataset.coords]
                    means[experiment] = dataset.mean(dim=mergingVariables, skipna=True)
                    stdevs[experiment] = dataset.std(dim=mergingVariables, skipna=True)
            # Save the datasets
            pickle.dump(means, open(pickleOutput + '_mean', 'wb'), protocol=-1)
            pickle.dump(stdevs, open(pickleOutput + '_std', 'wb'), protocol=-1)
            pickle.dump(newestFileTime, open('timeprocessed', 'wb'))
    else:
        means = {experiment: xr.Dataset() for experiment in experiments}
        stdevs = {experiment: xr.Dataset() for experiment in experiments}

    # QUICK CHARTING

    import matplotlib
    import matplotlib.pyplot as plt
    import matplotlib.cm as cmx

    matplotlib.rcParams.update({'axes.titlesize': 12})
    matplotlib.rcParams.update({'axes.labelsize': 10})

    def make_line_chart(
            xdata,
            ydata,
            title=None,
            ylabel=None,
            xlabel=None,
            colors=None,
            linewidth=1,
            error_alpha=0.2,
            figure_size=(6, 4)
    ):
        fig = plt.figure(figsize=figure_size)
        ax = fig.add_subplot(1, 1, 1)
        ax.set_title(title)
        ax.set_xlabel(xlabel)
        ax.set_ylabel(ylabel)
        #        ax.set_ylim(0)
        #        ax.set_xlim(min(xdata), max(xdata))
        index = 0
        for (label, (data, error)) in ydata.items():
            #            print(f'plotting {data}\nagainst {xdata}')
            lines = ax.plot(xdata, data, label=label, color=colors(index / (len(ydata) - 1)) if colors else None,
                            linewidth=linewidth)
            index += 1
            if error is not None:
                last_color = lines[-1].get_color()
                ax.fill_between(
                    xdata,
                    data + error,
                    data - error,
                    facecolor=last_color,
                    alpha=error_alpha,
                )
        return (fig, ax)


    def generate_all_charts(means, errors=None, basedir=''):
        viable_coords = {coord for coord in means.coords if means[coord].size > 1}
        for comparison_variable in viable_coords - {timeColumnName}:
            mergeable_variables = viable_coords - {timeColumnName, comparison_variable}
            for current_coordinate in mergeable_variables:
                merge_variables = mergeable_variables - {current_coordinate}
                merge_data_view = means.mean(dim=merge_variables, skipna=True)
                merge_error_view = errors.mean(dim=merge_variables, skipna=True)
                for current_coordinate_value in merge_data_view[current_coordinate].values:
                    beautified_value = beautifyValue(current_coordinate_value)
                    for current_metric in merge_data_view.data_vars:
                        title = f'{label_for(current_metric)} for diverse {label_for(comparison_variable)} when {label_for(current_coordinate)}={beautified_value}'
                        for withErrors in [True, False]:
                            fig, ax = make_line_chart(
                                title=title,
                                xdata=merge_data_view[timeColumnName],
                                xlabel=unit_for(timeColumnName),
                                ylabel=unit_for(current_metric),
                                ydata={
                                    beautifyValue(label): (
                                        merge_data_view.sel(selector)[current_metric],
                                        merge_error_view.sel(selector)[current_metric] if withErrors else 0
                                    )
                                    for label in merge_data_view[comparison_variable].values
                                    for selector in
                                    [{comparison_variable: label, current_coordinate: current_coordinate_value}]
                                },
                            )
                            ax.set_xlim(minTime, maxTime)
                            ax.legend()
                            fig.tight_layout()
                            by_time_output_directory = f'{output_directory}/{basedir}/{comparison_variable}'
                            Path(by_time_output_directory).mkdir(parents=True, exist_ok=True)
                            figname = f'{comparison_variable}_{current_metric}_{current_coordinate}_{beautified_value}{"_err" if withErrors else ""}'
                            for symbol in r".[]\/@:":
                                figname = figname.replace(symbol, '_')
                            fig.savefig(f'{by_time_output_directory}/{figname}.pdf')
                            plt.close(fig)


    for experiment in experiments:
        current_experiment_means = means[experiment]
        current_experiment_errors = stdevs[experiment]
        generate_all_charts(current_experiment_means, current_experiment_errors, basedir=f'{experiment}/all')

    current_experiment = "experiment_export"

    # Custom charting
    dataset_means = means[current_experiment]
    dataset_stdevs = stdevs[current_experiment]

    plt.rc('text.latex', preamble=r'\usepackage{amsmath,amssymb,amsfonts,amssymb,graphicx}')
    plt.rcParams.update({"text.usetex": True})

    # Filter out the ff_linpro_ac algorithm
    # dataset_means = dataset_means.where(dataset_means["Algorithm"] != "ff_linpro_ac", drop=True)
    # dataset_stdevs = dataset_stdevs.where(dataset_stdevs["Algorithm"] != "ff_linpro_ac", drop=True)

    # Create the output directory for custom charts
    os.makedirs(f'{output_directory}/{current_experiment}/custom', exist_ok=True)

    metrics_labels_full = [
        "Body Coverage",
        "Body Coverage (Only Covered)",
        "Fov Distance",
        "Fov Distance (Only Covered)",
        "Noise Perceived",
    ]
    metrics_labels = ["Body Coverage", "Fov Distance", "Noise Perceived (normalized)"]

    viridis = plt.colormaps['viridis']
    palette={"bc_re_c": viridis(0.1), "ff_linpro": viridis(0.3), "ff_linpro_c": viridis(0.7), "sm_av": viridis(0.9) }

    def plot_metric_by_algorithm(dataset, cam_herd_ratio, number_of_herds, labels):
        fig, ax = plt.subplots(1, len(dataset.columns), figsize=(18, 4), sharey=False, layout="constrained")
        fig.suptitle(r"$\nu$=" + str(int(cam_herd_ratio)) + r" - $\zeta$=" + str(int(number_of_herds)), fontsize=20)

        custom_labels = {
            "Body Coverage": r"$\Diamond$",
            "Body Coverage (Only Covered)": r"$\Diamond$ (Only Covered)",
            "Fov Distance": r"$\Gamma$",
            "Fov Distance (Only Covered)": r"$\Gamma$ (Only Covered)",
            "Noise Perceived": r"$\rho$ (dB)"
        }

        for a, metric, label in zip(ax, dataset.columns, labels):
            sns.boxplot(dataset, ax=a, x="Algorithm", y=metric, palette=palette, hue="Algorithm")
            # a.set_title(label, fontsize=16)
            a.xaxis.grid(True)
            a.yaxis.grid(True)
            a.tick_params(labelsize=15, axis='x', rotation=45)
            # a.set(ylabel=label if metric != "NoisePerceived[mean]" else "Noise Perceived (dB)")
            a.xaxis.get_label().set_fontsize(17)
            a.yaxis.get_label().set_fontsize(15)
            a.set(ylabel=r"{}".format(custom_labels[label]))

        fig.savefig(
            f'{output_directory}/{current_experiment}/custom/metrics_by_algorithms_CamHerRatio={cam_herd_ratio}_NumberOfHerds={number_of_herds}.pdf')


    def plot_k_coverage_by_algorithm(dataset, errors, cam_herd_ratio, number_of_herds):
        fig, ax = plt.subplots(1, len(dataset.columns), figsize=(18, 4), sharey=False, layout="constrained")
        fig.suptitle(r"$\nu$=" + str(int(cam_herd_ratio)) + r" - $\zeta$=" + str(int(number_of_herds)), fontsize=20)

        plus_sigma = dataset + errors
        minus_sigma = dataset - errors
        # get time values from index
        time = np.arange(minTime, maxTime, (maxTime - minTime) / timeSamples)
        time[-1] = maxTime

        for a, k in zip(ax, dataset.columns):
            sns.lineplot(dataset, ax=a, x="time", y=k, palette=palette, hue="Algorithm")
            for i, algo in enumerate(dataset[k].index.get_level_values(0).unique()):
                a.fill_between(time, minus_sigma[k][algo], plus_sigma[k][algo], alpha=0.3,
                               color=palette[algo])
            # a.set_title(k, fontsize=16)
            a.xaxis.grid(True)
            a.yaxis.grid(True)
            a.tick_params(labelsize=15)
            a.set_ylim(0, 1)
            a.margins(x=0)
            a.xaxis.get_label().set_fontsize(17)
            a.yaxis.get_label().set_fontsize(17)
            a.set_xlim(minTime, maxTime)

        fig.savefig(
            f'{output_directory}/{current_experiment}/custom/k_coverage_by_algorithms_CamHerRatio={cam_herd_ratio}_NumberOfHerds={number_of_herds}.pdf')


    # Plot metrics by algorithms
    for cam_ratio in dataset_means["CamHerdRatio"].to_numpy():
        for num_herds in dataset_means["NumberOfHerds"].to_numpy():
            metrics_by_algorithms = dataset_means.sel(
                {"CamHerdRatio": cam_ratio, "NumberOfHerds": num_herds}
            )[["BodyCoverage[mean]", "BodyCoverageOnlyCovered[mean]", "FovDistance[mean]",
               "FovDistanceOnlyCovered[mean]", "NoisePerceived[mean]"]].to_dataframe()
            metrics_by_algorithms.drop(["CamHerdRatio", "NumberOfHerds"], axis=1, inplace=True)

            plot_metric_by_algorithm(metrics_by_algorithms, cam_ratio, num_herds, metrics_labels_full)

            k_coverage_by_algorithm = dataset_means.sel(
                {"CamHerdRatio": cam_ratio, "NumberOfHerds": num_herds}
            )[["1-coverage", "2-coverage", "3-coverage"]].to_dataframe()
            k_coverage_by_algorithm.drop(["CamHerdRatio", "NumberOfHerds"], axis=1, inplace=True)
            k_coverage_by_algorithm_errors = dataset_stdevs.sel(
                {"CamHerdRatio": cam_ratio, "NumberOfHerds": num_herds}
            )[["1-coverage", "2-coverage", "3-coverage"]].to_dataframe()
            k_coverage_by_algorithm_errors.drop(["CamHerdRatio", "NumberOfHerds"], axis=1, inplace=True)

            plot_k_coverage_by_algorithm(k_coverage_by_algorithm, k_coverage_by_algorithm_errors, cam_ratio, num_herds)

    # Aggregate metric plotting

    def global_metric(v):
        #     return v["1-coverage"] * ((v["BodyCoverageOnlyCovered[mean]"] + v["FovDistanceOnlyCovered[mean]"]) / 2) * (1 - v["NoisePerceivedNormalized[mean]"])
        return (v["BodyCoverage[mean]"] * v["FovDistance[mean]"]) * (1 - v["NoisePerceivedNormalized[mean]"])


    dataset_means = dataset_means.assign(GlobalMetric=global_metric)


    def plot_global_metric_by_algorithm(ds, cam_herd_ratio, number_of_herds):
        fig, ax = plt.subplots(1, 1, figsize=(6, 4), sharey=False, layout="constrained")

        # fig.suptitle("Global Performance", fontsize=20)
        sns.boxplot(ds, ax=ax, x="Algorithm", y="GlobalMetric", palette=palette, hue="Algorithm")
        ax.set_title(f"CamHerdRatio={cam_herd_ratio} - NumberOfHerds={number_of_herds}", fontsize=12)
        ax.xaxis.grid(True)
        ax.yaxis.grid(True)
        ax.set(ylabel=r"$G$")
        ax.xaxis.get_label().set_fontsize(10)

        fig.savefig(
            f'{output_directory}/{current_experiment}/custom/global_metric_by_algorithms_CamHerRatio={cam_herd_ratio}_NumberOfHerds={number_of_herds}.pdf')


    for cam_ratio in dataset_means["CamHerdRatio"].to_numpy():
        for num_herds in dataset_means["NumberOfHerds"].to_numpy():
            metrics_by_algorithms = dataset_means.sel(
                {"CamHerdRatio": cam_ratio, "NumberOfHerds": num_herds}
            )[["GlobalMetric"]].to_dataframe()
            metrics_by_algorithms.drop(["CamHerdRatio", "NumberOfHerds"], axis=1, inplace=True)

            plot_global_metric_by_algorithm(metrics_by_algorithms, cam_ratio, num_herds)

    #  Custom global metric plotting

    def plot_selected_global_charts(dataset):
        selections = [
            {"CamHerdRatio": 1.0, "NumberOfHerds": 2.0},
            {"CamHerdRatio": 2.0, "NumberOfHerds": 4.0},
            {"CamHerdRatio": 3.0, "NumberOfHerds": 8.0},
        ]
        fix, ax = plt.subplots(1, len(selections), figsize=(18, 4), sharey=False, layout="constrained")
        # fix.suptitle("Global Performance", fontsize=20)
        
        for a, selection in zip(ax, selections):
            ds = dataset.sel(selection)["GlobalMetric"].to_dataframe()
            ds.drop(["CamHerdRatio", "NumberOfHerds"], axis=1, inplace=True)
            sns.boxplot(ds, ax=a, x="Algorithm", y="GlobalMetric", palette=palette, hue="Algorithm")
            a.set_title(r"$\nu$=" + str(int(selection['CamHerdRatio'])) + r" - $\zeta$=" + str(int(selection['NumberOfHerds'])), fontsize=16)
            a.xaxis.grid(True)
            a.yaxis.grid(True)
            a.tick_params(labelsize=15)
            a.set(ylabel=r"$G$")
            a.xaxis.get_label().set_fontsize(17)
            a.yaxis.get_label().set_fontsize(17)

        fix.savefig(f'{output_directory}/{current_experiment}/custom/selected_global_metric_by_algorithms.pdf')

    plot_selected_global_charts(dataset_means)

    ###########################################################################

    # Geometric average per algorithm

    def plot_geometric_average_per_algorithm(ds, errors, labels):
        fig, ax = plt.subplots(1, len(ds.columns), figsize=(18, 4), sharey=False, layout="constrained")
        fig.suptitle(f"Geometric Average", fontsize=20)

        custom_labels = {
            "Body Coverage": r"$\Diamond$",
            "Fov Distance": r"$\Gamma$",
            "Noise Perceived (normalized)": r"$\rho$"
        }

        plus_sigma = ds + errors
        minus_sigma = ds - errors
        time = np.arange(minTime, maxTime, (maxTime - minTime) / timeSamples)
        time[-1] = maxTime

        for a, metric, label in zip(ax, ds.columns, labels):
            sns.lineplot(ds, ax=a, x="time", y=metric, palette=palette, hue="Algorithm")
            for i, algo in enumerate(ds[metric].index.get_level_values(0).unique()):
                a.fill_between(time, minus_sigma[metric][algo], plus_sigma[metric][algo], alpha=0.3,
                               color=palette[algo])
            # a.set_title(label, fontsize=16)
            a.xaxis.grid(True)
            a.yaxis.grid(True)
            a.tick_params(labelsize=15)
            a.set(ylabel=r"{}".format(custom_labels[label]))
            a.margins(x=0)
            a.xaxis.get_label().set_fontsize(17)
            a.yaxis.get_label().set_fontsize(17)
            a.set_xlim(minTime, maxTime)

        fig.savefig(f'{output_directory}/{current_experiment}/custom/geometric_average_by_algorithms.pdf')


    size = len(dataset_means["CamHerdRatio"]) * len(dataset_means["NumberOfHerds"])
    geometric_average_metrics = dataset_means[
        ["BodyCoverage[mean]", "FovDistance[mean]", "NoisePerceivedNormalized[mean]"]
    ].prod(dim=["CamHerdRatio", "NumberOfHerds"], skipna=True)
    dataset_geometric_mean = geometric_average_metrics ** (1 / size)
    dataset_geometric_mean = dataset_geometric_mean.to_dataframe()

    geometric_average_metrics_errors = dataset_stdevs[
        ["BodyCoverage[mean]", "FovDistance[mean]", "NoisePerceivedNormalized[mean]"]
    ].prod(dim=["CamHerdRatio", "NumberOfHerds"], skipna=True)
    dataset_geometric_mean_errors = geometric_average_metrics_errors ** (1 / size)
    dataset_geometric_mean_errors = dataset_geometric_mean_errors.to_dataframe()

    plot_geometric_average_per_algorithm(dataset_geometric_mean, dataset_geometric_mean_errors, metrics_labels)

    # plt.show()
