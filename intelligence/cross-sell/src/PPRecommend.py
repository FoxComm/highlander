from math import sqrt
import numpy as np
from scipy.sparse import csr_matrix

class PPRecommend(object):
    def __init__(self):
        self.events = set()
        self.upToDate = False

    def addPoint(self, custID, prodID):
        """addPoint
        takes and event 'custID purchased prodID' and adds it to
        the recommender data.
        The sparse matrix needs to be recomputed before making more
        recommendations.
        """
        self.events.add((custID, prodID))
        self.upToDate = False

    def makeMatrix(self):
        """compute the similarity score matrix

        this only needs to be done once for each product purchased
        """
        A = csr_matrix((self.weights(), self.coords()))
        self.mat = A.T.dot(A)
        self.upToDate = True

    def weights(self):
        """weights
        these are the values to go in the sparse matrix so that the columns are
        l2 normalized. 
        """
        return np.array([1.0/sqrt(self.count(x)) for (_, x) in self.events])

    def count(self, prodID):
        """how many customers have purchased product prodID
        """
        return len([prod for (_, prod) in self.events if prod == prodID])

    def coords(self):
        """list of pairs (custID, prodID) where custID has purchased prodID
        """
        return (
            [custID for (custID, _) in self.events],
            [prodID for (_, prodID) in self.events]
        )

    def recommend(self, prodID):
        """recommend
        returns a list of (prodID, similarityScore)
        sorted in descending order.
            worst = 0 <= similarityScore <= 1 = best
        """
        if ~(self.upToDate):
            self.makeMatrix()

        v = self.mat[:, prodID].toarray()
        inds = np.argsort(v.T[0])[::-1]
        out = {'products': [{'id': np.asscalar(x), 'score': np.asscalar(y)}
                for (x, y) in zip(inds, v[inds].T[0])
                if x != int(prodID)]}

        return out
