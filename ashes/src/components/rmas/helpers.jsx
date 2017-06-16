import React from 'react';
import PropTypes from 'prop-types';

import { Link } from 'components/link';
import TableRow from '../table/row';
import TableCell from '../table/cell';
import { DateTime } from 'components/utils/datetime';
import Currency from '../common/currency';
import PaymentMethod from '../../components/payment/payment-method';

const CustomerInfo = props => {
  return (
    <div className="fc-rma-summary fc-content-box">
      <header className="fc-content-box-header">Message for Customer</header>
      <article>
        {props.rma.customerMessage}
      </article>
    </div>
  );
};

CustomerInfo.propTypes = {
  rma: PropTypes.object
};

const renderRow = (row, index) => {
  return (
    <TableRow key={`${index}`}>
      <TableCell><Link to="rma" params={{rma: row.referenceNumber}}>{row.referenceNumber}</Link></TableCell>
      <TableCell><DateTime value={row.createdAt} /></TableCell>
      <TableCell><Link to="order" params={{order: row.orderRefNum}}>{row.orderRefNum}</Link></TableCell>
      <TableCell>{row.customer.email}</TableCell>
      <TableCell>{row.state}</TableCell>
      <TableCell><Currency value={row.total} /></TableCell>
    </TableRow>
  );
};

export {
  CustomerInfo,
  PaymentMethod,
  renderRow
};
