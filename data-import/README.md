# Scripts to import test taxonomies / products into the system.

    ```
    python3 src/main.py --host HOST {taxonomies,products,both,inventory}
    ```
Run `python3 src/main.py -h` for additional options.

The `data` directory must have following input files:

| file name      |content|
|:---------      |:------|
|taxonomies.json |taxonomies which should be imported by `taxonomies` or `both` commands|
|products.json   |products which should be imported by `products` or `both` commands|

These files are not stored here.
You can generate these files from adidas sample data.
You need AWS S3 access to these files

[listings.json](https://s3-us-west-2.amazonaws.com/fc-dev-assets/sample_data/listings.json)
[products.json](https://s3-us-west-2.amazonaws.com/fc-dev-assets/sample_data/products.json)

Put the files into `adidas` folder and run `adidas-convert.py`:
    ```
    python3 src/adidas_convert.py
    ```
As result `taxonomies.json` and `products.json` will be written to `data` directory.
Run `python3 src/adidas_convert.py -h` for additional options.

You can omit running `adidas_convert.py` by specifying `--adidas` to `main.py`.
In this case `main.py` will read `listings.json` and `products.json` from `data` directory
and convert them on the fly without writing to file system.
