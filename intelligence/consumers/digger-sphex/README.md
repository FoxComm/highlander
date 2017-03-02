#Digger Sphex

The digger sphex is a type of wasp otherwise known as the gold digger.

This project parses nginx logs which are stored in kafka and counts requests in
henhouse. It parses tracking requests specially by aggregating counts.

The format of the tracking keys for example is.

    track.<scope>.<channel>.<cluster>.<context>.<object>.<object id>.<verb>.<subject>
