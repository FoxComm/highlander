import _ from 'lodash';
import React, { PropTypes } from 'react';
import EditableContentBox from '../content-box/editable-content-box';
import PaymentMethod from './payment-method';
import TableView from '../table/tableview';
import ContentBox from '../content-box/content-box';

const OrderPayment = props => {
  const order = props.order.currentOrder;

  return (
    <EditableContentBox
      className='fc-order-payment'
      title='Payment'
      isEditing={false}
      editAction={() => console.log('Not implemented')}
      viewContent={<TableView columns={props.tableColumns} data={{rows: _.compact([order.payment])}} />}/>
  );
};

OrderPayment.propTypes = {
  order: PropTypes.object,
  tableColumns: PropTypes.array
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
