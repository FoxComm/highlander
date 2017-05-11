--- row

<<< left
# Products Guide

The Fox Platform has a very flexible product model which allows custom attributes and
different versions of the same product in difference channels.
<<<

>>> right
<!-- include(../api-ref-snippet.md) -->
>>>

---

--- row

<<< left
## Products

Products are a collection of one or more variants along with merchandising information
such as a title, description, and images. If a product has many colors or sizes, 
you can add options to the product and assign each combination of options a variant. 


<img class='eimg' src="data/products.png"/>

### Learn more about Products
::: note
[Creating a New Product](products.html)
:::

<<<

>>> right

<br></br>
#### Getting a Product 

Along with the product id, you must specify which view you want.

``` javascript
fox.products.one('default',1343).then( (product) => {
    var title = product.attributes.title;
    //attributes have a type and a value in the 't' and 'v' 
    //keys.
});
```
>>>

---

--- row 

<<< left

## Options

Options provide a way for a customer to select a product based on varying attributes such 
as size and color. Every combination of options is assigned a variant.

### Learn more about Options
::: note
[Adding a Size and Color Option](options.html)
:::

<<<

---

--- row 

<<< left

## Variants

Variants are a specific combination of options of a product such as size and color.
They are associated with a particular inventory item. Variants can also have merchandising
information such as a description and separate images.

### Learn more about Variants
::: note
[Create and Assign a Variant to a Product](variants.html)
:::

<<<

---

<!-- include(../support.md) -->
