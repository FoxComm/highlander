def products_list_from_response(resp):
    """products_list_from_response
    takes a recommender response and returns a
    python list of integer product ids
    """
    return [prod['id'] for prod in resp['products']]

def zip_responses(recommender_resp, es_response):
    """zip_responses
    combines a list of full products from elasticsearch
    and a response from a recommender into an ordered response
    of full products with recommender scores
    """
    products = [
        {'score': rec_prod['score'], 'product': product}
        for rec_prod in recommender_resp['products']
        for product in es_response['result']
        if rec_prod['id'] == product['productId']
    ]
    return {'products': sorted(products, key=lambda pr: -pr['score'])}

def format_es_response(es_response, only_ids=False):
    """format_es_response
    format a plain es response
    """
    def single_product(product):
        if only_ids:
            return product['productId']
        else:
            return product

    products = [
        {'score': 1, 'product': single_product(product)}
        for product in es_response['result']
    ]
    return {'products': products}
