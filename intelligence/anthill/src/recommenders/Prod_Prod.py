from math import sqrt
import numpy as np
from scipy.sparse import csr_matrix

class Prod_Prod(object):
    """Prod_Prod
    a recommender based on products which have been purchased
    by the same customers
    """
    def __init__(self):
        self.events = set()
        self.up_to_date = False
        self.mat = csr_matrix(np.zeros(1))

    def add_point(self, cust_id, prod_id):
        """add_point
        takes and event 'cust_id purchased prod_id' and adds it to
        the recommender data.
        The sparse matrix needs to be recomputed before making more
        recommendations.
        """
        self.events.add((cust_id, prod_id))
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
        return np.array([1.0/sqrt(self.count(x)) for (_, x) in self.events])

    def count(self, prod_id):
        """how many customers have purchased product prod_id
        """
        return len([prod for (_, prod) in self.events if prod == prod_id])

    def is_empty(self):
        """is_empty
        """
        return len(self.events) == 0

    def product_ids(self):
        """prod_ids
        returns a list of product ids which have been purchased
        """
        return [prod_id for (_, prod_id) in self.events]

    def coords(self):
        """coords
        list of tuples (cust_id, prod_id) where cust_id has purchased prod_id
        """
        return (
            [cust_id for (cust_id, _) in self.events],
            [prod_id for (_, prod_id) in self.events]
        )

    def recommend(self, prod_ids, excludes=[]):
        """recommend
        returns a list of (prod_id, similarityScore)
        sorted in descending order.
            worst = 0 <= similarityScore <= 1 = best
        """
        if not self.up_to_date:
            self.make_matrix()

        scores = self.mat[:, prod_ids].toarray().mean(1)
        inds = np.argsort(scores)[::-1]
        products = [
            {'id': np.asscalar(x), 'score': np.asscalar(y)}
            for (x, y) in zip(inds, scores[inds])
            if x not in prod_ids
            if x not in excludes
            if y > 0
        ]

        return {'products': products}
