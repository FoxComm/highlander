from models.Nodes import (Customer, Product)

def add_purchase_event(cust_id, prod_id, channel):
    cust = Customer.get_or_create({"phoenix_id": cust_id})[0]
    prod = Product.get_or_create({"phoenix_id": prod_id})[0]
    cust.purchased.connect(prod, {"channel": channel}).save()
