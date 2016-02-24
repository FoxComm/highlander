/**
 * @flow
 */

import React, { Component } from 'react';

type ProductParams = {
  productId: number
};

type DetailsProps = {
  params: ProductParams
};

export default class ProductDetails extends Component {
  render() {
    return <div>Details for product {this.props.params.productId}</div>;
  }
}
