import React, { PropTypes } from 'react';
import classNames from 'classNames';

import { Button } from '../common/buttons';
import TableCell from '../table/cell';
import TableRow from '../table/row';

const ChooseCustomerRow = props => {
  const { customer } = props;

  const activeText = customer.isDisabled ? 'Inactive' : 'Active';
  const guestText = customer.isGuest ? 'Guest' : null;
  const className = classNames('fc-choose-customer-row', {
    '_inactive': customer.isDisabled
  });

  return (
    <TableRow className={className}>
      <TableCell>
        <div className="fc-choose-customer-row__name">{customer.name}</div>
        <div classname="fc-choose-customer-row__email">{customer.email}</div>
      </TableCell>
      <TableCell>
        {props.customer.orderCount}
      </TableCell>
      <TableCell>
        <div className="fc-choose-customer-row__account-type">
          <div className="fc-choose-customer-row__active">{activeText}</div>
          <div className="fc-choose-customer-row__guest">{guestText}</div>
          <div className="fc-choose-customer-row_view-customer">
            <Button>View</Button>
          </div>
        </div>
      </TableCell>
    </TableRow>
  );
};

ChooseCustomerRow.propTypes = {
  customer: PropTypes.node.isRequired,
};

export default ChooseCustomerRow;
