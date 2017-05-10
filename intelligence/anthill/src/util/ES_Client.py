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

def match_all_query():
    """match_all_query
    used as a last resort when no recommendations can be found
    """
    return json.dumps({
        'query': {
            'match_all': {}
        }
    })

def cleanup_search_result(json):
    """cleanup_search_results
    es response cleaning that usually happens in nginx
    """
    output = {
        "result": [hit['_source'] for hit in json['hits']['hits']],
        "pagination": {
            "total": json['hits']['total']
        }
    }
    return output

class ES_Client(object):
    """ES_Client
    provides an interface to query elasticsearch
    """
    def __init__(self, host, port):
        self.host = host
        self.port = port
        self.header = {'Content-Type': 'application/json'}
        self.url = '/public/products_catalog_view/_search'

    def get_full_url(self, from_param, size_param):
        """get_full_url
        append query params
        """
        return self.url + '?from=%d&size=%d' % (from_param, size_param)

    def get_products_list(self, prod_ids, from_param, size_param):
        """get_products_list
        query elasticsearch to get a list of full products
        """
        if len(prod_ids) > 0:
            body = products_list_query(prod_ids)
        else:
            body = match_all_query()
        conn = http.client.HTTPConnection(self.host, self.port)
        conn.request(
            method='POST',
            url=self.get_full_url(from_param, size_param),
            body=body,
            headers=self.header)
        resp = conn.getresponse().read().decode('utf-8')
        conn.close()
        output = cleanup_search_result(json.loads(resp))
        return output
