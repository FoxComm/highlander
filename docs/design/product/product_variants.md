# Product Variants
### Jeff Mataya

## Purpose

One of the most important parts of the product model is understanding how
individual items, know as SKUs, relate to each other. This is in many ways the
fundamental merchandising question: what SKUs should be displayed together?

This document will attempt to describe the problems that merchandisers face
when linking SKUs and hopefully a data model that will solve those problems.

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
