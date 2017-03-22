def products_list_from_response(resp):
    return [prod['id'] for prod in resp['products']]

def zip_responses(recommender_resp, es_response):
    products = [
        {'score': rec_prod['score'], 'product': product}
        for rec_prod in recommender_resp['products']
        for product in es_response['result']
        if rec_prod['id'] == product['id']
    ]
    return {'products': sorted(products, key=lambda pr: -pr['score'])}
