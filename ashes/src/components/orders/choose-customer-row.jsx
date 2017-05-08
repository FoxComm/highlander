import React from 'react';
import PropTypes from 'prop-types';
import { transitionTo } from 'browserHistory';
import classNames from 'classnames';
import _ from 'lodash';

import { Button } from 'components/core/button';
import TableCell from '../table/cell';
import TableRow from '../table/row';

const ChooseCustomerRow = (props) => {
  const { customer, onClick } = props;

  const activeText = customer.isDisabled ? 'Inactive' : 'Active';
  const guestText = customer.isGuest ? 'Guest' : null;
  const className = classNames('fc-choose-customer-row', {
    '_inactive': customer.isDisabled
  });

  const viewAction = () => transitionTo('customer', {
    customerId: customer.id
  });

  const clickAction = customer.isDisabled ? _.noop : onClick;

  return (
    <TableRow className={className} onClick={clickAction}>
      <TableCell>
        <div className="fc-choose-customer-row__name">{customer.name}</div>
        <div className="fc-choose-customer-row__email">{customer.email}</div>
        <div className="fc-choose-customer-row__phone">{customer.phoneNumber}</div>
      </TableCell>
      <TableCell>
        {props.customer.orderCount}
      </TableCell>
      <TableCell>
        <div className="fc-choose-customer-row__account-type">
          <div className="fc-choose-customer-row__active">{activeText}</div>
          <div className="fc-choose-customer-row__guest">{guestText}</div>
        </div>
      </TableCell>
      <TableCell>
        <div className="fc-choose-customer-row_view-customer" onClick={viewAction}>
          <Button>View</Button>
        </div>
      </TableCell>
    </TableRow>
  );
};

ChooseCustomerRow.propTypes = {
  customer: PropTypes.object.isRequired,
  onClick: PropTypes.func,
};

ChooseCustomerRow.defaultProps = {
  onClick: _.noop,
};

export default ChooseCustomerRow;
