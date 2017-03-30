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
