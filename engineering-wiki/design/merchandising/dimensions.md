# Merchandising: Dimensions and Facets
### Design Spec

## Overview

This document describes Dimensions and Facets, the mechanisms that we use to
flexibly categorize merchandising entities, such as Products and SKUs. This
document won't describe actually creating categories in any technical sense, but
will touch on out Dimensions and Facets are used to create a category structure.

#### Goals of Categorization

As we consider this design, we should keep in mind the overall goals of how
categories work in FoxCommerce:

1. Categories are decoupled from site navigation
2. The Products or SKUs returned from a category in site navigation should be
   the result of a saved search
3. The attributes upon which a saved search is built should include a flexible
   taxonomy structure that will enable both a tree-based menu system, such as
   [Sephora](http://www.sephora.com), or a search-based interface, such as
   [Adidas](http://www.adidas.com).

#### Definitions

* **Merchandising Entity:** Any object in Phoenix that is powered by `ObjectForm`,
  `ObjectShadow`, etc. This specifically includes `Product`, `Sku`, `Album`,
  `Variant`, `Promotion`, and `Coupon` at the time of writing.
* **Dimension:** A Dimension a specific taxonomy that defines an attribute on a
  merchandising entity.
* **Facet:** A Facet is a value that's associated with Dimension. For example,
  if we have a Dimension "Gender", then it might have Facets: "Men" and "Women".

## Requirements

1. Dimension and Facet should be context-aware and versioned
2. Dimensions should allow either their Facets to be either single-selected,
   meaning that only one of their Facets may exist on the merchandising entity,
   or multi-selected.
3. The Facets of a Dimension may be either flat or hierarchical. For example:

    ```
    // Flat
    Gender
    ------
    - Men
    - Women
    - Children

    // Hierarchical
    Apparel
    -------
    - Shirts
      - Short Sleeve
      - Long Sleeve
    - Pants
      - Shorts
      - Pants
    - Shoes
      - Dress
      - Casual
    ```

4. It should be possible to search for a merchandising entity based on its Facets.
5. Dimensions and Facets should be able to accept a flexible set of metadata for
   information that may be used when displaying it on storefront.

## Potential Design

#### Dimension

In order to achieve requirements (1) and (5), Dimension (and Facet) are built on
top of the `ObjectForm` and `ObjectShadow` infrastructure. Instead of going too
deeply into how that works, here's what the head object looks like:

```Scala
sealed trait SelectionType
case object Single extends SelectionType
case object Multi extends SelectionType

sealed trait FacetType
case object Flat extends FacetType
case object Hierarchical extends FacetType

case class Dimension(id: Int,
                     contextId: Int,
                     commitId: Int,
                     formId: Int,
                     shadowId: Int,
                     selectionType: SelectionType,
                     facetType: FacetType)
```

This signature is mostly the same as what you'd see for a `Product` or `Sku`
inside of Phoenix right now, except for the addition of `selectionType` and
`facetType`. These define the selection behavior and facet structure of the
Dimension.

Also, note that none of the metadata about the Dimension, such as name, exists
on the head. That will be kept inside the form/shadow attributes so that it can
be versioned correctly.

#### Facet

Like Dimension, Facet is build on top of `ObjectForm` and `ObjectShadow`.

```Scala
case class Facet(id: Int,
                 contextId: Int,
                 commitId: Int,
                 formId: Int,
                 shadowId: Int)
```

Facet ends up being a very simple implementation of an `ObjectHead`, with all
interesting metadata stored in the form and shadow.

#### Associating Dimensions and Facets

For now, we're going to punt on the implementation of hierarchical Dimensions,
so this section will only talk about flat Dimensions. Dimensions and Facets will
be associated through an `ObjectLink`, or more accurately, what's being proposed
to replace the `ObjectLink`.

<TODO: Update and insert the new ObjectLink documentation>

#### Associating Facets and Merchandising Objects

Finally, the interesting thing!
