/**
 * @flow
 */

// libs
import React, { Component, PropTypes } from 'react';

// components
import { Link, IndexLink } from '../link';
import LocalNav from '../local-nav/local-nav';

// types
import type { FullProduct } from '../../modules/products/details';

type Props = {
  productId: string,
  product: ?FullProduct,
};

export default class SubNav extends Component<void, Props, void> {
  static propTypes = {
    productId: PropTypes.string.isRequired,
    product: PropTypes.object,
  };
  
  render() {
    const params = {
      productId: this.props.productId,
      product: this.props.product,
    };

    return (
      <LocalNav>
        <IndexLink to="product-details" params={params}>Details</IndexLink>
        <Link to="product-images" params={params}>Images</Link>
        <Link to="product-notes" params={params}>Notes</Link>
        <Link to="product-activity-trail" params={params}>Activity Trail</Link>
      </LocalNav>
    );
  }
}
