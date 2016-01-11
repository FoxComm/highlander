import React, { PropTypes } from 'react';

import _ from 'lodash';
import { transitionTo } from '../../route-helpers';


import { DateTime } from '../common/datetime';
import { Checkbox } from '../checkbox/checkbox';
import Currency from '../common/currency';
import Link from '../link/link';
import Status from '../common/status';
import TableCell from '../table/cell';
import TableRow from '../table/row';

const CustomerRow = (props, context) => {
  const { customer, columns, ...rest } = props;
  const key = `customer-${customer.id}`;
  const clickAction = () => {
    transitionTo(context.history, 'customer', { customerId: customer.id });
  };

  const cells = _.reduce(columns, (visibleCells, col) => {
    const cellKey = `${key}-${col.field}`;
    let cellContents = null;
    let cellClickAction = clickAction;

    switch (col.field) {
      case 'id':
        cellContents = customer.id;
        break;
      case 'name':
        cellContents = customer.name;
        break;
      case 'email':
        cellContents = customer.email;
        break;
      case 'shipToRegion':
        cellContents = _.get(customer, ['shippingAddresses', 0, 'region'], '');
        break;
      case 'billToRegion':
        cellContents = _.get(customer, ['billingAddresses', 0, 'region'], '');
        break;
      case 'rank':
        cellContents = _.get(customer, 'rank', 'N/A');
        break;
      case 'toggleColumns':
        cellContents = '';
        break;
      case 'selectColumn':
        cellClickAction = _.noop;
        cellContents = <Checkbox />;
        break;
      default:
        return visibleCells;
    }

    visibleCells.push(
      <TableCell onClick={cellClickAction} key={cellKey} column={col}>
        {cellContents}
      </TableCell>
    );

    return visibleCells;
  }, []);

  return (
    <TableRow {...rest}>
      {cells}
    </TableRow>
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
