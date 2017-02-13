from flask import Flask, request, jsonify
from Recommend import Recommend

app = Flask(__name__)

fake_resp = {'products': [
    {'id': 1, 'score': 0.75},
    {'id': 2, 'score': 0.65},
    {'id': 3, 'score': 0.55}
]}

knn_rec = Recommend()

@app.route('/ping')
def hello_world():
    return 'pong'

@app.route('/recommend/prod-prod/<int:prod_id>', methods=['GET'])
def rec_prod_prod(prod_id):
    return jsonify(knn_rec.recommend(prod_id))

@app.route('/recommend/train', methods=['POST'])
def train():
    json_dict = request.get_json()
    for point in json_dict['points']:
        knn_rec.addPoint(point['custID'], point['prodID'])
    return ""
