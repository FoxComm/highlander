import http.client
import json
from datetime import datetime

def login_body():
    org = 'tenant'
    email = 'admin@admin.com'
    password = 'password'
    return json.dumps({
        'org': org,
        'email': email,
        'password': password
    })

def products_list_query(prod_ids):
    return json.dumps({
        'query': {
            'ids': {
                'values': prod_ids
            }
        }
    })

class ES_Client(object):
    def __init__(self, host):
        self.header = {'Content-Type': 'application/json'}
        self.conn = http.client.HTTPSConnection(host)
        self.conn.request(
            method='POST',
            url='/api/v1/public/login',
            body=login_body(),
            headers=self.header
        )
        resp = self.conn.getresponse()
        self.header.update({'jwt': resp.getheader('jwt')})
        resp.read() # necessary to reuse the connection

    def get_products_list(self, prod_ids):
        self.conn.request(
            method='POST',
            url='/api/search/admin/customers_search_view/_search?size=500',
            body=products_list_query(prod_ids),
            headers=self.header
        )
        resp = self.conn.getresponse()
        return json.loads(resp.read())
