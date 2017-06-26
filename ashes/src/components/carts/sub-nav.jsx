/* @flow */
import React, { Element } from 'react';

import { Link, IndexLink } from 'components/link';
import PageNav from 'components/core/page-nav';

import type { Cart } from 'paragons/order';

type Props = {
  cart: Cart,
};

const SubNav = (props: Props) => {
  const cart = props.cart;
  const params = {cart: cart.referenceNumber};

  return (
    <PageNav>
      <IndexLink to="cart-details" params={params}>Details</IndexLink>
      <Link to="cart-notes" params={params}>Notes</Link>
      <Link to="cart-activity-trail" params={params}>Activity Trail</Link>
    </PageNav>
  );
};

export default SubNav;
