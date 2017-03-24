import os

from controllers.PurchaseController import add_purchase_event, get_all_by_channel
from recommenders.Prod_Prod import Prod_Prod
from util.response_utils import products_list_from_response, zip_responses
from util.InvalidUsage import InvalidUsage
from neomodel import db

def connect_to_neo4j():
    NEO4J_USER = os.getenv('NEO4J_USER') # neo4j
    NEO4J_PASS = os.getenv('NEO4J_PASS') # password
    NEO4J_HOST = os.getenv('NEO4J_HOST') # localhost
    NEO4J_PORT = os.getenv('NEO4J_PORT') # 7687
    db.set_connection('bolt://%s:%s@%s:%s' % (NEO4J_USER, NEO4J_PASS, NEO4J_HOST, NEO4J_PORT))

def get_all_channels():
    query = "MATCH ()-[r]-() RETURN DISTINCT r.channel"
    channels, _ = db.cypher_query(query)
    return channels

def start_pprec_from_db(channel_id):
    pprec = Prod_Prod()
    for [cust_id, prod_id] in get_all_by_channel(channel_id):
        pprec.add_point(cust_id, prod_id)
    return pprec

class Prod_Prod_Manager(object):
    def __init__(self):
        self.recommenders = {}
        connect_to_neo4j()
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
        if channel_id < 0:
            raise InvalidUsage('Invalid Channel ID', status_code=400,
                               payload={'error_code': 100})
        elif channel_id not in self.recommenders.keys():
            raise InvalidUsage('Channel ID not found', status_code=400,
                               payload={'error_code': 101})
        else:
            if prod_id not in self.recommenders[channel_id].product_ids():
                raise InvalidUsage('Product ID not found in channel', status_code=400,
                                   payload={'error_code': 102})

    def recommend(self, prod_id, channel_id):
        self.validate(prod_id, channel_id)
        return self.recommenders[channel_id].recommend(prod_id)

    def recommend_full(self, prod_id, channel_id, es_client, from_param, size_param):
        recommender_output = self.recommend(prod_id, channel_id)
        es_resp = es_client.get_products_list(
            products_list_from_response(recommender_output)[from_param:(from_param + size_param)]
        )
        return zip_responses(recommender_output, es_resp)

    def add_point(self, cust_id, prod_id, channel_id):
        pprec = self.get_recommender(channel_id)
        pprec.add_point(cust_id, prod_id)
        add_purchase_event(cust_id, prod_id, channel_id)

    def train(self, points):
        for point in points:
            self.add_point(point['custID'], point['prodID'], point['chanID'])
