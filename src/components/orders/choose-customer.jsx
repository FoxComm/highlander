import React, { Component, PropTypes } from 'react';
import { transitionTo } from '../../route-helpers';
import classnames from 'classnames';
import _ from 'lodash';

import { Button, PrimaryButton } from '../common/buttons';
import ChooseCustomerRow from './choose-customer-row';
import Table from '../table/table';
import WaitAnimation from '../common/wait-animation';

const ChooseCustomer = (props, context) => {
  if (_.isEmpty(props.query)) {
    return <div></div>;
  }

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

  const footer = props.isGuest ? null : (
    <div className="fc-orders-choose-customer__footer">
      <Button>Checkout As Guest</Button>
      <div>or</div>
      <PrimaryButton onClick={() => transitionTo(context.history, 'customers-new')}>
        Create New Customer
      </PrimaryButton>
    </div>
  );

  const data = props.updating ? { rows: [] } : props.items;
  const waitAnimation = props.updating && <WaitAnimation />;
  const hasNoResults = !props.updating && _.isEmpty(_.get(props, 'items.rows'));
  const noResults = hasNoResults
    ? <div className="fc-orders-choose-customer__no-results">No results.</div>
    : null;

  return (
    <div className="fc-orders-choose-customer">
      <Table
        data={data}
        renderRow={renderRow}
        columns={tableColumns} />
      {waitAnimation}
      {noResults}
      {footer}
    </div>
  );
};

ChooseCustomer.propTypes = {
  isGuest: PropTypes.bool,
  items: PropTypes.object.isRequired,
  onItemClick: PropTypes.func,
  updating: PropTypes.bool,
  toggleVisibility: PropTypes.func,
  query: PropTypes.string,
};

ChooseCustomer.defaultProps = {
  isGuest: false,
  onItemClick: _.noop,
  updating: false,
  toggleVisibility: _.noop,
  query: '',
};

ChooseCustomer.contextTypes = {
  history: PropTypes.object.isRequired,
};

export default ChooseCustomer;
