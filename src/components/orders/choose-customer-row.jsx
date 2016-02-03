import React, { PropTypes } from 'react';
import { transitionTo } from '../../route-helpers';
import classNames from 'classnames';
import _ from 'lodash';

import { Button } from '../common/buttons';
import TableCell from '../table/cell';
import TableRow from '../table/row';

const ChooseCustomerRow = (props, context) => {
  const { customer, onClick } = props;

  const activeText = customer.isDisabled ? 'Inactive' : 'Active';
  const guestText = customer.isGuest ? 'Guest' : null;
  const className = classNames('fc-choose-customer-row', {
    '_inactive': customer.isDisabled
  });

  const viewAction = () => transitionTo(context.history, 'customer', {
    customerId: customer.id
  });

  const clickAction = customer.isDisabled ? _.noop : onClick;

  return (
    <TableRow className={className} onClick={clickAction}>
      <TableCell>
        <div className="fc-choose-customer-row__name">{customer.name}</div>
        <div className="fc-choose-customer-row__email">{customer.email}</div>
      </TableCell>
      <TableCell>
        {props.customer.orderCount}
      </TableCell>
      <TableCell>
        <div className="fc-choose-customer-row__account-type">
          <div className="fc-choose-customer-row__active">{activeText}</div>
          <div className="fc-choose-customer-row__guest">{guestText}</div>
          <div className="fc-choose-customer-row_view-customer" onClick={viewAction}>
            <Button>View</Button>
          </div>
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

ChooseCustomerRow.contextTypes = {
  history: PropTypes.object.isRequired,
};

export default ChooseCustomerRow;
