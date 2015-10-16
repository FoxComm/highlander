'use strict';

import React from 'react';
import TableHead from '../tables/head';
import TableBody from '../tables/body';

const columns = [
  {field: 'name', text: 'Method'},
  {field: 'price', text: 'Price', type: 'currency'}
];

let OrderShippingMethod = (props) => {
  return (
    <section className="fc-content-box" id="order-shipping-method">
      <header>Shipping Method</header>
      <table className="fc-table">
        <TableHead columns={columns} />
        <TableBody columns={columns} rows={[props.order.shippingMethod]} model='shipping-method' />
      </table>
    </section>
  );
};

export default OrderShippingMethod;
