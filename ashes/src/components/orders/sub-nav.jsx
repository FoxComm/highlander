import React from 'react';
import PropTypes from 'prop-types';
import { Link, IndexLink } from '../link';
import LocalNav from '../local-nav/local-nav';

// helpers
import { getClaims, isPermitted } from 'lib/claims';
import { frn, readAction } from 'lib/frn';

// styles
import s from './order.css';

const notesClaims = readAction(frn.note.order);
const shippingClaims = readAction(frn.mdl.shipment);
const activityClaims = readAction(frn.activity.order);

const SubNav = ({ order, isAmazon, className = '' }) => {
  const params = { order: order.referenceNumber };
  const claims = getClaims();

  if (isAmazon) {
    return (
      <LocalNav className={className}>
        <IndexLink to="amazon-order-details" params={params}>Details</IndexLink>
      </LocalNav>
    );
  }

  return (
    <LocalNav className={className}>
      <IndexLink to="order-details" params={params}>Details</IndexLink>
      <Link to="order-shipments" expectedClaims={shippingClaims} actualClaims={claims} params={params}>Shipments</Link>
      <Link to="order-notes" expectedClaims={notesClaims} actualClaims={claims} params={params}>Notes</Link>
      <Link to="order-activity-trail" expectedClaims={activityClaims} actualClaims={claims} params={params}>
        Activity Trail
      </Link>
    </LocalNav>
  );
};

SubNav.propTypes = {
  order: PropTypes.object
};

export default SubNav;
