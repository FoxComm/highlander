'use strict';

import _ from 'lodash';
import React from 'react';
import PaymentMethod from './payment-method';
import TableHead from '../tables/head';
import TableBody from '../tables/body';

export default class OrderPayment extends React.Component {
  render() {
    let order = this.props.order;

    return (
      <section className="fc-contentBox" id="order-payment">
        <header>Payment</header>
        <div className="fc-contentBox-body">
          <table className="fc-table">
            <TableHead columns={this.props.tableColumns}/>
            <TableBody columns={this.props.tableColumns} rows={_.compact([order.payment])} model='order'>
              <PaymentMethod/>
            </TableBody>
          </table>
        </div>
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
    {field: 'paymentMethod', text: 'Method', component: 'PaymentMethod'},
    {field: 'amount', text: 'Amount', type: 'currency'},
    {field: 'status', text: 'Status'},
    {field: 'createdAt', text: 'Date/Time', type: 'date'}
  ]
};
