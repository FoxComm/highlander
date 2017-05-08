import React from 'react';
import PropTypes from 'prop-types';
import _ from 'lodash';

import { Button, PrimaryButton } from 'components/core/button';
import ChooseCustomerRow from './choose-customer-row';
import Table from '../table/table';

const ChooseCustomer = (props) => {
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
    { field: 'view', text: '' },
  ];

  const guestCheckoutAction = () => {
    props.onGuestClick();
    props.toggleVisibility(false);
  };

  const footer = props.isGuest ? null : (
    <div className="fc-orders-choose-customer__footer">
      <Button onClick={guestCheckoutAction}>Checkout As Guest</Button>
      <div>or</div>
      <PrimaryButton onClick={props.onNewClick}>
        Create New Customer
      </PrimaryButton>
    </div>
  );

  const data = props.updating ? { rows: [] } : props.items;

  return (
    <div className="fc-orders-choose-customer">
      <Table
        data={data}
        emptyMessage="No results."
        isLoading={props.updating}
        renderRow={renderRow}
        columns={tableColumns} />
      {footer}
    </div>
  );
};

ChooseCustomer.propTypes = {
  isGuest: PropTypes.bool,
  items: PropTypes.object.isRequired,
  onItemClick: PropTypes.func,
  onGuestClick: PropTypes.func.isRequired,
  onNewClick: PropTypes.func.isRequired,
  updating: PropTypes.bool,
  toggleVisibility: PropTypes.func,
};

ChooseCustomer.defaultProps = {
  isGuest: false,
  onItemClick: _.noop,
  updating: false,
  toggleVisibility: _.noop,
};

export default ChooseCustomer;
