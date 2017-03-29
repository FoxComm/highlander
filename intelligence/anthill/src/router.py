from flask import Flask, request, jsonify
from prod_prod.PPRecommend import PPRecommend
from InvalidUsage import InvalidUsage
from controllers.PurchaseController import add_purchase_event, get_all_by_channel
import os

from neomodel import db
NEO4J_USER = os.getenv('NEO4J_USER') # neo4j
NEO4J_PASS = os.getenv('NEO4J_PASS') # password
NEO4J_HOST = os.getenv('NEO4J_HOST') # localhost
NEO4J_PORT = os.getenv('NEO4J_PORT') # 7687
db.set_connection('bolt://%s:%s@%s:%s' % (NEO4J_USER, NEO4J_PASS, NEO4J_HOST, NEO4J_PORT))

app = Flask(__name__)

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
@app.errorhandler(InvalidUsage)
def handle_invalid_usage(error):
    """handle_invalid_usage
    Returns JSON error message
    """
    response = jsonify(error.to_dict())
    response.status_code = error.status_code
    return response

# API Endpoints
@app.route('/ping')
def ping():
    """ping
    Returns pong
    """
    return 'pong'

@app.route('/prod-prod/<int:prod_id>', methods=['GET'])
def rec_prod_prod(prod_id):
    """rec_prod_prod
    """
    # Handle Invalid Channel
    channel_id = int(request.args.get('channel', ""))
    if channel_id == "":
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

@app.route('/prod-prod/train', methods=['POST'])
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
    app.run(host='0.0.0.0', port=port)
