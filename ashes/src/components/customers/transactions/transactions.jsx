import React from 'react';
import PropTypes from 'prop-types';
import { actions } from '../../../modules/customers/transactions';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import _ from 'lodash';
import * as dsl from '../../../elastic/dsl';

import ListPage from '../../list-page/list-page';
import OrderTransactionRow from './transaction-row';

const mapDispatchToProps = dispatch => {
  return { actions: bindActionCreators(actions, dispatch) };
};

@connect((state, props) => ({
  list: state.customers.transactions,
  customer: state.customers.details[props.params.customerId].details,
}), mapDispatchToProps)
export default class CustomerTransactions extends React.Component {

  static propTypes = {
    list: PropTypes.object.isRequired,
    params: PropTypes.object.isRequired,
    actions: PropTypes.object.isRequired,
  };

  componentDidMount() {
    this.props.actions.setExtraFilters([
      dsl.nestedTermFilter('customer.id', this.props.customer.id)
    ]);
    this.props.actions.fetch();
  }

  render() {
    const searchOptions = {
      singleSearch: true,
    };

    const renderRow = (row, index, columns, params) => {
      const key = `order-${row.referenceNumber}`;
      return (
        <OrderTransactionRow key={key}
                             order={row}
                             columns={columns}
                             params={params} />
      );
    };

    const tableColumns = [
      {field: 'referenceNumber', text: 'Order', model: 'order'},
      {field: 'placedAt', text: 'Date/Time', type: 'datetime'},
      {field: 'customer.modality', text: 'Modality'},
      {field: 'state', text: 'Order State', type: 'state', model: 'order'},
      {field: 'payment.state', text: 'Payment State'},
      {field: 'assignee', text: 'Assignee'},
      {field: 'grandTotal', text: 'Total', type: 'currency'},
      //{field: 'return', text: 'Return'},
    ];

    return (
      <div className="fc-customer-transactions">
        <ListPage
          entity="customers.transactions"
          addTitle="Order"
          emptyMessage="No orders found."
          list={this.props.list}
          renderRow={renderRow}
          tableColumns={tableColumns}
          searchActions={this.props.actions}
          searchOptions={searchOptions}
          title="Orders"/>
      </div>
    );
  }
}
