from neomodel import (StructuredNode, IntegerProperty, RelationshipTo, RelationshipFrom)
from models.Relationships import Purchased

class Customer(StructuredNode):
    phoenix_id = IntegerProperty(unique_index=True, required=True)
    purchased = RelationshipTo('Product', 'PURCHASED', model=Purchased)

class Product(StructuredNode):
    phoenix_id = IntegerProperty(unique_index=True, required=True)
    purchased = RelationshipFrom('Customer', 'PURCHASED', model=Purchased)
