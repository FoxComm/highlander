import React, { PropTypes } from 'react';
import { actions } from '../../../modules/customers/transactions';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import _ from 'lodash';

import ListPage from '../../list-page/list-page';
import OrderTransactionRow from './order-txn-row';

const mapDispatchToProps = dispatch => {
  return { actions: bindActionCreators(actions, dispatch) };
};

const searchUrl = "orders_search_view/_search";

@connect((state, props) => ({
  list: state.customers.transactions,
  customer: state.customers.details[props.params.customerId].details,
}), mapDispatchToProps)
export default class CustomerTransactions extends React.Component {

  componentDidMount() {
    this.props.actions.addSearchFilter(searchUrl, [{
      display: "Customer: " + this.props.customer.email,
      selectedTerm: "customer.email", // FIXME: change to customer.id when ES will support this mapping
      selectedOperator: "eq",
      hidden: true,
      value: {
        type: "string",
        value: this.props.customer.email
      }
    }]);
  }

  render() {
    const searchOptions = {
      single: true,
    };

    const renderRow = (row, index, columns) => {
      const key = `order-${row.referenceNumber}`;
      return <OrderTransactionRow order={row} columns={columns} key={key} />;
    };

    const tableColumns = [
      {field: 'referenceNumber', text: 'Order', type: 'text', model: 'order'},
      {field: 'placedAt', text: 'Date/Time', type: 'datetime'},
      {field: 'customer.modality', text: 'Modality'},
      {field: 'status', text: 'Order Status', type: 'status', model: 'order'},
      {field: 'payment.status', text: 'Payment Status'},
      {field: 'assignee', text: 'Assignee'},
      {field: 'grandTotal', text: 'Total'},
      //{field: 'return', text: 'Return'},
    ];

    return (
      <div className="fc-customer-transactions">
        <ListPage
        addTitle="Order"
        emptyResultMessage="No orders found."
        list={this.props.list}
        renderRow={renderRow}
        tableColumns={tableColumns}
        searchActions={this.props.actions}
        searchOptions={searchOptions}
        title="Orders"
        url={searchUrl} />
      </div>
    );
  }
}
