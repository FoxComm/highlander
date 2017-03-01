#! /usr/local/bin/python3

import random as r
import numpy.random as npr
import numpy as np
import sys
import itertools 
import json
from itertools import count, combinations

asJSON = True
nProductGroups = 6
productGroupSize = 6
lastStartIndex = (nProductGroups - 1) * productGroupSize + 1

def pickProductsFromGroup(ind):
    quantity = r.randint(1, productGroupSize)
    return npr.choice(range(ind, ind + productGroupSize), quantity, replace=False)

def getProducts(pair):
    return itertools.chain(
        pickProductsFromGroup(pair[0]),
        pickProductsFromGroup(pair[1])
    )

custPrefs = zip(count(start=1), map(
    getProducts, 
    combinations(range(1, lastStartIndex + 1, productGroupSize), 2)
))

if asJSON:
    points = {'points': [
        {'custID': c, 'prodID': np.asscalar(p)} for c, ps in custPrefs for p in ps 
    ]}
    print(json.dumps(points))
else:
    for c, ps in custPrefs:
        for p in ps:
            print(c, p)
