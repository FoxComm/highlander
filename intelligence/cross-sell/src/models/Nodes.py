from neomodel import (StructuredNode, IntegerProperty, RelationshipTo, RelationshipFrom)
from models.Relationships import Purchased

class Customer(StructuredNode):
    """Customer
    neo4j node representation of a customer
    """
    phoenix_id = IntegerProperty(unique_index=True, required=True)
    purchased = RelationshipTo('Product', 'PURCHASED', model=Purchased)

class Product(StructuredNode):
    """Product
    neo4j node representation of a product
    """
    phoenix_id = IntegerProperty(unique_index=True, required=True)
    purchased = RelationshipFrom('Customer', 'PURCHASED', model=Purchased)
