'use strict';

import _ from 'lodash';
import React, { PropTypes } from 'react';
import {PaymentMethod} from './helpers';
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
  rma: PropTypes.object,
  tableColumns: PropTypes.array
};

RmaPayment.defaultProps = {
  tableColumns: [
    {field: 'paymentMethod', text: 'Method', component: 'PaymentMethod'},
    {field: 'amount', text: 'Amount', type: 'currency'},
    {field: 'status', text: 'Status'},
    {field: 'createdAt', text: 'Date/Time', type: 'date'}
  ]
};
