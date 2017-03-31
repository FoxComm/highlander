import os
from neomodel import db

def connect_to_neo4j():
    """connect_to_neo4j
    """
    neo4j_user = os.getenv('NEO4J_USER')
    neo4j_pass = os.getenv('NEO4J_PASS')
    neo4j_host = os.getenv('NEO4J_HOST')
    neo4j_port = os.getenv('NEO4J_PORT')
    db.set_connection('bolt://%s:%s@%s:%s' % (neo4j_user, neo4j_pass, neo4j_host, neo4j_port))

def get_all_channels():
    """get_all_channels
    """
    query = "MATCH ()-[r]-() RETURN DISTINCT r.channel"
    channels, _ = db.cypher_query(query)
    return channels

def get_purchased_products(customer_id, channel_id):
    """get_purchased_products
    return a list of products which have been purchased
    by the customer over a specific channel
    """
    query = """
        MATCH (:Customer {phoenix_id: {phoenix_id}})-
        [:PURCHASED {channel: {channel}}]->(p:Product)
        return p.phoenix_id
        """
    params = {"phoenix_id": customer_id, "channel": channel_id}
    res, _meta = db.cypher_query(query, params)
    return [x for [x] in res]

def get_all_by_channel(channel_id):
    """get_all_by_channel
    return a list of [customer_id, product_id] purchase events
    for a given channel
    """
    query = """
        MATCH (c:Customer)-[:PURCHASED {channel: {channel_id}}]->(p:Product)
        return c.phoenix_id, p.phoenix_id
        """
    params = {"channel_id": channel_id}
    res, _meta = db.cypher_query(query, params)
    return res
