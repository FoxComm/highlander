import os

from flask import Flask, request, jsonify
from prod_prod.PPRecommend import PPRecommend
from controllers.PurchaseController import add_purchase_event, get_all_by_channel
from util.ES_Client import ES_Client
from util.InvalidUsage import InvalidUsage
from util.response_utils import products_list_from_response, zip_responses

from neomodel import db
NEO4J_USER = os.getenv('NEO4J_USER') # neo4j
NEO4J_PASS = os.getenv('NEO4J_PASS') # password
NEO4J_HOST = os.getenv('NEO4J_HOST') # localhost
NEO4J_PORT = os.getenv('NEO4J_PORT') # 7687
db.set_connection('bolt://%s:%s@%s:%s' % (NEO4J_USER, NEO4J_PASS, NEO4J_HOST, NEO4J_PORT))

APP = Flask(__name__)
ES_CLIENT = ES_Client('ic-cross.foxcommerce.com')

# TODO Create some kind of PPRecommendManager class.
pprecs = {}
def get_pprec(pprecs, channel_id):
    """get_pprec
    Return PPRecommend object at channel_id if found else create new PPRecommend
    """
    if channel_id in pprecs.keys():
        return pprecs[channel_id]
    else:
        return PPRecommend()

def update_pprec(pprecs, channel_id, pprec):
    """update_pprec
    Update the PPRecommend dictionary for the given channel_id
    """
    pprecs[channel_id] = pprec
    return pprecs

def startup_pprecs():
    """startup_pprecs
    get data from neo4j and train pprecs for all present channels
    """
    query = "MATCH ()-[r]-() RETURN DISTINCT r.channel"
    channels, _ = db.cypher_query(query)
    for [channel_id] in channels:
        pprec = get_pprec(pprecs, channel_id)
        for [cust_id, prod_id] in get_all_by_channel(channel_id):
            pprec.add_point(cust_id, prod_id)
        update_pprec(pprecs, channel_id, pprec)

# Register Middleware
@APP.errorhandler(InvalidUsage)
def handle_invalid_usage(error):
    """handle_invalid_usage
    Returns JSON error message
    """
    response = jsonify(error.to_dict())
    response.status_code = error.status_code
    return response

# API Endpoints
@APP.route('/ping')
def ping():
    """ping
    Returns pong
    """
    return 'pong'

@APP.route('/prod-prod/<int:prod_id>', methods=['GET'])
def rec_prod_prod(prod_id):
    """rec_prod_prod
    """
    # Handle Invalid Channel
    channel_id = int(request.args.get('channel', -1))
    if channel_id < 0:
        raise InvalidUsage('Invalid Channel ID', status_code=400,
                           payload={'error_code': 100})

    pprec = get_pprec(pprecs, channel_id)
    if pprec.is_empty():
        raise InvalidUsage('Channel ID not found', status_code=400,
                           payload={'error_code': 101})

    # Handle product_id that has not been added to valid channel
    if prod_id not in pprec.product_ids():
        raise InvalidUsage('Product ID not found in channel', status_code=400,
                           payload={'error_code': 102})

    recommend_output = pprec.recommend(prod_id)

    update_pprec(pprecs, channel_id, pprec)

    return jsonify(recommend_output)

@APP.route('/prod-prod/full/<int:prod_id>', methods=['GET'])
def rec_prod_prod_full(prod_id):
    """rec_prod_prod_full
    returns a list of full products from elasticsearch
    """
    # Handle Invalid Channel
    channel_id = int(request.args.get('channel', -1))
    if channel_id < 0:
        raise InvalidUsage('Invalid Channel ID', status_code=400,
                           payload={'error_code': 100})

    pprec = get_pprec(pprecs, channel_id)
    if pprec.is_empty():
        raise InvalidUsage('Channel ID not found', status_code=400,
                           payload={'error_code': 101})

    # Handle product_id that has not been added to valid channel
    if prod_id not in pprec.product_ids():
        raise InvalidUsage('Product ID not found in channel', status_code=400,
                           payload={'error_code': 102})

    recommend_output = pprec.recommend(prod_id)
    es_resp = ES_CLIENT.get_products_list(products_list_from_response(recommend_output))
    full_resp = zip_responses(recommend_output, es_resp)

    update_pprec(pprecs, channel_id, pprec)

    return jsonify(full_resp)

@APP.route('/prod-prod/train', methods=['POST'])
def train_prod_prod():
    """train
    """
    json_dict = request.get_json()

    for point in json_dict['points']:
        channel_id = int(point['chanID'])
        pprec = get_pprec(pprecs, channel_id)

        pprec.add_point(point['custID'], point['prodID'])

        update_pprec(pprecs, channel_id, pprec)
        add_purchase_event(point['custID'], point['prodID'], channel_id)

    return ""

port = os.getenv('PORT', 5000)

if __name__ == "__main__":
    startup_pprecs()
    APP.run(host='0.0.0.0', port=port)
