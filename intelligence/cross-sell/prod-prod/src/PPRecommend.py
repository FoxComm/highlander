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

    def make_matrix(self, chan_id):
        """compute the similarity score matrix

        this only needs to be done once for each product purchased
        Optionally filters by channel id
        """
        filtered_events = set([event for event in self.events if event[2] == chan_id])

        # Return all events if nothing was filtered
        if set() == filtered_events:
            filtered_events = self.events

        A = csr_matrix((self.weights(filtered_events), self.coords(filtered_events)))
        self.mat = A.T.dot(A)
        self.up_to_date = True

    def weights(self, filtered_events):
        """weights
        these are the values to go in the sparse matrix so that the columns are
        l2 normalized.
        """
        return np.array([1.0/sqrt(self.count(x)) for (_, x, _) in filtered_events])

    def count(self, prod_id):
        """how many customers have purchased product prodID
        """
        return len([prod for (_, prod, _) in self.events if prod == prod_id])

    def coords(self, filtered_events):
        """coords
        list of tuples (cust_id, prod_id, chan_id) where cust_id has purchased prod_id
        """
        return (
            [cust_id for (cust_id, _, _) in filtered_events],
            [prod_id for (_, prod_id, _) in filtered_events]
        )

    def recommend(self, prod_id, chan_id):
        """recommend
        returns a list of (prod_id, similarityScore)
        sorted in descending order.
            worst = 0 <= similarityScore <= 1 = best
        """
        if ~(self.up_to_date):
            self.make_matrix(chan_id)

        v = self.mat[:, prod_id].toarray()
        inds = np.argsort(v.T[0])[::-1]
        out = {'products': [{'id': np.asscalar(x), 'score': np.asscalar(y)}
                            for (x, y) in zip(inds, v[inds].T[0])
                            if x != int(prod_id)]}

        return out
