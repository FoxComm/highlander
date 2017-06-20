/* @flow */

import React from 'react';

// libs
import _ from 'lodash';

// components
import MultiSelectRow from '../table/multi-select-row';

type Props = {
  customer: Object,
  columns: Columns,
  params: Object,
};

const CustomerRow = (props: Props) => {
  const { customer, columns, params } = props;

  const setCellContents = (customer: Object, field: string) => {
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

  return (
    <MultiSelectRow
      columns={columns}
      linkTo="customer"
      linkParams={{customerId: customer.id}}
      row={customer}
      setCellContents={setCellContents}
      params={params}
    />
  );
};

export default CustomerRow;
