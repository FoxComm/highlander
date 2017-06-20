#!/usr/bin/python3
#
import argparse
import itertools
import json
import os.path
import logging
import urllib.request
import ssl
from collections import defaultdict
from urllib.error import HTTPError

from adidas_convert import convert_taxonomies, convert_products


class Taxon:
    def __init__(self, taxon_id: int, parent_id: int, name: str, taxonomy_id: int):
        self.taxonomyId = taxonomy_id
        self.parentId = parent_id
        self.name = name
        self.taxon_id = taxon_id
        self.path = [name] if parent_id is None else None


class Taxonomy:
    def __init__(self, taxonomy_id, name, taxons):
        self.taxons = [] if taxons is None else taxons
        self.name = name
        self.taxonomy_id = taxonomy_id
        self.build_paths()

    def get_taxon_by_name(self, taxon_name, parent_id=None):
        def matches(taxon: Taxon):
            result = taxon.name == taxon_name
            if parent_id is not None:
                return result and parent_id == taxon.parentId
            else:
                return result

        return next(iter([taxon for taxon in self.taxons if matches(taxon)]), None)

    def get_taxon_by_path(self, path: list):
        parent_id = None
        taxon = None
        for name in path:
            taxon = self.get_taxon_by_name(name, parent_id)
            parent_id = taxon.parentId

        return taxon

    def get_taxon_by_id(self, taxon_id):
        for taxon in self.taxons:
            if taxon.taxon_id == taxon_id:
                return taxon

        return None

    def get_path(self, taxon: Taxon):
        if taxon.path is None:
            parent = self.get_taxon_by_id(taxon.parentId)
            taxon.path = parent.path + [taxon.name]

        return taxon.path

    def build_paths(self):
        for taxon in self.taxons:
            self.get_path(taxon)

    def merge(self, taxonomy, taxon: Taxon):
        if taxonomy is not None:
            assert type(taxonomy) == Taxonomy
            self.taxons = list(set(self.taxons + taxonomy.taxons))

        if taxon is not None:
            self.taxons = list(set(self.taxons.append(taxon)))

        self.build_paths()
        return self


class Elasticsearch:
    def __init__(self, jwt, host):
        self.host = host
        self.jwt = jwt
        self.taxonomies = {}

    def do_query(self, view_name: str):
        endpoint = 'https://' + self.host + '/api/search/admin/' + view_name + '/_search/?size=10000&pretty=0'
        req = urllib.request.Request(endpoint, headers={"Content-Type": "application/json", "JWT": self.jwt})

        try:
            context = ssl.create_default_context()
            context.check_hostname = False
            response = urllib.request.urlopen(req, context=context)
        except HTTPError as err:
            logging.error(repr(err))
            raise
        return json.loads(response.read().decode('utf-8'))

    def get_taxonomies(self):
        response = self.do_query('taxonomies_search_view')
        return [(item['name'], item['taxonomyId']) for item in response["result"] if
                item['context'] == 'default' and item['archivedAt'] is None]

    def get_taxons(self):
        def read_item(item):
            return Taxon(item['taxonId'], item['parentId'], item['name'], item['taxonomyId'])

        response = self.do_query('taxons_search_view')
        taxonomies = [read_item(item) for item in response["result"] if
                      ('context' in item and item['context'] == 'default' and item['archivedAt'] is None)]
        return taxonomies

    def get_products(self):
        response = self.do_query('products_search_view')
        return response['result']

    def get_inventory(self):
        response = self.do_query('inventory_search_view')
        return response['result']


class Phoenix:
    def __init__(self, host, user, password, org):
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
            context = ssl.create_default_context()
            context.check_hostname = False
            response = urllib.request.urlopen(req, context=context)
        except HTTPError as err:
            logging.error("HTTP error. code: {}. message: {}".format(err.code, err.read()))
            raise

        code = response.getcode()

        if code == 204:
            return code, None
        return code, json.loads(response.read().decode('utf-8'))

    def do_login(self):
        logging.info("logging in: host:{}, user:{}, organization: {}".format(self.login_endpoint(), self.user, self.org))
        payload = json.dumps({'email': self.user, 'password': self.password, 'org': self.org}).encode()
        context = ssl.create_default_context()
        context.check_hostname = False
        req = urllib.request.Request(self.login_endpoint(), payload, method='POST')
        req.add_header('Content-Type', 'application/json')

        try:
            response = urllib.request.urlopen(req, context=context)
        except urllib.error.URLError as err:
            logging.error("Cannot connect to %s %s", self.login_endpoint(), err)
            raise

        content = json.loads(response.read().decode('utf-8'))
        self.jwt = dict(response.info())['Jwt']
        logging.info("logged in: " + self.prefix + " name: " + content['name'] + " scope: " + content['scope'])
        return True

    def create_taxonomy(self, taxonomy_json):
        self.ensure_logged_in()
        data = {k: v for k, v in taxonomy_json.items() if k != "taxons"}
        code, response = self.do_query("/taxonomies/default", data, method="POST")

        logging.info("taxonomy created: id:%d, attributes: %r" % (response['id'], response['attributes']))
        return Taxonomy(response['id'], response["attributes"]["name"]["v"], [])

    def create_taxon(self, taxon_json, taxonomy_id, parent_id):
        self.ensure_logged_in()
        if parent_id is not None:
            taxon_json = taxon_json.copy()
            taxon_json['location'] = {'parent': parent_id}
        code, response = self.do_query("/taxonomies/default/" + str(taxonomy_id) + "/taxons", taxon_json, method="POST")
        logging.info("taxon created: id:%d, attributes: %r" % (response['id'], response['attributes']))
        return Taxon(response['id'], parent_id, taxon_json["attributes"]["name"]["v"], taxonomy_id)

    def login_endpoint(self):
        return self.prefix + "/public/login"

    def upload_product(self, code, product):
        logging.info("uploading: " + code)
        self.ensure_logged_in()
        try:
            code, response = self.do_query("/products/default", product, method="POST")
            if code != 200:
                logging.error("error uploading: " + response)
                return False, None
        except HTTPError as err:
            logging.error("error uploading: " + repr(err))
            return False, None
        return True, response

    def assign_taxon(self, product_id: int, taxon_id: int):
        self.ensure_logged_in()
        try:
            self.do_query("/taxons/default/{}/product/{}".format(taxon_id, product_id), data=None, method="PATCH")
        except HTTPError:
            logging.error("cannot assign taxon {} to product {}".format(taxon_id, product_id))


def load_taxonomies(file_name):
    return json.load(open(file_name, 'r'))


def load_products(file_name):
    return json.load(open(file_name, 'r'))


def query_es_taxonomies(jwt: str, host: str):
    es = Elasticsearch(jwt, host=host)
    taxons = es.get_taxons()
    taxonomies = es.get_taxonomies()

    result = defaultdict(lambda: None)
    for (name, taxonomy_id) in taxonomies:
        taxonomy_taxons = [taxon for taxon in taxons if taxon.taxonomyId == taxonomy_id]
        result[name] = Taxonomy(taxonomy_id, name, taxonomy_taxons)
    return result


def assign_taxonomies(p: Phoenix, settings, taxonomies, data_product, product_id):
    def get_sku_taxonomies(sku_record):
        if 'taxonomies' in sku_record:
            return sku_record['taxonomies']
        elif 'taxonomies' in sku_record['attributes']:
            return sku_record["attributes"]["taxonomies"]["v"]
        else:
            return []

    data_taxonomies = defaultdict(set)
    for sku in data_product["skus"]:
        product_taxonomies = get_sku_taxonomies(sku)
        for (taxonomy, taxon) in product_taxonomies.items():
            if type(taxon) is list:
                data_taxonomies[taxonomy] = data_taxonomies[taxonomy].union(taxon)
            else:
                data_taxonomies[taxonomy].add(taxon)

    for (taxonomy, taxons) in data_taxonomies.items():
        for taxon in taxons:
            es_taxonomy, es_taxon = map_to_es_taxon(p, settings, data_product, taxon, taxonomies, taxonomy)

            if es_taxonomy is None or es_taxon is None:
                logging.info("Skipping taxon '{}' (Taxonomy: '{}')".format(taxon, taxonomy))
            else:
                p.assign_taxon(product_id, es_taxon.taxon_id)
                logging.info("taxon {} is assigned to product {}".format(es_taxon.taxon_id, product_id))


def map_to_es_taxon(p: Phoenix, settings, data_product, taxon, taxonomies, taxonomy):
    es_taxonomy = taxonomies[taxonomy]
    if es_taxonomy is None:
        if settings.unknown_taxonomies[0] == 'fail':
            raise ValueError(
                "product '{}' references to unknown taxonomy '{}'".format(product_code(data_product), taxonomy))
        elif settings.unknown_taxonomies[0] == 'ignore':
            return None, None
        else:
            assert settings.unknown_taxonomies[0] == 'create'
            r = create_taxon_from_name(p, taxonomy, taxon)
            es_taxonomy = r[0].merge(None, r[1])
            taxonomies[taxonomy] = es_taxonomy

    es_taxon = next(iter([t for t in es_taxonomy.taxons if t.name == taxon]), None)
    if es_taxon is None:
        if settings.unknown_taxonomies[0] == 'fail':
            raise ValueError(
                "product '{}' references to unknown taxon '{}' in taxonomy '{}'".format(product_code(data_product), taxon,
                                                                                  taxonomy))
        elif settings.unknown_taxonomies[0] == 'ignore':
            return es_taxonomy, None
        else:
            assert settings.unknown_taxonomies[0] == 'create'
            r = create_taxon_from_name(p, es_taxonomy, taxon)
            taxonomies[taxonomy] = r[0].merge(es_taxonomy, r[1])
            es_taxon = r[1]

    return es_taxonomy, es_taxon


def create_taxon_from_name(p: Phoenix, taxonomy, taxon):
    if type(taxonomy) == Taxonomy:
        es_taxonomy = taxonomy
    else:
        assert type(taxonomy) == str
        es_taxonomy = p.create_taxonomy({"attributes": {"name": {"t": "string", "v": taxonomy}}, "hierarchical": False})

    assert type(taxon) == str
    es_taxon = p.create_taxon({"attributes": {"name": {"t": "string", "v": taxon}}}, es_taxonomy.taxonomy_id, None)
    return es_taxonomy, es_taxon


def import_taxons(p: Phoenix, taxons, existing_taxonomy, parent_id=None):
    for taxon in taxons:
        name = taxon["attributes"]["name"]["v"]
        existing_taxon = existing_taxonomy.get_taxon_by_name(name)
        if existing_taxon is None:
            t = p.create_taxon(taxon, existing_taxonomy.taxonomy_id, parent_id)
            taxon_id = t.taxon_id
        else:
            logging.info("skipping taxon '{}' as soon as it already exists. id: {}".format(taxon,
                                                                                    existing_taxon.taxon_id))
            taxon_id = existing_taxon.taxon_id

        if 'children' in taxon and taxon['children'] is not None:
            import_taxons(p, taxon['children'], existing_taxonomy, taxon_id)


def import_taxonomies(p: Phoenix, input_dir, import_from_adidas):
    print("Importing taxonomies\n")

    if import_from_adidas:
        taxonomies = convert_taxonomies(input_dir)
    else:
        taxonomies_json = load_taxonomies(input_dir + "/taxonomies.json")
        taxonomies = taxonomies_json["taxonomies"]

    if p.ensure_logged_in():
        imported = query_es_taxonomies(p.jwt, p.host)

        print("about to add {} taxonomies with overall {} taxons".format(len(taxonomies),
                                                                         sum([len(k["taxons"]) for k in
                                                                              taxonomies])))
        for taxonomy in taxonomies:
            name = taxonomy["attributes"]["name"]["v"]
            taxons = taxonomy["taxons"]

            existing_taxonomy = imported[name]
            if existing_taxonomy is None:
                existing_taxonomy = p.create_taxonomy(taxonomy)
            else:
                msg = "skipping taxonomy '{}' as soon as it already exists. id: {}"
                logging.info(msg.format(taxonomy, existing_taxonomy.taxonomy_id))

            import_taxons(p, taxons, existing_taxonomy, )


def import_products(p: Phoenix, settings, max_products, input_dir, import_from_adidas):
    print("Importing products\n")

    if import_from_adidas:
        products = convert_products(input_dir)
    else:
        products_json = load_products(input_dir + "/products.json")
        products = products_json["products"]

    cache_dir = "cache"
    if not os.path.exists(cache_dir):
        os.makedirs(cache_dir)

    p.ensure_logged_in()
    taxonomies = query_es_taxonomies(p.jwt, p.host)

    products = products if max_products is None else itertools.islice(products, int(max_products))

    for product in products:
        code = product_code(product)

        cache_file = cache_dir + "/" + code + ".json"
        skip = os.path.exists(cache_file)

        if not skip:
            uploaded, result = p.upload_product(code, product)
            if uploaded:
                json.dump(product, open(cache_file, 'w'))
                assign_taxonomies(p, settings, taxonomies, product, result['id'])


def product_code(product):
    return product['skus'][0]['attributes']['code']['v']


def get_inventory(phoenix):
    es = Elasticsearch(phoenix.jwt, phoenix.host)
    return es.get_inventory()


def add_inventory_to_stock_item(phoenix, stock_item, amount):
    typ = stock_item['type']
    if typ != 'Sellable':
        return

    itm = stock_item['stockItem']
    item_id = str(itm['id'])
    sku = itm['sku']
    old_amount = str(stock_item['onHand'])

    logging.info(sku + ' (' + item_id + ') ' + old_amount + ' => ' + str(amount))
    increment = {"qty": amount, "type": "Sellable", "status": "onHand"}
    try:
        code, response = phoenix.do_query("/inventory/stock-items/" + item_id + "/increment", increment, method="PATCH")
        if code != 204:
            logging.error("error adding inventory: " + response)
    except HTTPError as err:
        logging.error("error adding inventory: " + repr(err))


def add_inventory(phoenix, amount):
    phoenix.ensure_logged_in()
    inventory = get_inventory(phoenix)
    for itm in inventory:
        add_inventory_to_stock_item(phoenix, itm, amount)

def config_logging():
    logging.basicConfig(format='%(asctime)s:%(levelname)s:%(message)s', level=logging.DEBUG)

def main():
    config_logging()
    options = read_cmd_line()

    print("HOST: ", options.host)
    print("CMD: ", options.command[0])
    if options.max_products is not None:
        print("MAX: ", options.max_products[0])

    max_products = None if options.max_products is None else options.max_products[0]

    p = Phoenix(host=options.host, user='admin@admin.com', password='password', org='tenant')

    if options.command[0] == 'taxonomies':
        import_taxonomies(p, options.input[0], options.adidas)
    elif options.command[0] == 'products':
        import_products(p, options, max_products, options.input[0], options.adidas)
    elif options.command[0] == 'both':
        import_taxonomies(p, options.input[0], options.adidas)
        import_products(p, options, max_products, options.input[0], options.adidas)
    elif options.command[0] == 'inventory':
        add_inventory(p, options.inventory_amount[0])
    else:
        print("Valid commands are, 'taxonomies', 'products', 'both', or 'inventory'")


def read_cmd_line():
    pp = argparse.ArgumentParser(
        description='Data import')
    pp.add_argument("--host", type=str, required=True, help="host of the API to import into")
    pp.add_argument("--max-products", "-m", nargs=1, type=int,
                    help="Max products. Provides a way to restrict how many products are imported")
    pp.add_argument("--input", "-i", nargs=1, type=str, default=['data'], help="input directory")
    pp.add_argument("--inventory_amount", nargs=1, type=int, default=[100], help="inventory amount")
    pp.add_argument("--adidas", action='store_true', default=False,
                    help="treat input directory as container of listing.json and products.json with adidas data")
    pp.add_argument("command", nargs=1, choices=['taxonomies', 'products', 'both', 'inventory'],
                    type=str, help="Command")
    pp.add_argument("--unknown-taxonomies", nargs=1, choices=['ignore', 'create', 'fail'], type=str,
                    help="defines behavior in case if product references on taxonomy/taxon which wasn't created before."
                         "  <ignore> - ignore the taxon and continue import."
                         "  <fail> - stop import. Prints error message."
                         "  <create> - creates the absent taxonomy and taxon. The created taxonomy is flat.",
                    default='fail')

    return pp.parse_args()


if __name__ == "__main__":
    main()
