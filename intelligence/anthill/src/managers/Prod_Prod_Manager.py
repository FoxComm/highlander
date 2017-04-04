from controllers.PurchaseController import add_purchase_event
from recommenders.Prod_Prod import Prod_Prod
from util.response_utils import products_list_from_response, zip_responses
from util.InvalidUsage import InvalidUsage
from util.neo4j_utils import (
    get_all_channels,
    get_purchased_products,
    get_declined_products,
    get_all_by_channel
)

def start_pprec_from_db(channel_id):
    """start_pprec_from_db
    finds all channels used in neo4j, and starts up
    prod-prod recommenders for each one channel
    """
    pprec = Prod_Prod()
    for [cust_id, prod_id] in get_all_by_channel(channel_id):
        pprec.add_point(cust_id, prod_id)
    return pprec

class Prod_Prod_Manager(object):
    """Prod_Prod_Manager
    provides an interface for several Prod_Prod recommenders
    """
    def __init__(self):
        self.recommenders = {}
        for [channel_id] in get_all_channels():
            self.update_pprec(channel_id, start_pprec_from_db(channel_id))

    def get_recommender(self, channel_id):
        """get_pprec
        Return Prod_Prod object at channel_id if found else create new Prod_Prod
        """
        if channel_id in self.recommenders.keys():
            return self.recommenders[channel_id]
        else:
            return Prod_Prod()

    def update_pprec(self, channel_id, pprec):
        """update_pprec
        Update the Prod_Prod dictionary for the given channel_id
        """
        self.recommenders[channel_id] = pprec

    def validate_channel(self, channel_id):
        """validate_channel
        """
        if channel_id < 0:
            raise InvalidUsage('Invalid Channel ID', status_code=400,
                               payload={'error_code': 100})
        elif channel_id not in self.recommenders.keys():
            raise InvalidUsage('Channel ID not found', status_code=400,
                               payload={'error_code': 101})

    def recommend(self, prod_id, channel_id):
        """recommend
        take a product id
        get list of product ids from the recommender
        """
        self.validate_channel(channel_id)
        rec = self.get_recommender(channel_id)
        if prod_id in rec.product_ids():
            return rec.recommend([prod_id])
        else:
            return {'products': []}

    def recommend_full(self, prod_id, channel_id, es_client, from_param, size_param):
        """recommend_full
        take a product id
        get a list of full products from elasticsearch based on
        product ids from the recommender
        """
        recommender_output = self.recommend(prod_id, channel_id)
        es_resp = es_client.get_products_list(
            products_list_from_response(recommender_output)[from_param:(from_param + size_param)]
        )
        return zip_responses(recommender_output, es_resp)

    def cust_recommend(self, cust_id, channel_id):
        """cust_recommend
        take a customer id
        get list of product ids from the recommender
        """
        self.validate_channel(channel_id)
        prod_ids = get_purchased_products(cust_id, channel_id)
        excludes = get_declined_products(cust_id)
        return self.recommenders[channel_id].recommend(prod_ids, excludes)

    def cust_recommend_full(self, cust_id, channel_id, es_client, from_param, size_param):
        """cust_recommend_full
        get a list of full products from elasticsearch based on
        product ids from the recommender
        """
        recommender_output = self.cust_recommend(cust_id, channel_id)
        es_resp = es_client.get_products_list(
            products_list_from_response(recommender_output)[from_param:(from_param + size_param)]
        )
        return zip_responses(recommender_output, es_resp)

    def add_point(self, cust_id, prod_id, channel_id):
        """add_point
        add a purchase event to the recommender and to neo4j
        """
        pprec = self.get_recommender(channel_id)
        pprec.add_point(cust_id, prod_id)
        add_purchase_event(cust_id, prod_id, channel_id)
        self.update_pprec(channel_id, pprec)

    def train(self, points):
        """train
        train a recommender with a set of purchase events
        """
        for point in points:
            self.add_point(point['custID'], point['prodID'], point['chanID'])
