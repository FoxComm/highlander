import React, { PropTypes } from 'react';

import _ from 'lodash';
import { transitionTo } from '../../route-helpers';

import { DateTime } from '../common/datetime';
import { Checkbox } from '../checkbox/checkbox';
import Currency from '../common/currency';
import Link from '../link/link';
import Status from '../common/status';
import MultiSelectRow from '../table/multi-select-row';

const setCellContents = (customer, field) => {
  switch (field) {
    case 'id':
      return customer.id;
    case 'name':
      return customer.name;
    case 'email':
      return customer.email;
    case 'shipRegion':
      return _.get(customer, ['shippingAddresses', 0, 'region'], '');
    case 'billRegion':
      return _.get(customer, ['billingAddresses', 0, 'region'], '');
    case 'rank':
      return _.isNull(customer.rank) ? 'N/A' : customer.rank;
    case 'joinedAt':
      return _.get(customer, 'joinedAt', '');
    default:
      return null;
  }
};

const CustomerRow = (props, context) => {
  const { customer, columns } = props;
  const key = `customer-${customer.id}`;
  const clickAction = () => {
    transitionTo(context.history, 'customer', { customerId: customer.id });
  };

  return (
    <MultiSelectRow
      cellKeyPrefix={key}
      columns={columns}
      onClick={clickAction}
      row={customer}
      setCellContents={setCellContents} />
  );
};

CustomerRow.propTypes = {
  customer: PropTypes.object,
  columns: PropTypes.array
};

CustomerRow.contextTypes = {
  history: PropTypes.object.isRequired
};

export default CustomerRow;
