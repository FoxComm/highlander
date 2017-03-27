import os

from controllers.PurchaseController import add_purchase_event, get_all_by_channel, get_customer_purchases
from recommenders.Prod_Prod import Prod_Prod
from util.response_utils import products_list_from_response, zip_responses
from util.InvalidUsage import InvalidUsage
from util.neo4j_utils import get_all_channels

def start_pprec_from_db(channel_id):
    pprec = Prod_Prod()
    for [cust_id, prod_id] in get_all_by_channel(channel_id):
        pprec.add_point(cust_id, prod_id)
    return pprec

class Prod_Prod_Manager(object):
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

    def validate(self, prod_id, channel_id):
        self.validate_channel(channel_id)
        self.validate_prod_id(prod_id, channel_id)

    def validate_channel(self, channel_id):
        if channel_id < 0:
            raise InvalidUsage('Invalid Channel ID', status_code=400,
                               payload={'error_code': 100})
        elif channel_id not in self.recommenders.keys():
            raise InvalidUsage('Channel ID not found', status_code=400,
                               payload={'error_code': 101})

    def validate_prod_id(self, prod_id, channel_id):
        if prod_id not in self.recommenders[channel_id].product_ids():
            raise InvalidUsage('Product ID not found in channel', status_code=400,
                               payload={'error_code': 102})

    def recommend(self, prod_id, channel_id):
        self.validate(prod_id, channel_id)
        return self.recommenders[channel_id].recommend([prod_id])

    def recommend_full(self, prod_id, channel_id, es_client, from_param, size_param):
        recommender_output = self.recommend(prod_id, channel_id)
        es_resp = es_client.get_products_list(
            products_list_from_response(recommender_output)[from_param:(from_param + size_param)]
        )
        return zip_responses(recommender_output, es_resp)

    def cust_recommend(self, cust_id, channel_id):
        self.validate_channel(channel_id)
        prod_ids = get_customer_purchases(cust_id, channel_id)
        return self.recommenders[channel_id].recommend(prod_ids)

    def cust_recommend_full(self, cust_id, channel_id, es_client, from_param, size_param):
        recommender_output = self.cust_recommend(cust_id, channel_id)
        es_resp = es_client.get_products_list(
            products_list_from_response(recommender_output)[from_param:(from_param + size_param)]
        )
        return zip_responses(recommender_output, es_resp)

    def add_point(self, cust_id, prod_id, channel_id):
        pprec = self.get_recommender(channel_id)
        pprec.add_point(cust_id, prod_id)
        add_purchase_event(cust_id, prod_id, channel_id)
        self.update_pprec(channel_id, pprec)

    def train(self, points):
        for point in points:
            self.add_point(point['custID'], point['prodID'], point['chanID'])
