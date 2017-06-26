import os
from functools import reduce
from neo4j.v1 import GraphDatabase, basic_auth

def get_all_channels(client):
    """get_all_channels
    """
    client.start()
    query = """
        MATCH ()-[r:PURCHASED]-()
        WHERE r.channel IS NOT NULL
        RETURN DISTINCT r.channel"""
    result = client.session.run(query)
    out = [record['r.channel'] for record in result]
    client.stop()
    return out

def get_popular_products(client):
    """get_popular_products
    get all products ordered by number of times purchased
    """
    client.start()
    query = """MATCH (p:Product)
        MATCH (c:Customer)-[:PURCHASED]->(p)
        RETURN p.phoenix_id, count(*)
        ORDER BY count(*) DESC"""
    result = client.session.run(query)
    out = {'products': [
        {'id': record['p.phoenix_id'],
         'score': record['count(*)']}
        for record in result
    ]}
    return out

def get_purchased_products(customer_id, channel_id, client):
    """get_purchased_products
    return a list of products which have been purchased
    by the customer over a specific channel
    """
    client.start()
    client.session.run(merge_customer_node("c", customer_id))
    query = match_xyz(
        {"model": "Customer", "props": {"phoenix_id": customer_id}},
        {"model": "PURCHASED", "props": {"channel": channel_id}},
        {"model": "Product"}
    ) + "\nRETURN z.phoenix_id"
    result = client.session.run(query)
    out = [record['z.phoenix_id'] for record in result]
    client.stop()
    return out

def get_declined_products(customer_id, client):
    """get_deleted_products
    return a list of products which have been declined
    by the customer through the suggester flow
    """
    client.start()
    query = match_xyz(
        {"model": "Customer", "props": {"phoenix_id": customer_id}},
        {"model": "DECLINED"},
        {"model": "Product"}
    ) + "\nRETURN z.phoenix_id"
    result = client.session.run(query)
    out = [record['z.phoenix_id'] for record in result]
    client.stop()
    return out

def get_all_by_channel(channel_id, client):
    """get_all_by_channel
    return a list of [customer_id, product_id] purchase events
    for a given channel
    """
    client.start()
    query = match_xyz(
        {"model": "Customer"},
        {"model": "PURCHASED", "props": {"channel": channel_id}},
        {"model": "Product"}
    ) + "\nRETURN x.phoenix_id, z.phoenix_id"
    result = client.session.run(query)
    out = [[record['x.phoenix_id'], record['z.phoenix_id']] for record in result]
    client.stop()
    return out

def comma_concat(x, y):
    return "%s, %s" % (x, y)

def match_customer_node(label, phoenix_id):
    return "MATCH (%s:Customer {phoenix_id: %d})\n" % (label, phoenix_id)

def match_product_node(label, phoenix_id):
    return "MATCH (%s:Product {phoenix_id: %d})\n" % (label, phoenix_id)

def merge_customer_node(label, phoenix_id):
    return "MERGE (%s:Customer {phoenix_id: %d})\n" % (label, phoenix_id)

def merge_product_node(label, phoenix_id):
    return "MERGE (%s:Product {phoenix_id: %d})\n" % (label, phoenix_id)

def merge_product_nodes(product_ids):
    merge_products = lambda x, y: x + merge_product_node("p%d" % y, y)
    return reduce(merge_products, product_ids, "")

def match_xyz(start_node, rel, end_node):
    """match_xyz
    matches (x:_)-[y:_]->(z:_)
    each parameter should have the structure:
    {"model": "Customer". "props": {"phoenix_id": 12}} or
    {"model": "Customer". "props": None} or
    """
    return "MATCH (x:%s %s)-[y:%s %s]->(z:%s %s)" % (
        start_node["model"], stringify_props(start_node.get("props")),
        rel["model"], stringify_props(rel.get("props")),
        end_node["model"], stringify_props(end_node.get("props")))

def stringify_props(props):
    """stringify_props
    {'some_key': 'some_value'} -> "{some_key: some_value}"
    only supports dictionaries with string or number values
    """
    if props is None:
        return ""
    pairs = ["%s: %s" % (key, props[key]) for key in props.keys()]
    return "{" + reduce(comma_concat, pairs) + "}"

def merge_purchased_rel(label, customer, product, props=None):
    if props is None:
        props_string = ""
    else:
        props_string = stringify_props(props)
    return "MERGE (%s)-[%s:PURCHASED %s]->(%s)\n" % (customer, label, props_string, product)

def merge_purchased_rels(customer, product_ids, props=None):
    merge_rels = lambda x, y: x + merge_purchased_rel("r%d" % y, customer, "p%d" % y, props)
    return reduce(merge_rels, product_ids, "")

def return_values(labels):
    return "RETURN " + reduce(comma_concat, labels)

def build_purchase_query(payload):
    cust_id = payload["cust_id"]
    prod_ids = payload["prod_ids"]
    props = {"channel": payload["channel_id"]}
    return (
        merge_customer_node("c", cust_id) +
        merge_product_nodes(prod_ids) +
        merge_purchased_rels("c", prod_ids, props))

def add_purchase_event(payload, neo4j_client):
    neo4j_client.start()
    neo4j_client.session.run(
        build_purchase_query(payload))
    neo4j_client.stop()

class Neo4j_Client(object):
    def __init__(self):
        self.driver = None
        self.session = None

    def start(self):
        """start
        open connection for a transaction
        """
        neo4j_user = os.getenv('NEO4J_USER')
        neo4j_pass = os.getenv('NEO4J_PASS')
        neo4j_host = os.getenv('NEO4J_HOST')
        neo4j_port = os.getenv('NEO4J_PORT')
        self.driver = GraphDatabase.driver(
            'bolt://%s:%s@%s:%s' % (neo4j_user, neo4j_pass, neo4j_host, neo4j_port),
            auth=basic_auth('neo4j', 'password'))
        self.session = self.driver.session()

    def stop(self):
        """close
        close connection after a transaction
        """
        self.session.close()
        self.driver.close()
