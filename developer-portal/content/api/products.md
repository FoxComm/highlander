# Products

## Product

This is an object that represent a unique product within a storefront. It
differs from `Variants`, which represent the unique variations of a product. To
understand the different, consider a store that sells jackets. Each jacket style
would be a `Product`, while the size and color combinations of each style would
be individual `Variants`.

The defining features of the object is an ability to very flexibly customize the
set of properties (known as `attributes`) on the `Product` to your liking. Every
change made to the object is automatically versioned and can be reverted at any
time.

### Attributes

#### id: (required, number)

##### Description

Unique identifier for the product.

##### Example

1

#### attributes (required, hash)

##### Description

The set of properties used to describe the product. The hash is composed of keys
(representing product attribute names) and values that store the attribute's
type and value.

##### Example

    {
      "title": {
        "t": "string",
        "v": "Leather Jacket"
      },
      "description": {
        "t": "richText",
        "v": "<p>The greatest leather jacket!</p>"
      }
    }
    
#### variants (required, array[Variant])

##### Description

List of variants that make up the product.

##### Example

    [{
      "id": 1,
      "attributes": {
        "title": {
          "t": "string",
          "v": "Leather Jacket - Brown"
        },
        "code": {
          "t": "string",
          "v": "JKT-BROWN-LEATHER"
        },
        "salePrice": {
          "t": "price",
          "v": {
            "currency": "USD",
            "value": 39500
          }
        }
      }
    },
    {
      "id": 2,
      "attributes": {
        "title": {
          "t": "string",
          "v": "Leather Jacket - Black"
        },
        "code": {
          "t": "string",
          "v": "JKT-BLACK-LEATHER"
        },
        "salePrice": {
          "t": "price",
          "v": {
            "currency": "USD",
            "value": 39500
          }
        }
      }
    }]
    
#### options: (optonal, array[Option])
      
+ Attributes
    + id: (required, number) - Unique identifier for the product.
    + attributes: (required, ContentProperties) - The set of properties used to describe the product.
    + variants: (required, array[Variant]) - List of variants that make up the product.
    + options: (optional, array[Option]) - List of options that define how multiple variants relate to each other.
    + albums: (optional, array[Album]) - List of collections of images used to merchandise the product.
    + taxons: (optional, array[Taxon]) - List of taxons, with associated taxonomy information, that are used to classify the product.


