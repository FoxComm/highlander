# Object Associations Design Document

**Author:** Jeff Mataya  
**Date:** May 19, 2016

## Overview

The PIM and Merchandising systems of Phoenix are built on top of a context-aware
and versioning model called `ObjectForm` and `ObjectShadow`. This gives us a lot
of power to craft very specific views on objects such as products, SKUs, and
categories.

While this system works well for individual entities, it breaks down when we
to link entities across objects, such as when a SKU needs to be associated with
a product. This document will describe how multiple objects are associated with
each other and maintained across versions and contexts.

## Goals

At the highest level, the following problems exist in the current data model:

- Updates to linked objects are computationally expensive.
- Storage of updates are storage expensive because we manage links with shadows.
- The table that links objects is generic, thus we sacrifice some referential
  integrity - we can validate in application code, but it would be nice to
  leverage the database for this,

On the other side, because we strictly maintain versioned associations, rolling
an object graph back to a previous version is very efficient.

However, we want to optimize performance for the update scenario, as it is the
operation that will most frequently occur (by multiple orders of magnitude),
while still retaining our ability to correctly version the whole graph.

The rest of the document will dive into the problem and present the solution.

## A Review

If you haven't read it, go check out the [Product Design Doc](../product/product_model.pdf)
and [Product Versioning Design Doc](../product/product_model_with_versioning.pdf)
as a refresher about the model. One of the pieces not covered, is how objects
are associated with each other. Currently, it's through the `ObjectLink` model,
which roughly has the following signature:

```Scala
case class ObjectLink(leftId: Int, rightId: Int, linkType: String)
```

This model associates the shadows of two specific objects. For example, to make
an association between a `Product` and `Sku`, a link with `leftId` pointing to
the product's `shadowId` and `rightId` pointing to the SKU's `shadowId` would be
created. Since shadows are specific to a context and unique per version, we have
a link that's context-aware and versioned. Woo!

## The Problem

Alas, we have trouble in paradise: while this architecture optimizes how to
handle different versions and contexts excellently, the update scenario is much
rockier. In short, the problem is that every time an object gets updated a new
shadow is created, and then the `ObjectLinks` in the _entire object graph must
be refreshed._

### Updates are Inefficient

Consider the following example of a product, its SKUs, and their images.  

```
                    ┌───────────────────────────┐
                    │ Product (FoxComm T-Shirt) │
                    └─────────────┬─────────────┘
               ┌──────────────────┴────────┬─────────────────────────┐
  ┌────────────┴────────────┐ ┌────────────┴─────────────┐ ┌─────────┴────────┐
  │           SKU           │ │           SKU            │ │       Album      │
  │ (Black FoxComm T-Shirt) │ │ (Orange FoxComm T-Shirt) │ │ (Desktop Images) │
  └────────────┬────────────┘ └────────────┬─────────────┘ └──────────────────┘
               │                           │
      ┌────────┴───────┐          ┌────────┴────────┐
      │      Album     │          │      Album      │
      │ (Black Images) │          │ (Orange Images) │
      └────────────────┘          └─────────────────┘
```

In this case, we'll have the following `ObjectLinks`:  

```Scala
// Links between the Product and each SKU.
ObjectLink(leftId = product.shadow.id, rightId = blackSku.shadow.id, linkType = ProductSku)
ObjectLink(leftId = product.shadow.id, rightId = orangeSku.shadow.id, linkType = ProductSku)

// Link between the Product and it's Album.
ObjectLink(leftId = product.shadow.id, rightId = album.shadow.id, linkType = ProductAlbum)

// Link between each SKU and it's Album.
ObjectLink(leftId = blackSku.shadow.id, rightId = blackAlbum.shadow.id, linkType = SkuAlbum)
ObjectLink(leftId = orangeSku.shadow.id, rightId = orangeAlbum.shadow.id, linkType= SkuAlbum)
```

Now, let's say that the administrator decides to update the title of the orange
SKU to be "Burnt Orange SKU". The following operations will need to happen:

1. `orangeSku` will be updated and will have a new shadow ID.

2. In order for the link to `orangeAlbum` to be valid, a new link needs to be
   generated between the new `orangeSku` shadow and the old `orangeAlbum`
   shadow.<sup>[1](#footnote1)</sup>

   ```Scala
   ObjectLink(leftId = newOrangeSkuShadowId, rightId = orangeAlbum.shadow.id, linkType = SkuAlbum)
   ```

3. Update the link to the `Product`. This will require creating a new shadow
   for `Product` and a link between each object's new shadow.

   ```Scala
   ObjectLink(leftId = newProductShadowId, rightId = newOrangeSkuShadowId, linkType = ProductSku)
   ```

4. Update the link between `Product`'s new shadow and `blackSku`.

  ```Scala
  ObjectLink(leftId = newProductShadowId, rightId = blackSku.shadow.id, linkType = ProductSku)
  ```

As you can see in this simple, very small, example: a lot of tree traversal is
needed for any update operation on an object that's nested deep within a tree.
In a real-world scenario, the object graph is going to be a lot larger, as more
object associations, such as categories, tags, and variants/variators will be
part of the graph.

What we need instead is an algorithm that is more efficient on update operations
even if the potential tradeoff is a less efficient rollback scenario.

## The Solution

The solution to our problem is to leverage `ObjectHeads`<sup>[2](#footnote2)</sup>
as the primary point of connection between objects and overriding the link when
we want to associate specific commits in the past.

Consider this alternate model of an `ObjectLink`:

```Scala
case class ObjectLink(leftHeadId: Int, leftCommitId: Option[Int] = None,
                      rightHeadId: Int, rightCommitId: Option[Int] = None)
```

The core concepts of this new model are:

- `leftHeadId` and `rightHeadId` point to the heads of the objects.
- `leftCommitId` and `rightCommitId` will have a value of `None` by default,
  meaning that objects will be linked with whatever commit is referenced by the
  head.
- In the less likely case that objects need to persist against specific commits,
  `leftCommitId` and `rightCommitId` can reference those, bypassing the commits
  associated with the heads.
- Each type of association with have its own table, so that we can have more
  referential integrity in the database.

### Example I: Updating a SKU

In the 90% case, when we're updating objects that are the most recent versions,
it makes updates far simpler. Consider the example that was shown in the previous
section.

**Footnotes**

<a name="footnote1">*1*</a>: We might actually be better off updating `orangeAlbum`
to have a new shadow ID as well, then you can find its place in object graph
with the Album as a root node. For now, though, let's not worry about it.

<a name="footnote2">*2*</a>: `ObjectHead` is analogous to the head object in a
git branch. It stores the `context` and points to the `ObjectForm` and most
recent `ObjectShadow` and `ObjectCommit` references for an object in a context.
