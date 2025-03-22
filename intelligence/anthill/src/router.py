import os

from flask import Flask, request, jsonify, make_response
from managers.Prod_Prod_Manager import Prod_Prod_Manager
from util.ES_Client import ES_Client
from util.InvalidUsage import InvalidUsage
from util.neo4j_utils import Neo4j_Client

APP = Flask(__name__)
NEO4J_CLIENT = Neo4j_Client()
ES_CLIENT = ES_Client(
        os.getenv('ES_HOST', 'elasticsearch.service.consul'),
        os.getenv('ES_PORT', '9200'))
PP_MANAGER = Prod_Prod_Manager(NEO4J_CLIENT, ES_CLIENT)

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
@APP.route('/public/ping')
def ping():
    """ping
    Returns pong
    """
    return jsonify({'ping': 'pong'})

@APP.route('/public/prod-prod/<int:prod_id>', methods=['GET'])
def rec_prod_prod(prod_id):
    """rec_prod_prod
    """
    channel_id = int(request.args.get('channel', -1))
    resp = PP_MANAGER.recommend(prod_id, channel_id)
    return jsonify(resp)

@APP.route('/public/prod-prod/full/<int:prod_id>', methods=['GET'])
def rec_prod_prod_full(prod_id):
    """rec_prod_prod_full
    returns a list of full products from elasticsearch
    """
    channel_id = int(request.args.get('channel', -1))
    size_param = int(request.args.get('size', 10))
    from_param = int(request.args.get('from', 0))
    full_resp = PP_MANAGER.recommend_full(prod_id, channel_id, from_param, size_param)
    resp = make_response(jsonify(full_resp))
    resp.headers['Allow-Control-Allow-Origin'] = '*'
    return resp

@APP.route('/public/cust-prod/<int:cust_id>', methods=['GET'])
def rec_cust_prod(cust_id):
    """rec_cust_prod
    """
    channel_id = int(request.args.get('channel', -1))
    resp = PP_MANAGER.cust_recommend(cust_id, channel_id)
    return jsonify(resp)

@APP.route('/public/cust-prod/full/<int:cust_id>', methods=['GET'])
def rec_cust_prod_full(cust_id):
    """rec_cust_prod_full
    returns a list of full products from elasticsearch
    """
    channel_id = int(request.args.get('channel', -1))
    size_param = int(request.args.get('size', 5))
    from_param = int(request.args.get('from', 0))
    full_resp = PP_MANAGER.cust_recommend_full(cust_id, channel_id, from_param, size_param)
    return jsonify(full_resp)

@APP.route('/private/prod-prod/train', methods=['POST'])
def train_prod_prod():
    """train
    """
    PP_MANAGER.train(request.get_json())
    return 'success'

port = os.getenv('PORT', 5000)

if __name__ == "__main__":
    APP.run(host='0.0.0.0', port=port)
