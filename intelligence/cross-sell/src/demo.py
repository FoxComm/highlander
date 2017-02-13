#! /usr/local/bin/python3

import sys
import numpy as np
import matplotlib.pyplot as plt
from scipy.sparse import csr_matrix
from Recommend import Recommend

rec = Recommend()

for line in sys.stdin:
    pair = line.split(' ')
    rec.addPoint(
        int(pair[0]),
        int(pair[1])
    )

print(rec.recommend(sys.argv[1]), range(5))

plt.imshow(rec.mat.toarray())
plt.show()
