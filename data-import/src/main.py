import json
import urllib.request
from urllib.error import HTTPError


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
        return json.loads(response.read().decode('utf-8'))

    def do_login(self):
        payload = json.dumps({'email': self.user, 'password': self.password, 'org': self.org}).encode()
        req = urllib.request.Request(self.login_endpoint(), payload)
        req.add_header('Content-Type', 'application/json')

        response = urllib.request.urlopen(req)

        content = json.loads(response.read().decode('utf-8'))
        self.jwt = dict(response.info())['Jwt']
        print("logged in: " + content['name'] + " scope: " + content['scope'])
        return True

    def create_taxonomy(self, name, hierarchical):
        self.ensure_logged_in()
        data = {'attributes': {'name': {'t': 'string', 'v': name}}, 'hierarchical': hierarchical}
        response = self.do_query("/taxonomies/default", data, method="POST")

        print("taxonomy created: id:%d, attributes: %r" % (response['id'], response['attributes']))
        return response

    def create_taxon(self, name, taxonomy_id):
        self.ensure_logged_in()
        data = {'name': name}
        response = self.do_query("/taxonomies/default/" + str(taxonomy_id), data, method="POST")
        print("taxon created: id = {}, name = {}, taxonomy_id = {}".format(response["taxon"]["id"],
                                                                           response["taxon"]["name"],
                                                                           response["taxonomyId"]))
        return response

    def login_endpoint(self):
        return self.prefix + "/public/login"


def load_taxonomies(file_name):
    json_data = json.load(open(file_name, 'r'))
    json_taxonomies = [entry['taxonomies'] for entry in json_data]
    taxonomy_names = set([taxonomy_name for pair in json_taxonomies for taxonomy_name in pair])
    return dict(
        [(key, set([pairs[key] for pairs in json_taxonomies if pairs[key] is not None])) for key in taxonomy_names])


def main():
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
    p = Phoenix()

    for taxonomy in taxonomies:
        taxons = taxonomies[taxonomy]
        created_taxonomy = p.create_taxonomy(taxonomy, False)
        for taxon in taxons:
            p.create_taxon(taxon, created_taxonomy["id"])


if __name__ == "__main__":
    main()
