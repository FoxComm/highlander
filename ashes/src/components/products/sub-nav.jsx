/**
 * @flow
 */

// libs
import React, { Component, PropTypes, Element } from 'react';

// components
import { Link, IndexLink } from '../link';
import LocalNav from '../local-nav/local-nav';

// types
import type { Product } from 'paragons/product';

type Props = {
  productId: string,
  product: ?Product,
  context: string
};

export default class SubNav extends Component<void, Props, void> {
  static propTypes = {
    productId: PropTypes.string.isRequired,
    product: PropTypes.object,
    context: PropTypes.string
  };

  get isNew(): boolean {
    return this.props.productId === 'new';
  }

  get detailsLinks(): ?Element[] {
    if (this.isNew) {
      return;
    }

    return [
      <Link to="product-images" params={this.props} key="images">Images</Link>,
      <Link to="product-notes" params={this.props} key="notes">Notes</Link>,
      <Link to="product-analytics" params={this.props} key="analytics">Analytics</Link>,
      <Link to="product-activity-trail" params={this.props} key="activity-trail">Activity Trail</Link>,
    ];
  }

  render() {
    return (
      <LocalNav>
        <IndexLink to="product-details" params={this.props}>Details</IndexLink>
        {this.detailsLinks}
      </LocalNav>
    );
  }
}
