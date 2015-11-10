import _ from 'lodash';
import React, { PropTypes } from 'react';
import {PaymentMethod} from './helpers';
import TableView from '../table/tableview';
import ContentBox from '../content-box/content-box';

export default class RmaPayment extends React.Component {
  render() {
    return (
      <ContentBox title="Payment">
        <TableView columns={this.props.tableColumns} data={{rows: _.compact([this.props.rma.payment])}} />
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
