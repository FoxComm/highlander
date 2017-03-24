import os
from neomodel import db

def connect_to_neo4j():
    NEO4J_USER = os.getenv('NEO4J_USER') # neo4j
    NEO4J_PASS = os.getenv('NEO4J_PASS') # password
    NEO4J_HOST = os.getenv('NEO4J_HOST') # localhost
    NEO4J_PORT = os.getenv('NEO4J_PORT') # 7687
    db.set_connection('bolt://%s:%s@%s:%s' % (NEO4J_USER, NEO4J_PASS, NEO4J_HOST, NEO4J_PORT))

def get_all_channels():
    query = "MATCH ()-[r]-() RETURN DISTINCT r.channel"
    channels, _ = db.cypher_query(query)
    return channels
