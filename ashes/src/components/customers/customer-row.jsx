import React from 'react';
import PropTypes from 'prop-types';
import _ from 'lodash';

import MultiSelectRow from '../table/multi-select-row';

const setCellContents = (customer, field) => {
  switch (field) {
    case 'id':
    case 'name':
    case 'email':
      return _.get(customer, field);
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

const CustomerRow = props => {
  const { customer, columns, params } = props;
  const key = `customer-${customer.id}`;

  return (
    <MultiSelectRow
      columns={columns}
      linkTo="customer"
      linkParams={{customerId: customer.id}}
      row={customer}
      setCellContents={setCellContents}
      params={params} />
  );
};

CustomerRow.propTypes = {
  customer: PropTypes.object,
  columns: PropTypes.array,
  params: PropTypes.object,
};

export default CustomerRow;
