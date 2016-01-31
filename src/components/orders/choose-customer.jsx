import React, { Component, PropTypes } from 'react';

import { Button, PrimaryButton } from '../common/buttons';
import ChooseCustomerRow from './choose-customer-row';
import Table from '../table/table';

export default class ChooseCustomer extends Component {
  static propTypes = {
    items: PropTypes.object.isRequired,
  };

  get tableColumns() {
    return [
      { field: 'orderSummary', text: 'Customer' },
      { field: 'orderCount', text: 'Total Orders' },
      { field: 'accountType', text: 'Account Type' }
    ];
  }

  get renderRow() {
    return (row, index, columns) => {
      const key = `order-choose-customer-${row.id}`;
      return <ChooseCustomerRow customer={row} columns={columns} key={key} />;
    };
  }

  render() {
    return (
      <div className="fc-orders-choose-customer">
        <Table
          className="_adaptable"
          data={this.props.items}
          renderRow={this.renderRow}
          columns={this.tableColumns} />
        <div className="fc-orders-choose-customer__footer">
          <Button>Checkout As Guest</Button>
          <div className="fc-orders-choose-customer__footer-divider">or</div>
          <PrimaryButton>Create New Customer</PrimaryButton>
        </div>
      </div>
    );
  }
}
