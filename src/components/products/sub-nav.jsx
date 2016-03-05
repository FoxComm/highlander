/**
 * @flow
 */

// libs
import React, { Component } from 'react';

// components
import { Link, IndexLink } from '../link';
import LocalNav from '../local-nav/local-nav';

// helpers
import Product from '../../paragons/product';

type Props = {
  productId: number,
  product: ?Product,
};

export default class SubNav extends Component<void, Props, void> {
  render() {
    const params = {
      productId: this.props.productId,
      product: this.props.product,
    };

    return (
      <LocalNav>
        <IndexLink to="product-details" param={params}>Details</IndexLink>
        <a href="">Notes</a>
        <a href="">ActivityTrail</a>
      </LocalNav>
    );
  }
}
