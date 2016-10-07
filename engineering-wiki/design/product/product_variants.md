# Product Variants
### Jeff Mataya

## Purpose

One of the most important parts of the product model is understanding how
individual items, know as SKUs, relate to each other. This is in many ways the
fundamental merchandising question: what SKUs should be displayed together?

This document will attempt to describe the problems that merchandisers face
when linking SKUs and hopefully a data model that will solve those problems.

## Relevant Documents

- [Product Design Document](https://github.com/FoxComm/phoenix-scala/blob/master/docs/design/product/product_model.pdf)
- [Product Variants Mockups](https://www.dropbox.com/s/mvout93fx0nzjh3/Product_Variants.pdf?dl=0)

## Requirements

### I. Allow an admin to add SKUs to a product

In the most basic operation, a user will use variants as a way to relate
multiple SKUs to each other. That workflow may look a bit like this in the
example of a t-shirt that comes in three colors.

1. User opens the new product form;
2. User sets basic attributes of a t-shirt (such as name, description, etc.);
3. User create a variant called _color_;
4. User sets the possible values of _color_ as _green_, _black_, and _grey_;
5. User create (or associates) a SKU with the _green_, _black_, and _grey_
   values.

One this product is published to the storefront, a user will be able to see a
single page that displays each of the t-shirts. A customer will be able to
select which t-shirt to add to cart by toggling the _color_ that they desire.

### II. Reuse variants from existing products

While the workflow above works well enough to create an individual PDP, it gets
cumbersome to repeat. Therefore, we want to provide two shortcuts (both of which
are illustrated in the Product Variants Mockups).

1. Instead of creating a new variant, a user can select from existing variants;
2. If an existing variant is selected, the values that have been used previously
   (_green_, _black_, _grey_ in the above example) can be optionally reused on
  the new product.

## The Models

### Variant

The definition of a attribute that should connect multiple SKUs. Common examples
in the fashion and apparel world are _size_ and _color_. In reality, any
attribute could be a variant.

**Example: Variant JSON**
```JSON
{
  "id": 123,
  "name": "color"
}
```

### Variant Value

The instantiation of a `Variant` is the `VariantValue`. Consider the above
example where we have the `Variant` _color_. Examples of the `VariantValue`
might be _red_, _green_, or _blue_.

**Example: VariantValue JSON responses for Variant color**
```JSON
[
  {
    "id": 456,
    "variantId": 123,
    "value": "red"
  }, {
    "id": 457,
    "variantId": 123,
    "value": "blue"
  }, {
    "id": 458,
    "variantId": 123,
    "value": "green"
  }
]
```

### Variant Attributes

In addition to defining how SKUs relate to each other, the `Variant` is also
responsible for defining how the differences between SKUs are merchandised. It
does this by specifying a series of attributes that the `VariantValue` may (or
in some cases, must) set.

The attribute is defined in a simple object that defines that attribute's type
and whether it's required.

**Example: Variant JSON with defined attributes**
```JSON
{
  "id": 123,
  "name": "color",
  "attributes": {
    "swatch": {
      "type": "color",
      "required": true
    },
    "thumbnail": {
      "type": "image",
      "required": false
    }
  }
}
```

### Variant Value Attribute

Similar to the ways that attributes are defined in `ProductForm` and
`ProductShadow`, the value on `VariantValue` is a simple value.

**Example: VariantValue JSON fro the above Variant**
```JSON
{
  "id": 456,
  "variantId": 123,
  "value": "red",
  "attributes": {
    "swatch": "#ff0000",
    "thumbnail": "http://some.cdn/product-image.png"
  }
}
```

## Variants in a Product

At this point, if you aren't familiar with the Product and SKU models as
described in
[product_model.pdf](https://github.com/FoxComm/phoenix-scala/blob/master/docs/design/product/product_model.pdf),
it's worth stopping to read.

In that document, the following example exists (I've updated it slightly to
match the current state of the API).

**Example: Product With Variants**
```JavaScript
{
  id: 1,
  ...
  variants: {
    Germany: {
      color: {
        red: "SKU-RED1",
        green: "SKU-GREEN2",
      },
    },
    USA: {
      color: {
        purple: "SKU-BLUE3",
        orange: "SKU-ORGAN3",
      },
    },
  },
}
```

### Accounting for Context

In the example above, _Germany_ and _USA_ are contexts, _color_ is a `Variant`,
and _red_, _green_, _purple_, and _orange_ are `VariantValue`s.

In the update that's proposed in this document, the Variants in the example
above would be updated to include the attributes described above. Here's what we
might see:

**Example: Product With Variants and Attributes**
```JavaScript
{
  id: 1,
  ...
  variants: {
    Germany: {
      color: {
        red: {
          attributes: {
            swatch: "#ff0000",
          },
          sku: "SKU-RED1",
        },
        green: {
          attributes: {
            swatch: "##00ff00",
          },
          sku: "SKU-GREEN2",
        },
      },
    },
    USA: {
      color: {
        purple: {
          attributes: {
            swatch: "#551a8b",
          },
          sku: "SKU-BLUE3",
        },
        orange: {
          attributes: {
            swatch: "##ffa500",
          },
          sku: "SKU-ORGAN3",
        },
      },
    },
  },
}
```

What we can see above is that we rely on Context in the `Product`, rather than
attaching the Context to the `Variant`.

### Nested Variants

It's possible for a single product to have multiple variants. In that case, the
variants end up nested, creating a tree with variants as the leaf node.

A common use case is size and color. So, let's use that as the example.

**Example: Product with Nested Attributes**
```JavaScript
{
  id: 1,
  ...
  variants: {
    default: {
      color: {
        red: {
          attibutes: {
            swatch: "#ff0000"
          },
          variants: {
            size: {
              small: {
                sku: "RED-SMALL",
              },
              medium: {
                sku: "RED-MEDIUM",
              },
              large: {
                sku: "RED-LARGE",
              },
            },
          },
        },
        green: {
          attibutes: {
            swatch: "#00ff00"
          },
          variants: {
            size: {
              small: {
                sku: "GREEN-SMALL",
              },
              medium: {
                sku: "GREEN-MEDIUM",
              },
              large: {
                sku: "GREEN-LARGE",
              },
            },
         },
        },
      },
    },
  },
}
```

## Open Questions

1. What do we want the final output in a product to look like?
2. Can we get away with not making the `Variant` context-dependent? Would like
   to access context through the product or SKU, if possible.
3. What about versioning?
