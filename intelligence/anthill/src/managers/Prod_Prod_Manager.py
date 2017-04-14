from recommenders.Prod_Prod import Prod_Prod
from util.InvalidUsage import InvalidUsage
from util.response_utils import (
    products_list_from_response,
    zip_responses,
    format_es_response
)
from util.neo4j_utils import (
    add_purchase_event,
    get_all_channels,
    get_purchased_products,
    get_declined_products,
    get_all_by_channel,
    get_popular_products
)

EMPTY = {'products': []}

def start_pprec_from_db(channel_id, neo4j_client):
    """start_pprec_from_db
    finds all channels used in neo4j, and starts up
    prod-prod recommenders for each one channel
    """
    pprec = Prod_Prod()
    for [cust_id, prod_id] in get_all_by_channel(channel_id, neo4j_client):
        pprec.add_point(cust_id, prod_id)
    return pprec

class Prod_Prod_Manager(object):
    """Prod_Prod_Manager
    provides an interface for several Prod_Prod recommenders
    """
    def __init__(self, neo4j_client, es_client):
        self.recommenders = {}
        self.neo4j_client = neo4j_client
        self.es_client = es_client
        for channel_id in get_all_channels(self.neo4j_client):
            self.update_pprec(channel_id, start_pprec_from_db(channel_id, self.neo4j_client))

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

    def is_valid_channel(self, channel_id):
        return channel_id in self.recommenders.keys()

    def fallback_to_popular(self, response, source):
        """fallback_to_popular
        if response contains no products, instead use popular products
        This still requires data to be in neo4j
        """
        if len(response['products']) > 0:
            response.update(source=source)
            return response
        else:
            return self.fallback_to_all(
                get_popular_products(self.neo4j_client),
                source='anthill-popular')

    def fallback_to_popular_full(self, response, source, from_param, size_param):
        """fallback_to_popular_full
        full es response version of fallback_to_popular
        """
        es_resp = self.es_client.get_products_list(
            products_list_from_response(response),
            from_param,
            size_param)
        if len(es_resp['result']) > 0:
            response = zip_responses(response, es_resp)
            response.update(source=source)
            return response
        else:
            popular_response = get_popular_products(self.neo4j_client)
            es_resp = self.es_client.get_products_list(
                products_list_from_response(popular_response),
                from_param,
                size_param)
            return self.fallback_to_all(
                zip_responses(popular_response, es_resp),
                source='anthill-popular',
                only_ids=False)

    def fallback_to_all(self, response, source, only_ids=False):
        """fallback_to_all
        matches all products in elasticsearch
        """
        if len(response['products']) > 0:
            response.update(source=source)
            return response
        else:
            response = format_es_response(
                self.es_client.get_products_list([], 0, 10),
                only_ids=only_ids)
            response.update(source='es-match-all')
            return response

    def recommend(self, prod_id, channel_id):
        """recommend
        take a product id
        get list of product ids from the recommender
        """
        if not self.is_valid_channel(channel_id):
            return self.fallback_to_popular(EMPTY, source='')
        rec = self.get_recommender(channel_id)
        if prod_id in rec.product_ids():
            resp = rec.recommend([prod_id])
        else:
            resp = EMPTY
        return self.fallback_to_popular(response=resp, source='anthill-similar')

    def recommend_full(self, prod_id, channel_id, from_param, size_param):
        """recommend_full
        take a product id
        get a list of full products from elasticsearch based on
        product ids from the recommender
        """
        recommender_output = self.recommend(prod_id, channel_id)
        return self.fallback_to_popular_full(
            response=recommender_output,
            source=recommender_output['source'],
            from_param=from_param,
            size_param=size_param)

    def cust_recommend(self, cust_id, channel_id):
        """cust_recommend
        take a customer id
        get list of product ids from the recommender
        """
        if not self.is_valid_channel(channel_id):
            return self.fallback_to_popular(EMPTY, source='')
        prod_ids = get_purchased_products(cust_id, channel_id, self.neo4j_client)
        excludes = get_declined_products(cust_id, self.neo4j_client)
        resp = self.recommenders[channel_id].recommend(prod_ids, excludes)
        return self.fallback_to_popular(response=resp, source='anthill-similar')

    def cust_recommend_full(self, cust_id, channel_id, from_param, size_param):
        """cust_recommend_full
        get a list of full products from elasticsearch based on
        product ids from the recommender
        """
        recommender_output = self.cust_recommend(cust_id, channel_id)
        return self.fallback_to_popular_full(
            response=recommender_output,
            source=recommender_output['source'],
            from_param=from_param,
            size_param=size_param)

    def add_point(self, cust_id, prod_id, channel_id):
        """add_point
        add a purchase event to the recommender and to neo4j
        """
        pprec = self.get_recommender(channel_id)
        pprec.add_point(cust_id, prod_id)
        self.update_pprec(channel_id, pprec)

    def train(self, payload):
        """train
        train a recommender with a set of purchase events
        """
        add_purchase_event(payload, self.neo4j_client)
        cust_id = payload.get('cust_id')
        channel_id = payload.get('channel_id')
        for prod_id in payload.get('prod_ids'):
            self.add_point(cust_id, prod_id, channel_id)
