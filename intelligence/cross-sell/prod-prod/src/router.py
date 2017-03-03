from flask import Flask, request, jsonify
from PPRecommend import PPRecommend
from InvalidUsage import InvalidUsage

app = Flask(__name__)

# TODO Create some kind of PPRecommendManager class.
pprecs = {}
def get_pprec(pprecs, channel_id):
    """get_pprec
    Return PPRecommend object at channel_id if found else create new PPRecommend
    """
    if channel_id in pprecs:
        return pprecs[channel_id]
    else:
        return PPRecommend()

def update_pprec(pprecs, channel_id, pprec):
    """update_pprec
    Update the PPRecommend dictionary for the given channel_id
    """
    pprecs[channel_id] = pprec
    return pprecs

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

@app.route('/recommend/prod-prod/<int:prod_id>', methods=['GET'])
def rec_prod_prod(prod_id):
    """rec_prod_prod
    """
    # Handle Invalid Channel
    channel_id = int(request.args.get('channel', -1))
    if channel_id <= -1:
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

@app.route('/recommend/prod-prod/train', methods=['POST'])
def train():
    """train
    """
    json_dict = request.get_json()

    for point in json_dict['points']:
        channel_id = point['chanID']
        pprec = get_pprec(pprecs, channel_id)

        pprec.add_point(point['custID'], point['prodID'], channel_id)
        update_pprec(pprecs, channel_id, pprec)

    return ""
