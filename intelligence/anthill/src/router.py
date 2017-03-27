import os

from flask import Flask, request, jsonify
from recommenders.Prod_Prod import Prod_Prod
from controllers.PurchaseController import add_purchase_event, get_all_by_channel
from managers.Prod_Prod_Manager import Prod_Prod_Manager
from util.ES_Client import ES_Client
from util.InvalidUsage import InvalidUsage
from util.neo4j_utils import connect_to_neo4j

connect_to_neo4j()
APP = Flask(__name__)
ES_CLIENT = ES_Client('ic-cross.foxcommerce.com')
PP_MANAGER = Prod_Prod_Manager()

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
    channel_id = int(request.args.get('channel', -1))
    resp = PP_MANAGER.recommend(prod_id, channel_id)
    return jsonify(resp)

@APP.route('/prod-prod/full/<int:prod_id>', methods=['GET'])
def rec_prod_prod_full(prod_id):
    """rec_prod_prod_full
    returns a list of full products from elasticsearch
    """
    channel_id = int(request.args.get('channel', -1))
    size_param = int(request.args.get('size', 5))
    from_param = int(request.args.get('from', 0))
    full_resp = PP_MANAGER.recommend_full(prod_id, channel_id, ES_CLIENT, from_param, size_param)
    return jsonify(full_resp)

@APP.route('/cust-prod/<int:cust_id>', methods=['GET'])
def rec_cust_prod(cust_id):
    """rec_cust_prod
    """
    connect_to_neo4j()
    channel_id = int(request.args.get('channel', -1))
    resp = PP_MANAGER.cust_recommend(cust_id, channel_id)
    return jsonify(resp)

@APP.route('/cust-prod/full/<int:cust_id>', methods=['GET'])
def rec_cust_prod_full(cust_id):
    """rec_cust_prod_full
    returns a list of full products from elasticsearch
    """
    connect_to_neo4j()
    channel_id = int(request.args.get('channel', -1))
    size_param = int(request.args.get('size', 5))
    from_param = int(request.args.get('from', 0))
    full_resp = PP_MANAGER.cust_recommend_full(cust_id, channel_id, ES_CLIENT, from_param, size_param)
    return jsonify(full_resp)

@APP.route('/prod-prod/train', methods=['POST'])
def train_prod_prod():
    """train
    """
    connect_to_neo4j()
    json_dict = request.get_json()
    PP_MANAGER.train(json_dict['points'])
    return 'success'

port = os.getenv('PORT', 5000)

if __name__ == "__main__":
    APP.run(host='0.0.0.0', port=port)
