import _ from 'lodash';
import React from 'react';
import PropTypes from 'prop-types';
import TableView from '../table/tableview';
import ContentBox from '../content-box/content-box';

export default class RmaPayment extends React.Component {
  render() {
    return (
      <ContentBox title="Payment" id="payment">
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
    {field: 'state', text: 'State'},
    {field: 'createdAt', text: 'Date/Time', type: 'date'}
  ]
};
