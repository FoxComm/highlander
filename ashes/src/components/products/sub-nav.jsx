/**
 * @flow
 */

// libs
import React, { Component, PropTypes } from 'react';

// components
import { Link, IndexLink } from '../link';
import LocalNav from '../local-nav/local-nav';

// helpers
import { getClaims } from 'lib/claims';
import { frn, readAction } from 'lib/frn';

// types
import type { Product } from 'paragons/product';

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

  render() {
    const params = {
      productId: this.props.productId,
      product: this.props.product,
      context: this.props.context
    };

    const actualClaims = getClaims();

    return (
      <LocalNav>
        <IndexLink
          to="product-details"
          params={params}
          actualClaims={actualClaims}
          expectedClaims={detailsClaim}>
          Details
        </IndexLink>
        <Link
          to="product-images"
          params={params}
          actualClaims={actualClaims}
          expectedClaims={imagesClaim}>
          Images
        </Link>
        <Link
          to="product-notes"
          params={params}
          actualClaims={actualClaims}
          expectedClaims={notesClaim}>
          Notes
        </Link>
        <Link
          to="product-activity-trail"
          params={params}
          actualClaims={actualClaims}
          expectedClaims={activityClaim}>
          Activity Trail
        </Link>
      </LocalNav>
    );
  }
}
