import http.client
import json

def products_list_query(prod_ids):
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
    def __init__(self, host):
        self.host = host
        self.header = {'Content-Type': 'application/json'}

    def get_products_list(self, prod_ids):
        conn = http.client.HTTPSConnection(self.host)
        conn.request(
            method='POST',
            url='/api/search/public/products_catalog_view/_search?size=500',
            body=products_list_query(prod_ids),
            headers=self.header
        )
        resp = conn.getresponse().read().decode('utf-8')
        conn.close()
        return json.loads(resp)
