/**
 * @flow
 */

// libs
import React, { Component, Element } from 'react';
import PropTypes from 'prop-types';

// components
import { Link, IndexLink } from 'components/link';
import PageNav from 'components/core/page-nav';

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

  get detailsLinks(): ?Element<*>[] {
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
      <PageNav>
        <IndexLink to="product-details" params={this.props}>Details</IndexLink>
        {this.detailsLinks}
      </PageNav>
    );
  }
}
