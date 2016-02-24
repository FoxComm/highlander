/**
 * @flow
 */

import React from 'react';

type ProductProps = {
  children: Object
};

const Product = (props: ProductProps) => {
  return <div>Product {props.children}</div>;
};

export default Product;
