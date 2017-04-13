import json
import http.client

def products_list_query(prod_ids):
    """products_list_query
    elasticsearch query string to get full products from
    a list of product ids
    """
    return json.dumps({
        'query': {
            'bool': {
                'filter':[
                    {'term': {'context': 'default'}},
                    {'terms':{'productId': prod_ids}}
                ]
            }
        }
    })

class ES_Client(object):
    """ES_Client
    provides an interface to query elasticsearch
    """
    def __init__(self, host):
        self.host = host
        self.header = {'Content-Type': 'application/json'}

    def get_products_list(self, prod_ids, from_param, size_param):
        """get_products_list
        query elasticsearch to get a list of full products
        """
        conn = http.client.HTTPSConnection(self.host)
        conn.request(
            method='POST',
            url='/api/search/public/products_catalog_view/_search?from=%d&size=%d' % (from_param, size_param),
            body=products_list_query(prod_ids),
            headers=self.header
        )
        resp = conn.getresponse().read().decode('utf-8')
        conn.close()
        return json.loads(resp)
