'use strict';

import _ from 'lodash';
import React from 'react';
import EditableContentBox from '../content-box/editable-content-box';
import PaymentMethod from './payment-method';
import TableHead from '../tables/head';
import TableBody from '../tables/body';

const OrderPayment = (props) => {
  const order = props.order.currentOrder;

  const viewContent = (
    <table className="fc-table">
      <TableHead columns={props.tableColumns}/>
      <TableBody columns={props.tableColumns} rows={_.compact([order.payment])} model='payment-method'>
        <PaymentMethod/>
      </TableBody>
    </table>
  );

  return (
    <EditableContentBox
      className='fc-order-payment'
      title='Payment'
      isEditing={false}
      editAction={() => console.log('Not implemented')}
      viewContent={viewContent} />
  );
};

OrderPayment.propTypes = {
  order: React.PropTypes.object,
  tableColumns: React.PropTypes.array
};

OrderPayment.defaultProps = {
  tableColumns: [
    {field: 'paymentMethod', text: 'Method', component: 'PaymentMethod'},
    {field: 'amount', text: 'Amount', type: 'currency'},
    {field: 'status', text: 'Status'},
    {field: 'createdAt', text: 'Date/Time', type: 'date'}
  ]
};

export default OrderPayment;