from math import sqrt
import numpy as np
from scipy.sparse import csr_matrix

class PPRecommend(object):
    def __init__(self):
        self.events = set()
        self.up_to_date = False

    def add_point(self, cust_id, prod_id, chan_id):
        """add_point
        takes and event 'custID purchased prodID, channel ID' and adds it to
        the recommender data.
        The sparse matrix needs to be recomputed before making more
        recommendations.
        """
        self.events.add((cust_id, prod_id, chan_id))
        self.up_to_date = False

    def make_matrix(self):
        """compute the similarity score matrix

        this only needs to be done once for each product purchased
        """
        # Return all events if nothing was filtered
        A = csr_matrix((self.weights(), self.coords()))
        self.mat = A.T.dot(A)
        self.up_to_date = True

    def weights(self):
        """weights
        these are the values to go in the sparse matrix so that the columns are
        l2 normalized.
        """
        return np.array([1.0/sqrt(self.count(x)) for (_, x, _) in self.events])

    def count(self, prod_id):
        """how many customers have purchased product prodID
        """
        return len([prod for (_, prod, _) in self.events if prod == prod_id])

    def is_empty(self):
        return len(self.events) == 0

    def product_ids(self):
        return [prod_id for (_, prod_id, _) in self.events]

    def coords(self):
        """coords
        list of tuples (cust_id, prod_id, chan_id) where cust_id has purchased prod_id
        """
        return (
            [cust_id for (cust_id, _, _) in self.events],
            [prod_id for (_, prod_id, _) in self.events]
        )

    def recommend(self, prod_id):
        """recommend
        returns a list of (prod_id, similarityScore)
        sorted in descending order.
            worst = 0 <= similarityScore <= 1 = best
        """
        if ~(self.up_to_date):
            self.make_matrix()

        v = self.mat[:, prod_id].toarray()
        inds = np.argsort(v.T[0])[::-1]
        out = {'products': [{'id': np.asscalar(x), 'score': np.asscalar(y)}
                            for (x, y) in zip(inds, v[inds].T[0])
                            if x != int(prod_id)]}

        return out
