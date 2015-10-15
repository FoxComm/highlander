'use strict';

import React from 'react';
import TableHead from '../tables/head';
import TableBody from '../tables/body';

export default class OrderShippingMethod extends React.Component {
  static propTypes = {
    order: React.PropTypes.object
  }

  static defaultProps = {
    viewColumns: [
      {field: 'name', text: 'Method'},
      {field: 'price', text: 'Price', type: 'currency'}
    ]
  }

  constructor(props, context) {
    super(props, context);
  }

  render() {
    return (
      <section className="fc-content-box" id="order-shipping-method">
        <header>Shipping Method</header>
        <table className="fc-table">
          <TableHead columns={this.props.viewColumns} />
          <TableBody columns={this.props.viewColumns} rows={[this.props.order.shippingMethod]} model='shipping-method'>
          </TableBody>
        </table>
      </section>
    );
  }
}
