from flask import Flask, request, jsonify
from prod_prod.PPRecommend import PPRecommend
import os

app = Flask(__name__)

pprec = PPRecommend()

@app.route('/ping')
def ping():
    return 'pong'

@app.route('/recommend/prod-prod/<int:prod_id>', methods=['GET'])
def rec_prod_prod(prod_id):
    return jsonify(pprec.recommend(prod_id))

@app.route('/recommend/prod-prod/train', methods=['POST'])
def train():
    json_dict = request.get_json()
    for point in json_dict['points']:
        pprec.add_point(point['custID'], point['prodID'], point['chanID'])
    return ""

port = os.getenv('PORT', 5000)

if __name__ == "__main__":
    app.run(host='0.0.0.0', port=port)

