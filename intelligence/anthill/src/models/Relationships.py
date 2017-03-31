from neomodel import (StructuredRel, IntegerProperty)

class Purchased(StructuredRel):
    """Purchased
    neo4j relationship representation of a purchased event
    """
    channel = IntegerProperty()
