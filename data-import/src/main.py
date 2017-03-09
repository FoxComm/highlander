import json
import sys
import os.path
import urllib.request
from urllib.error import HTTPError
from copy import deepcopy
from collections import defaultdict

class Phoenix:
    def __init__(self, host='appliance-10-240-0-14.foxcommerce.com', user='admin@admin.com', password='password',
                 org='tenant'):
        self.host = host
        self.user = user
        self.password = password
        self.org = org
        self.prefix = "https://" + host + "/api/v1"
        self.jwt = None
        self.taxonomies = {}

    def ensure_logged_in(self):
        if self.jwt is None:
            return self.do_login()
        else:
            return True

    def do_query(self, endpoint_suffix, data, method="GET"):
        self.ensure_logged_in()
        endpoint = self.prefix + endpoint_suffix
        payload = None if (data is None or method == "GET") else json.dumps(data).encode()
        req = urllib.request.Request(endpoint, payload, headers={"Content-Type": "application/json", "JWT": self.jwt},
                                     method=method)

        try:
            response = urllib.request.urlopen(req)
        except HTTPError as err:
            print(repr(err))
            raise
        return response.getcode(), json.loads(response.read().decode('utf-8'))

    def do_login(self):
        payload = json.dumps({'email': self.user, 'password': self.password, 'org': self.org}).encode()
        req = urllib.request.Request(self.login_endpoint(), payload)
        req.add_header('Content-Type', 'application/json')

        response = urllib.request.urlopen(req)

        content = json.loads(response.read().decode('utf-8'))
        self.jwt = dict(response.info())['Jwt']
        print("logged in: " + self.prefix + " name: " + content['name'] + " scope: " + content['scope'])
        return True

    def create_taxonomy(self, name, hierarchical):
        self.ensure_logged_in()
        data = {'attributes': {'name': {'t': 'string', 'v': name}}, 'hierarchical': hierarchical}
        code, response = self.do_query("/taxonomies/default", data, method="POST")

        print("taxonomy created: id:%d, attributes: %r" % (response['id'], response['attributes']))
        return response

    def create_taxon(self, name, taxonomy_id):
        self.ensure_logged_in()
        data = {'name': name}
        code, response = self.do_query("/taxonomies/default/" + str(taxonomy_id), data, method="POST")
        print("taxon created: id = {}, name = {}, taxonomy_id = {}".format(response["taxon"]["id"],
                                                                           response["taxon"]["name"],
                                                                           response["taxonomyId"]))
        return response

    def login_endpoint(self):
        return self.prefix + "/public/login"

    def create_product(self, product_group):
        first_images = []
        skus = {} 
        color_variants = defaultdict(list)
        color_images = {}
        size_variants = defaultdict(list)
        first_sku = None
        product_images = [] 

        for s in product_group:
            images = []
            for im in s['details']['images']:
                if len(im['title']) > 0:
                    images.append({
                        'title': im['title'],
                        'src': im['full'],
                        'alt': im['alt'],
                        'thumb': im['thumbnail']
                        })
            
            #each sku will have an album but the product album
            #contains first images from each sku
            if len(images) > 0:
                product_images.append(images[0])

            sku_name = s['sku']
            title = s['title']
            description = s['details']['description']
            description_list = s['details'].get('description_list', '')
            price = int(float(s['price']['price']) * 100)
            taxonomies = s['taxonomies']
            color = taxonomies['color']

            tags = list(taxonomies.values())
                
            base_sku = {
                    'attributes' : {
                        'title' : {
                            't': 'string',
                            'v': title
                            },
                        'description' : {
                            't': 'richText',
                            'v': description
                            },
                        'description_list' : {
                            't': 'richText',
                            'v': description_list
                            },
                        'retailPrice' : {
                            't': 'price',
                            'v': {
                                'currency': 'USD',
                                'value': price
                                }
                            },
                        'salePrice' : {
                            't': 'price',
                            'v': {
                                'currency': 'USD',
                                'value': price
                                }
                            },
                        'taxonomies': {
                            't': 'object',
                            'v': taxonomies
                            },
                        'tags': {
                            't': 'tags',
                            'v': tags
                            }
                        },
                    'albums' : [{ 'name': 'default', 'images' : images }]
                    }
            options = s['details']['options']
            if options != None:
                for o in options:
                    sku_code = o['sku']
                    size = o['size']
                    osku = deepcopy(base_sku)
                    osku['attributes']['size'] = {
                            't': 'string',
                            'v': size
                            }
                    osku['attributes']['code'] = {
                            't': 'string',
                            'v': sku_code
                            }
                    color_variants[color].append(sku_code)
                    if len(images) > 0:
                        color_images[color] = images[0]['thumb']
                    size_variants[size].append(sku_code)

                    skus[sku_code] = osku

        variants = [ 
                {
                    'attributes': {
                        'name': {
                            't': 'string',
                            'v': 'color'
                            },
                        'type' : {
                            't': 'string',
                            'v': 'color'
                            }
                        },
                    'values': []
                    }, 
                {
                    'attributes': {
                        'name': {
                            't': 'string',
                            'v': 'size'
                            },
                        'type' : {
                            't': 'string',
                            'v': 'size'
                            }
                        },
                    'values': []
                    }]

        color_values = variants[0]['values']
        for k, v in color_variants.items():
            v = {
                'name' : k,
                'skuCodes': v,
                'swatch': ''
                }
            if k in color_images:
                img = color_images[k]
                v['image'] = img

            color_values.append(v)

        size_values = variants[1]['values']
        for k, v in size_variants.items():
            size_values.append({
                'name' : k,
                'skuCodes': v,
                'swatch': ''
                })

        if len(skus) > 0:
            sorted_skus = list(sorted(skus.values(), key=lambda s: s['attributes']['code']['v']))
            first_sku = sorted_skus[0]

            product = {
                    'attributes' : {
                        'title' : first_sku['attributes']['title'],
                        'description' : first_sku['attributes']['description'],
                        'description_list' : first_sku['attributes']['description_list'],
                        'tags' : first_sku['attributes']['tags'],
                        },
                    'skus' : sorted_skus,
                    'albums' : [{ 'name': 'default', 'images' : product_images }],
                    'variants': variants
                    }
            return product
        return None

    def upload_product(self, code, product):
        print("uploading: " + code)
        self.ensure_logged_in()
        try:
            code, response = self.do_query("/products/default", product, method="POST")
            if code != 200:
                print("error uploading: " + response)
                return False
        except HTTPError as err:
            print("error uploading: " + repr(err))
            return False
        return True

def load_taxonomies(file_name):
    json_data = json.load(open(file_name, 'r'))
    json_taxonomies = [entry['taxonomies'] for entry in json_data]
    taxonomy_names = set([taxonomy_name for pair in json_taxonomies for taxonomy_name in pair])
    return dict(
        [(key, set([pairs[key] for pairs in json_taxonomies if pairs[key] is not None])) for key in taxonomy_names])

def load_products(file_name):
    json_data = json.load(open(file_name, 'r'))

    products = defaultdict(list)
    for p in json_data:
        colors = p['colors']
        products[colors].append(p)

    return products


def main():

    host = sys.argv[1]
    command = sys.argv[2]
    print("HOST: " + host)
    print("CMD: " + command)

    p = Phoenix(host=host)

    if command == 'taxonomies':
        listing_taxonomies = load_taxonomies("./addidas/listings.json")
        taxonomies = load_taxonomies("./addidas/products.json")

        # merge taxonomies from listing.json and products.json
        for k in listing_taxonomies:
            if k in taxonomies:
                taxonomies[k].union(listing_taxonomies[k])
            else:
                taxonomies[k] = listing_taxonomies[k]

        print("about to add {} taxonomies with overall {} taxons".format(len(taxonomies),
                                                                         sum([len(taxonomies[k]) for k in taxonomies])))

        for taxonomy in taxonomies:
            taxons = taxonomies[taxonomy]
            created_taxonomy = p.create_taxonomy(taxonomy, False)
            for taxon in taxons:
                p.create_taxon(taxon, created_taxonomy["id"])

    elif command == 'products':
        products = load_products("./addidas/products.json")
        cache_dir = "cache"

        for v in products.values():
            product = p.create_product(v)
            if product != None:
                code = product['skus'][0]['attributes']['code']['v']

                cache_file = cache_dir + "/" + code + ".json"
                skip = os.path.exists(cache_file)

                if not skip:
                    uploaded = p.upload_product(code, product)
                    if uploaded:
                        json.dump(product, open(cache_file, 'w'))



if __name__ == "__main__":
    main()
