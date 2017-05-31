import React from 'react';
import PropTypes from 'prop-types';

import { Link, IndexLink } from 'components/link';
import PageNav from 'components/core/page-nav';

// helpers
import { getClaims } from 'lib/claims';
import { frn, readAction } from 'lib/frn';

const notesClaims = readAction(frn.note.order);
const shippingClaims = readAction(frn.mdl.shipment);
const activityClaims = readAction(frn.activity.order);

const SubNav = ({ order, isAmazon, className = '' }) => {
  const params = { order: order.referenceNumber };
  const claims = getClaims();

  if (isAmazon) {
    return (
      <PageNav className={className}>
        <IndexLink to="amazon-order-details" params={params}>Details</IndexLink>
      </PageNav>
    );
  }

  return (
    <PageNav className={className}>
      <IndexLink to="order-details" params={params}>Details</IndexLink>
      <Link to="order-shipments" expectedClaims={shippingClaims} actualClaims={claims} params={params}>Shipments</Link>
      <Link to="order-notes" expectedClaims={notesClaims} actualClaims={claims} params={params}>Notes</Link>
      <Link to="order-activity-trail" expectedClaims={activityClaims} actualClaims={claims} params={params}>
        Activity Trail
      </Link>
    </PageNav>
  );
};

SubNav.propTypes = {
  order: PropTypes.object
};

export default SubNav;
