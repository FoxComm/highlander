from models.Nodes import (Customer, Product)

def add_purchase_event(cust_id, prod_id, channel_id):
    """add_purchase_event
        cust_id :: int
        prod_id :: int
        channel_id :: string

    stores a purchase event in neo4j
    """
    cust = Customer.get_or_create({"phoenix_id": cust_id})[0]
    prod = Product.get_or_create({"phoenix_id": prod_id})[0]
    cust.purchased.connect(prod, {"channel": channel_id}).save()

def get_all_by_channel(channel_id):
    """get_all_by_channel
        channel_id :: string

    returns a list of pairs [cust_id, prod_id] for which Customer
    cust_id has purchased Product prod_id over channel channel_id
    """
    return [[cust.phoenix_id, prod.phoenix_id]
            for cust in Customer.nodes
            for prod in cust.purchased.match(channel=channel_id)
           ]
