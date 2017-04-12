import React, { PropTypes } from 'react';
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

const SubNav = props => {
  const { order } = props;
  const params = {order: order.referenceNumber};
  const claims = getClaims();
  const rest = { params, activeClassName: s.lactive };

  return (
    <LocalNav className={props.className}>
      <IndexLink to="order-details" {...rest}>Details</IndexLink>
      <Link to="order-shipments" expectedClaims={shippingClaims} actualClaims={claims} {...rest}>Shipments</Link>
      <Link to="order-notes" expectedClaims={notesClaims} actualClaims={claims} {...rest}>Notes</Link>
      <Link to="order-activity-trail" expectedClaims={activityClaims} actualClaims={claims} {...rest}>
        Activity Trail
      </Link>
    </LocalNav>
  );
};

SubNav.propTypes = {
  order: PropTypes.object
};

export default SubNav;
