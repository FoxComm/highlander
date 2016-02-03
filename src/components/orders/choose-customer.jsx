import React, { Component, PropTypes } from 'react';
import { transitionTo } from '../../route-helpers';
import _ from 'lodash';

import { Button, PrimaryButton } from '../common/buttons';
import ChooseCustomerRow from './choose-customer-row';
import Table from '../table/table';

const ChooseCustomer = (props, context) => {
  const renderRow = (row, index, columns) => {
    const key = `order-choose-customer-${row.id}`;
    const clickItem = () => {
      props.toggleVisibility(false);
      props.onItemClick(row);
    };

    return <ChooseCustomerRow customer={row} onClick={clickItem} key={key} />;
  };

  const tableColumns = [
    { field: 'orderSummary', text: 'Customer' },
    { field: 'orderCount', text: 'Total Orders' },
    { field: 'accountType', text: 'Account Type' },
  ];

  const primaryButton = (
    <PrimaryButton onClick={() => transitionTo(context.history, 'customers-new')}>
      Create New Customer
    </PrimaryButton>
  );

  return (
    <div className="fc-orders-choose-customer">
      <Table
        className="_adaptable"
        data={props.items}
        renderRow={renderRow}
        columns={tableColumns} />
      <div className="fc-orders-choose-customer__footer">
        <Button>Checkout As Guest</Button>
        <div className="fc-orders-choose-customer__footer-divider">or</div>
        {primaryButton}
      </div>
    </div>
  );
};

ChooseCustomer.propTypes = {
  items: PropTypes.object.isRequired,
  onItemClick: PropTypes.func,
  updating: PropTypes.bool,
  toggleVisibility: PropTypes.func,
};

ChooseCustomer.defaultProps = {
  onItemClick: _.noop,
  updating: false,
  toggleVisibility: _.noop,
};

ChooseCustomer.contextTypes = {
  history: PropTypes.object.isRequired,
};

export default ChooseCustomer;
