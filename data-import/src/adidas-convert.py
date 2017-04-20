#!/usr/bin/python3
#

import itertools
import json
import math
import os.path
from collections import defaultdict
from copy import deepcopy

from elastic import Elasticsearch
from model import Taxonomy
from phoenix import Phoenix


def create_product(product_group):
    skus = {}
    color_variants = defaultdict(list)
    color_images = {}
    size_variants = defaultdict(list)
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

        title = s['title']
        description = s['details']['description']
        description_list = s['details'].get('description_list', '')
        short_description = s['details'].get('short_description', '')
        price = int(float(s['price']['price']) * 100)
        taxonomies = s['taxonomies']
        taxonomies_processed = read_taxonomies_section_as_list(s)
        color = taxonomies['color']

        tags = list(taxonomies.values())
        tags.sort()

        base_sku = {
            'attributes': {
                'title': {
                    't': 'string',
                    'v': title
                },
                'description': {
                    't': 'richText',
                    'v': description
                },
                'shortDescription': {
                    't': 'string',
                    'v': short_description
                },
                'description_list': {
                    't': 'richText',
                    'v': description_list
                },
                'retailPrice': {
                    't': 'price',
                    'v': {
                        'currency': 'USD',
                        'value': price
                    }
                },
                'salePrice': {
                    't': 'price',
                    'v': {
                        'currency': 'USD',
                        'value': price
                    }
                },
                'taxonomies': {
                    't': 'object',
                    'v': taxonomies_processed
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
            'albums': [{'name': 'default', 'images': images}]
        }
        options = s['details']['options']
        if options is not None:
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
    color_values.sort(key=lambda val:val["name"])

    size_values = variants[1]['values']
    for k, v in size_variants.items():
        size_values.append({
            'name': k,
            'skuCodes': v,
            'swatch': ''
        })
    size_values.sort(key=lambda val:val["name"])

    if len(skus) > 0:
        sorted_skus = list(sorted(skus.values(), key=lambda sku: sku['attributes']['code']['v']))
        first_sku = sorted_skus[0]

        product = {
            'attributes': {
                'title': first_sku['attributes']['title'],
                'description': first_sku['attributes']['description'],
                'description_list': first_sku['attributes']['description_list'],
                'shortDescription': first_sku['attributes']['shortDescription'],
                'tags': first_sku['attributes']['tags'],
                'activeTo': first_sku['attributes']['activeTo'],
                'activeFrom': first_sku['attributes']['activeFrom'],
            },
            'skus': sorted_skus,
            'albums': [{'name': 'default', 'images': product_images}],
            'variants': variants
        }
        return product
    return None


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


def query_es_taxonomies(jwt: str, host: str):
    es = Elasticsearch(jwt, host=host)
    taxons = es.get_taxons()
    taxonomies = es.get_taxonomies()

    result = defaultdict(lambda: None)
    for (name, taxonomy_id) in taxonomies:
        taxonomy_taxons = [taxon for taxon in taxons if taxon.taxonomyId == taxonomy_id]
        result[name] = Taxonomy(taxonomy_id, name, taxonomy_taxons)
    return result


def load_file_taxonomies():
    listing_taxonomies = load_taxonomies("./adidas/listings.json")
    taxonomies = load_taxonomies("./adidas/products.json")

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


def read_taxonomies_section_as_list(product):
    def toListOrValue(v):
        result = list(v)
        return result[0] if len(result) == 1 else sorted(result)

    data_taxonomies = read_taxonomies_section(product)
    return {k: toListOrValue(v) for (k, v) in data_taxonomies.items()}


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
    print("Importing taxonomies\n")
    if p.ensure_logged_in():
        imported = query_es_taxonomies(p.jwt, p.host)
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
                print("skipping taxonomy '{}' as soon as it already exists. id: {}"
                      .format(taxonomy, existing_taxonomy.taxonomy_id))

            for taxon in taxons:
                existing_taxon = existing_taxonomy.get_taxon_by_name(taxon)
                if existing_taxon is None:
                    p.create_taxon(taxon, existing_taxonomy.taxonomy_id)
                else:
                    print("skipping taxon '{}' as soon as it already exists. id: {}"
                          .format(taxon, existing_taxon.taxon_id))


def import_products(p: Phoenix, max_products):
    print("Importing products\n")
    products = load_products("./adidas/products.json")
    cache_dir = "cache"
    p.ensure_logged_in()
    taxonomies = query_es_taxonomies(p.jwt, p.host)

    product_groups = products.values() if max_products is None else itertools.islice(products.values(),
                                                                                     int(max_products))

    for g in product_groups:
        product = create_product(g)
        if product is not None:
            code = product['skus'][0]['attributes']['code']['v']

            cache_file = cache_dir + "/" + code + ".json"
            skip = os.path.exists(cache_file)

            if not skip:
                uploaded, result = p.upload_product(code, product)
                if uploaded:
                    json.dump(product, open(cache_file, 'w'))
                    assign_taxonomies(p, taxonomies, g, result['id'])


def create_taxonomy(name, hierarchical="false", taxons=None):
    return {'attributes': {'name': {'t': 'string', 'v': name}}, 'hierarchical': hierarchical, 'taxons': taxons}


def create_taxon(name):
    return {'attributes': {'name': {'t': 'string', 'v': name}}}


def main():
    outDir = "data_r2"
    if not os.path.isdir(outDir):
        os.makedirs(outDir)

    max_products_per_file = 1

    converted_taxonomies = convert_taxonomies()
    result = {'taxonomies': converted_taxonomies}
    file = open(outDir + "/taxonomies.json", "w")
    try:
        json.dump(result, fp=file, indent=4, sort_keys=True)
    finally:
        file.close()

    converted_products = convert_products("./adidas/products.json")
    converted_products.sort(key = lambda product: product['skus'][0]['attributes']['code']['v'])

    if max_products_per_file is not None and len(converted_products) > max_products_per_file:
        for index in range(0, int(math.ceil(len(converted_products) / max_products_per_file))):
            result = {'products': list(
                itertools.islice(converted_products, index * max_products_per_file, (index + 1) * max_products_per_file))}
            dump_products(result, outDir + "/products" + str(index) + ".json")
    else:
        dump_products(converted_products, outDir+"/products.json")


def dump_products(products, file_name):
    file = open(file_name, "w")
    try:
        json.dump(products, fp=file, indent=4, sort_keys=True)
    finally:
        file.close()


def convert_taxonomies():
    taxonomies = load_file_taxonomies()
    return [create_taxonomy(name, taxons=[create_taxon(taxon_name) for taxon_name in values]) for (name, values) in
            taxonomies.items()]


def convert_products(file_location):
    products = load_products(file_location)
    converted = [create_product(g) for g in products.values()]
    return [v for v in converted if v is not None]


if __name__ == "__main__":
    main()
