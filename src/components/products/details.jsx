/**
 * @flow
 */

import React from 'react';

type ProductParams = {
  productId: number
};

type DetailsProps = {
  params: ProductParams
};

const ProductDetails = (props: DetailsProps) => {
  return <div>Details for product {props.params.productId}</div>;
};

export default ProductDetails;
