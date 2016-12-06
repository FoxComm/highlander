import React, { PropTypes } from 'react';
import { Link, IndexLink } from '../link';
import LocalNav from '../local-nav/local-nav';

// helpers
import { getClaims, isPermitted } from 'lib/claims';
import { frn, readAction } from 'lib/frn';

const notesClaims = readAction(frn.note.order);
const shippingClaims = readAction(frn.mdl.shipment);
const activityClaims = readAction(frn.activity.order);

const SubNav = props => {
  const order = props.order;
  const params = {order: order.referenceNumber};
  const claims = getClaims();

  return (
    <LocalNav>
      <IndexLink to="order-details" params={params}>Details</IndexLink>
      <Link to="order-shipments" params={params} expectedClaims={shippingClaims} actualClaims={claims}>Shipments</Link>
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
