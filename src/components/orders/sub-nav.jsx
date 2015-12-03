import React, { PropTypes } from 'react';
import { Link, IndexLink } from '../link';
import LocalNav from '../local-nav/local-nav';

const SubNav = props => {
  const order = props.order;
  const params = {order: order.referenceNumber};

  if (order.isCart) {
    return (
      <LocalNav>
        <IndexLink to="order-details" params={params}>Details</IndexLink>
        <Link to="order-notifications" params={params}>Notifications</Link>
        <Link to="order-notes" params={params}>Notes</Link>
        <Link to="order-activity-trail" params={params}>Activity Trail</Link>
      </LocalNav>
    );
  } else {
    return (
      <LocalNav>
        <IndexLink to="order-details" params={params}>Details</IndexLink>
        <a href="">Shipments</a>
        <Link to="order-returns" params={params}>Returns</Link>
        <Link to="order-notifications" params={params}>Notifications</Link>
        <Link to="order-notes" params={params}>Notes</Link>
        <Link to="order-activity-trail" params={params}>Activity Trail</Link>
      </LocalNav>
    );
  }
}

export default SubNav;
