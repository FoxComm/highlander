'use strict';

import _ from 'lodash';
import React from 'react';
import PaymentMethod from './payment-method';
import TableHead from '../tables/head';
import TableBody from '../tables/body';
import ContentBox from '../content-box/content-box';

export default class RmaPayment extends React.Component {
  render() {
    return (
      <ContentBox title="Payment">
        <table className="fc-table">
          <TableHead columns={this.props.tableColumns}/>
          <TableBody columns={this.props.tableColumns} rows={_.compact([this.props.rma.payment])} model='rma'>
            <PaymentMethod/>
          </TableBody>
        </table>
      </ContentBox>
    );
  }
}

RmaPayment.propTypes = {
  rma: React.PropTypes.object,
  tableColumns: React.PropTypes.array
};

RmaPayment.defaultProps = {
  tableColumns: [
    {field: 'paymentMethod', text: 'Method', component: 'PaymentMethod'},
    {field: 'amount', text: 'Amount', type: 'currency'},
    {field: 'status', text: 'Status'},
    {field: 'createdAt', text: 'Date/Time', type: 'date'}
  ]
};
