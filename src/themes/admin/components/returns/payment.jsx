'use strict';

import _ from 'lodash';
import React from 'react';
import PaymentMethod from './payment-method';
import TableHead from '../tables/head';
import TableBody from '../tables/body';

export default class ReturnPayment extends React.Component {
  render() {
    let retrn = this.props.return;

    return (
      <section className="fc-content-box" id="return-payment">
        <header className="header">Payment</header>
        <table className="fc-table">
          <TableHead columns={this.props.tableColumns}/>
          <TableBody columns={this.props.tableColumns} rows={_.compact([retrn.payment])} model='return'>
            <PaymentMethod/>
          </TableBody>
        </table>
      </section>
    );
  }
}

ReturnPayment.propTypes = {
  return: React.PropTypes.object,
  tableColumns: React.PropTypes.array
};

ReturnPayment.defaultProps = {
  tableColumns: [
    {field: 'paymentMethod', text: 'Method', component: 'PaymentMethod'},
    {field: 'amount', text: 'Amount', type: 'currency'},
    {field: 'status', text: 'Status'},
    {field: 'createdAt', text: 'Date/Time', type: 'date'}
  ]
};
