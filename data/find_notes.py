import math
import sys
import os

import matplotlib.pyplot as plt
import numpy as np


def get_notes(fname):
    """
    Finds the notes in a file
    :param fname: file name
    :return: list of notes
    """
    notes = []
    with open(fname) as f:
        for line in f:
            line = line.strip()
            try:
                notes.append(float(line))
            except:
                pass
    return notes


def get_scale(notes, do_plot=False):
    # plot histogram where there are 200 bins between 0 and 100
    # centered around the midpoint
    note_max = 104
    num_bins = note_max * 4
    bars = plt.hist(notes, bins=num_bins, range=(0, note_max), align="mid")
    # for each whole number, count the number of notes in the range
    # around 0.5 of the whole number
    counts = []
    notes = []
    for note in range(note_max):
        bin_idx = round(num_bins * note / note_max)
        count = bars[0][bin_idx] + bars[0][bin_idx + 1] + bars[0][bin_idx - 1]
        counts.append(count)
        notes.append(note)

    # create histogram of counts
    counts_average = np.average(counts) + np.std(counts) / 3
    # find all the notes with counts above average
    notes_above_average = []
    for i in range(len(counts)):
        if counts[i] > counts_average:
            notes_above_average.append(notes[i])

    # find the notes with the two highest counts
    max_count = max(counts)
    max_count_idx = counts.index(max_count)
    second_max_count = 0
    second_max_count_idx = 0
    for i in range(len(counts)):
        if counts[i] > second_max_count and i != max_count_idx:
            second_max_count = counts[i]
            second_max_count_idx = i

    if do_plot:
        print(f"found {len(notes_above_average)} notes")
        print(notes_above_average, notes[max_count_idx], notes[second_max_count_idx])
        plt.xlabel("Note")
        plt.ylabel("Frequency")
        plt.show()

    return notes_above_average, notes[max_count_idx], notes[second_max_count_idx]


if len(sys.argv) > 1:
    print(get_scale(get_notes(sys.argv[1]), do_plot=True))
else:
    scales = [
        "ambrette",
        "benzoin",
        "bergamot",
        "labdanum",
        "neroli",
        "orris",
        "tolu",
        "vetiver",
        "ylang",
    ]

    for scale in scales:
        fname = scale + ".txt"
        notes = get_notes(fname)
        notes_in_scale, _, _ = get_scale(notes)
        drone_notes = get_notes(scale + "_drone.txt")
        _, drone1, drone2 = get_scale(drone_notes)
        notes_in_drone = [drone1, drone2]
        notes_in_scale = list(set(notes_in_scale) - set(notes_in_drone))
        notes_in_scale.sort()
        # print(scale)
        # print(f"number of notes: {len(notes_in_scale)}")
        # print(notes_in_drone)
        # print(notes_in_scale)
        print(f'scales.put("{scale}",{notes_in_drone+notes_in_scale});')
