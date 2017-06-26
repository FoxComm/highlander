#!/usr/bin/python3
#
import argparse
import itertools
import json
import math
import os.path
from collections import defaultdict
from copy import deepcopy


def convert_product(product_group):
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
        taxonomies_processed = read_taxonomies_section_for_product(s)
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


def convert_taxonomies(dir):

    def set_active_from(attributes):
        attributes['activeFrom'] = {'t':'datetime','v':'2017-03-09T19:59:21.609Z'}
        return attributes

    def create_taxonomy(name, hierarchical=False, taxons=None):
        return {'attributes': set_active_from({'name': {'t': 'string', 'v': name}}), 'hierarchical': hierarchical, 'taxons': taxons}

    def create_taxon(name):
        return {'attributes': set_active_from({'name': {'t': 'string', 'v': name},})}

    taxonomies = load_file_taxonomies(dir)
    return [create_taxonomy(name, taxons=[create_taxon(taxon_name) for taxon_name in values]) for (name, values) in
            taxonomies.items()]


def load_file_taxonomies(dir):
    def load_taxonomies(file_name, save_to = defaultdict(set)):
        json_data = json.load(open(file_name, 'r'))
        json_taxonomies = [read_taxonomies_section(e) for e in json_data]

        for t in json_taxonomies:
            for (k,v) in t.items():
                save_to[k] = save_to[k].union(v)
        return save_to

    result = defaultdict(set)
    load_taxonomies(dir+"/listings.json", result)
    load_taxonomies(dir+"/products.json", result)
    return result


def read_taxonomies_section(data_product):
    def parse_taxon(taxonomy, taxon):
        delimiter = '|' if taxonomy == 'sport' else '/'
        return [t.strip() for t in taxon.split(delimiter)]

    data_taxonomies = data_product['taxonomies']
    skip_taxonomies = ['model_id', 'video']
    # filter taxonomies and split taxons like 'a/b/c' to multiple taxons [a,b,c]
    return {taxonomy: parse_taxon(taxonomy, taxon) for (taxonomy,taxon) in data_taxonomies.items() if taxonomy not in skip_taxonomies}


def read_taxonomies_section_for_product(product):
    def to_list_or_single_value(v):
        values = list(set(v))
        return v[0] if len(values) == 1 else sorted(values)

    return {k: to_list_or_single_value(v) for (k, v) in read_taxonomies_section(product).items()}


def convert_products(dir):
    def load_products(file_name):
        json_data = json.load(open(file_name, 'r'))

        products = defaultdict(list)
        for p in json_data:
            colors = p['colors']
            products[colors].append(p)

        return products

    file_location = dir+"/products.json"
    products = load_products(file_location)
    converted = [convert_product(g) for g in products.values()]
    return [v for v in converted if v is not None]


def main():
    settings = read_cmd_line()
    if not os.path.isdir(settings.output):
        os.makedirs(settings.output)

    converted_taxonomies = convert_taxonomies(settings.input)
    dump_to_file(settings.output, "taxonomies", converted_taxonomies, settings.split_taxonomies[0])

    converted_products = convert_products(settings.input)
    converted_products.sort(key = lambda product: product['skus'][0]['attributes']['code']['v'])

    dump_to_file(settings.output, "products", converted_products, settings.split_products[0])


def read_cmd_line():
    pp = argparse.ArgumentParser(
        description='Converts products.json and listings.json to taxonomies.json and products.json.')
    pp.add_argument("--input", "-i", default="adidas", nargs=1, type=str, help="input directory")
    pp.add_argument("--output", "-o", default="data", nargs=1, type=str, help="output directory")
    pp.add_argument("--split-products", nargs=1, type=int, default=[0],
                    help="""if defined splits output to a multiple files.
                    Each file contains amount of products which is less or equal 'split_products' value""")
    pp.add_argument("--split-taxonomies", nargs=1, type=int, default=[0],
                    help="""if defined splits output to a multiple files.
                    Each file has amount of taxonomies which is <= 'split_taxonomies' value""")

    return pp.parse_args()


def dump_to_file(dir, objects_type, items, max_per_file):

    def do_write(data, file_name):
        file = open(file_name, "w")
        try:
            json.dump(data, fp=file, indent=4, sort_keys=True)
        finally:
            file.close()

    assert type(items) is list
    do_split = max_per_file is not None and max_per_file>0
    if do_split and len(items) > max_per_file:
        for index in range(0, int(math.ceil(len(items) / max_per_file))):
            result = {objects_type: list(
                itertools.islice(items, index * max_per_file,
                                 (index + 1) * max_per_file))}
            do_write(result, dir + "/"+objects_type + str(index) + ".json")
    else:
        do_write({objects_type: items}, dir + "/"+objects_type+".json")


if __name__ == "__main__":
    main()
