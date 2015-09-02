'use strict';

import React from 'react';
import PaymentMethod from './payment-method';
import TableHead from '../table/head';
import TableBody from '../table/body';

export default class OrderPayment extends React.Component {
  render() {
    let order = this.props.order;

    return (
      <section id="order-payment">
        <header>Payment</header>
        <table className="fc-table">
          <TableHead columns={this.props.tableColumns}/>
          <TableBody columns={this.props.tableColumns} rows={order.payments} model='order'>
            <PaymentMethod/>
          </TableBody>
        </table>
      </section>
    );
  }
}

OrderPayment.propTypes = {
  order: React.PropTypes.object,
  tableColumns: React.PropTypes.array
};

OrderPayment.defaultProps = {
  tableColumns: [
    {field: 'method', text: 'Method', component: 'PaymentMethod'},
    {field: 'amount', text: 'Amount', type: 'currency'},
    {field: 'status', text: 'Status'},
    {field: 'createdAt', text: 'Date/Time', type: 'date'}
  ]
};
