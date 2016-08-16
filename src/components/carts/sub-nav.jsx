/* @flow */
import React, { Element } from 'react';
import { Link, IndexLink } from 'components/link';
import LocalNav from 'components/local-nav/local-nav';

type Props = {
  cart: {
    referenceNumber: string,
  },
};

const SubNav = (props: Props): Element => {
  const cart = props.cart;
  const params = {cart: cart.referenceNumber};

  return (
    <LocalNav>
      <IndexLink to="cart-details" params={params}>Details</IndexLink>
      <Link to="cart-notes" params={params}>Notes</Link>
      <Link to="cart-activity-trail" params={params}>Activity Trail</Link>
    </LocalNav>
  );
};

export default SubNav;
