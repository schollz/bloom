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
            notes.append(float(line))
    return notes


def find_most_frequent(notes):
    # plot histogram where there are 200 bins between 0 and 100
    # centered around the midpoint
    num_bins = 400
    bars = plt.hist(notes, bins=num_bins, range=(0, 100), align="mid")
    # for each whole number, count the number of notes in the range
    # around 0.5 of the whole number
    counts = []
    notes = []
    for note in range(100):
        bin_idx = round(num_bins * note / 100)
        count = bars[0][bin_idx] + bars[0][bin_idx + 1] + bars[0][bin_idx - 1]
        counts.append(count)
        notes.append(note)

    # create histogram of counts
    counts_average = np.average(counts) + np.std(counts) / 2
    # find all the notes with counts above average
    notes_above_average = []
    for i in range(len(counts)):
        if counts[i] > counts_average:
            notes_above_average.append(notes[i])

    print(notes_above_average)

    # title the figure with name of file
    plt.title(os.path.basename(sys.argv[1]))
    # xlabel is the note
    plt.xlabel("Note")
    plt.ylabel("Frequency")
    plt.show()


notes = get_notes(sys.argv[1])
print(notes)
find_most_frequent(notes)
