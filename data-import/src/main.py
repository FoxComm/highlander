#!/usr/bin/python3
#

import json
import os.path
import sys
import urllib.request
from collections import defaultdict
from copy import deepcopy
from urllib.error import HTTPError


class Taxon:
    def __init__(self, taxon_id: int, parent_id: int, name: str, taxonomy_id: int):
        self.taxonomyId = taxonomy_id
        self.parentId = parent_id
        self.name = name
        self.taxon_id = taxon_id


class Taxonomy:
    def __init__(self, taxonomy_id, name, taxons):
        self.taxons = [] if taxons is None else taxons
        self.name = name
        self.taxonomy_id = taxonomy_id

    def get_taxon_by_name(self, taxon_name):
        return next(iter([taxon for taxon in self.taxons if taxon.name == taxon_name]), None)


class Elasticsearch:
    def __init__(self, jwt, host='appliance-10-240-0-14.foxcommerce.com'):
        self.host = host
        self.jwt = jwt
        self.taxonomies = {}

    def do_query(self, view_name: str):
        endpoint = 'https://' + self.host + '/api/search/admin/' + view_name + '/_search/?size=10000&pretty=0'
        req = urllib.request.Request(endpoint, headers={"Content-Type": "application/json", "JWT": self.jwt})

        try:
            response = urllib.request.urlopen(req)
        except HTTPError as err:
            print(repr(err))
            raise
        return json.loads(response.read().decode('utf-8'))

    def get_taxonomies(self):
        response = self.do_query('taxonomies_search_view')
        return [(item['name'], item['taxonomyId']) for item in response["result"] if item['context'] == 'default']

    def get_taxons(self):
        def read_item(item):
            return Taxon(item['taxonId'], item['parentId'], item['name'], item['taxonomyId'])

        response = self.do_query('taxons_search_view')
        return [read_item(item) for item in response["result"] if item['context'] == 'default']

    def get_products(self):
        response = self.do_query('products_search_view')
        return response['result']


class Phoenix:
    def __init__(self, host='appliance-10-240-0-14.foxcommerce.com', user='admin@admin.com', password='password',
                 org='tenant'):
        self.host = host
        self.user = user
        self.password = password
        self.org = org
        self.prefix = "https://" + host + "/api/v1"
        self.jwt = None

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
        return Taxonomy(response['id'], name, [])

    def create_taxon(self, name, taxonomy_id):
        self.ensure_logged_in()
        data = {'name': name}
        code, response = self.do_query("/taxonomies/default/" + str(taxonomy_id), data, method="POST")
        print("taxon created: id = {}, name = {}".format(response["taxon"]["id"],
                                                         response["taxon"]["name"]))
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

            # each sku will have an album but the product album
            # contains first images from each sku
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
                            },
                        'activeTo': {
                            't': 'datetime',
                            'v': None
                            },
                        'activeFrom': {
                            't': 'datetime',
                            'v': '2017-03-09T19:59:21.609Z'
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
                    'type': {
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
                    'type': {
                        't': 'string',
                        'v': 'size'
                    }
                },
                'values': []
            }]

        color_values = variants[0]['values']
        for k, v in color_variants.items():
            v = {
                'name': k,
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
                'name': k,
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
                        'activeTo' : first_sku['attributes']['activeTo'],
                        'activeFrom' : first_sku['attributes']['activeFrom'],
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
                return False, None
        except HTTPError as err:
            print("error uploading: " + repr(err))
            return False, None
        return True, response

    def assign_taxon(self, product_id: int, taxon_id: int):
        self.ensure_logged_in()
        try:
            self.do_query("/taxons/default/{}/product/{}".format(taxon_id, product_id), data=None, method="PATCH")
        except HTTPError:
            print("cannot assign taxon {} to product {}".format(taxon_id, product_id))


def load_taxonomies(file_name):
    json_data = json.load(open(file_name, 'r'))
    json_taxonomies = [read_taxonomies_section(e) for e in json_data]
    taxonomy_names = set([taxonomy_name for pair in json_taxonomies for taxonomy_name in pair])
    return dict(
        [(key, set().union(*[pairs[key] for pairs in json_taxonomies if pairs[key] is not None])) for key in
         taxonomy_names])


def load_products(file_name):
    json_data = json.load(open(file_name, 'r'))

    products = defaultdict(list)
    for p in json_data:
        colors = p['colors']
        products[colors].append(p)

    return products


def query_es_taxonomies(jwt: str):
    es = Elasticsearch(jwt)
    taxons = es.get_taxons()
    taxonomies = es.get_taxonomies()

    result = defaultdict(lambda: None)
    for (name, taxonomy_id) in taxonomies:
        taxonomy_taxons = [taxon for taxon in taxons if taxon.taxonomyId == taxonomy_id]
        result[name] = Taxonomy(taxonomy_id, name, taxonomy_taxons)
    return result


def load_file_taxonomies():
    listing_taxonomies = load_taxonomies("./addidas/listings.json")
    taxonomies = load_taxonomies("./addidas/products.json")

    # merge taxonomies from listing.json and products.json
    for k in listing_taxonomies:
        if k in taxonomies:
            taxonomies[k].union(listing_taxonomies[k])
        else:
            taxonomies[k] = listing_taxonomies[k]
    return taxonomies


def read_taxonomies_section(data_product):
    data_taxonomies = data_product['taxonomies']

    # filter taxonomies and split taxons like 'a/b/c' to multiple taxons [a,b,c]
    result = {}
    for taxonomy in data_taxonomies:
        if taxonomy not in ['model_id', 'video']:
            delimiter = '|' if taxonomy == 'sport' else '/'
            result[taxonomy] = set([t.strip() for t in data_taxonomies[taxonomy].split(delimiter)])

    return result


def assign_taxonomies(p: Phoenix, taxonomies, data_product, product_id):
    data_taxonomies = defaultdict(set)
    if type(data_product) is list:
        for product in data_product:
            for (taxonomy, taxons) in read_taxonomies_section(product).items():
                data_taxonomies[taxonomy] = data_taxonomies[taxonomy].union(taxons)
    else:
        data_taxonomies = read_taxonomies_section(data_product)

    for (taxonomy, taxons) in data_taxonomies.items():
        for taxon in taxons:
            es_taxonomy = taxonomies[taxonomy]
            es_taxon = next(iter([t for t in es_taxonomy.taxons if t.name == taxon]), None)
            p.assign_taxon(product_id, es_taxon.taxon_id)
            print("taxon {} is assigned to product {}".format(es_taxon.taxon_id, product_id))


def import_taxonomies(p: Phoenix):
    if p.ensure_logged_in():
        imported = query_es_taxonomies(p.jwt)
        taxonomies = load_file_taxonomies()

        print("about to add {} taxonomies with overall {} taxons".format(len(taxonomies),
                                                                         sum([len(taxonomies[k]) for k in
                                                                              taxonomies])))
        for taxonomy in taxonomies:
            taxons = taxonomies[taxonomy]

            existing_taxonomy = imported[taxonomy]
            if existing_taxonomy is None:
                existing_taxonomy = p.create_taxonomy(taxonomy, False)
            else:
                print("skipping taxonomy '{}' as soon as it already exists. id: {}".format(taxonomy,
                                                                                           existing_taxonomy.taxonomy_id))

            for taxon in taxons:
                existing_taxon = existing_taxonomy.get_taxon_by_name(taxon)
                if existing_taxon is None:
                    p.create_taxon(taxon, existing_taxonomy.taxonomy_id)
                else:
                    print("skipping taxon '{}' as soon as it already exists. id: {}".format(taxon,
                                                                                            existing_taxon.taxon_id))


def import_products(p:Phoenix):
    products = load_products("./addidas/products.json")
    cache_dir = "cache"
    p.ensure_logged_in()
    taxonomies = query_es_taxonomies(p.jwt)

    for v in products.values():
        product = p.create_product(v)
        if product is not None:
            code = product['skus'][0]['attributes']['code']['v']

            cache_file = cache_dir + "/" + code + ".json"
            skip = os.path.exists(cache_file)

            if not skip:
                uploaded, result = p.upload_product(code, product)
                if uploaded:
                    json.dump(product, open(cache_file, 'w'))
                    assign_taxonomies(p, taxonomies, v, result['id'])


def main():
    host = sys.argv[1]
    command = None if len(sys.argv) < 3 else sys.argv[2]
    print("HOST: ", host)
    print("CMD: ", command)

    p = Phoenix(host=host)

    if command == 'taxonomies':
        import_taxonomies(p)
    elif command == 'products':
        import_products(p)
    elif command is None:
        print("Importing taxonomies\n")
        import_taxonomies(p)
        print("Importing products\n")
        import_products(p)

if __name__ == "__main__":
    main()
