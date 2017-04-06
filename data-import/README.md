# Scripts to import test products into the system. 

    ```
    python3 src/main.py <host> <command> <max products?>
    ```

1. Host specifies the host of the API to import into.
2. Command can be "taxonomies", "products", "both", or "inventory".
3. Max products is optional and provides a way to restrict how many are imported


The data directory must also have the listings.json and products.json file.
These files are not stored here.

You need AWS S3 access to these files

[listings.json](https://s3-us-west-2.amazonaws.com/fc-dev-assets/sample_data/listings.json)
[products.json](https://s3-us-west-2.amazonaws.com/fc-dev-assets/sample_data/products.json)


