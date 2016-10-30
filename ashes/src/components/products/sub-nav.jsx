/**
 * @flow
 */

// libs
import React, { Component, PropTypes, Element } from 'react';

// components
import { Link, IndexLink } from '../link';
import LocalNav from '../local-nav/local-nav';

// helpers
import { getClaims } from 'lib/claims';
import { frn, readAction } from 'lib/frn';

// types
import type { Product } from 'paragons/product';
import type { Claims } from 'lib/claims';

type Props = {
  productId: string,
  product: ?Product,
  context: string
};

const detailsClaim = readAction(frn.pim.product);
const imagesClaim = readAction(frn.pim.album);
const notesClaim = readAction(frn.note.product);
const activityClaim = readAction(frn.activity.product);

export default class SubNav extends Component<void, Props, void> {
  static propTypes = {
    productId: PropTypes.string.isRequired,
    product: PropTypes.object,
    context: PropTypes.string
  };

  get isNew(): boolean {
    return this.props.productId === 'new';
  }

  detailsLinks(claims: Claims): ?Element[] {
    if (this.isNew) {
      return;
    }

    return [
      <Link
        to="product-images"
        params={this.props}
        actualClaims={claims}
        expectedClaims={imagesClaim}
        key="images">
        Images
      </Link>,
      <Link
        to="product-notes"
        params={this.props}
        actualClaims={claims}
        expectedClaims={notesClaim}
        key="notes">
        Notes
      </Link>,
      <Link
        to="product-activity-trail"
        params={this.props}
        actualClaims={claims}
        expectedClaims={activityClaim}
        key="activity-trail">
        Activity Trail
      </Link>,
    ];
  }

  render() {
    const actualClaims = getClaims();

    return (
      <LocalNav>
        <IndexLink
          to="product-details"
          params={this.props}
          actualClaims={actualClaims}
          expectedClaims={detailsClaim}>
          Details
        </IndexLink>
        {this.detailsLinks(actualClaims)}
      </LocalNav>
    );
  }
}
