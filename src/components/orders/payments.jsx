import _ from 'lodash';
import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';

import * as paymentActions from '../../modules/orders/payment-methods';

import EditableContentBox from '../content-box/editable-content-box';
import ContentBox from '../content-box/content-box';
import TableView from '../table/tableview';
import GiftCard from './payments/gift-card';
import StoreCredit from './payments/store-credit';
import CreditCard from './payments/credit-card';
import Dropdown from '../dropdown/dropdown';
import { AddButton } from '../common/buttons';
import PanelHeader from './panel-header';

const viewColumns = [
  {field: 'name', text: 'Method'},
  {field: 'amount', text: 'Amount', type: 'currency'}
];

const editColumns = viewColumns.concat([
  {field: 'edit'}
]);

@connect(state => ({ payments: state.orders.paymentMethods }), paymentActions)
export default class Payments extends React.Component {
  static propTypes = {
    isCart: PropTypes.bool,
    order: PropTypes.shape({
      currentOrder: PropTypes.shape({
        paymentMethods: PropTypes.array
      })
    }).isRequired,
    status: PropTypes.string,
    payments: PropTypes.shape({
      isEditing: PropTypes.bool.isRequired
    }),
    orderPaymentMethodStartEdit: PropTypes.func.isRequired,
    orderPaymentMethodStopEdit: PropTypes.func.isRequired,

    deleteOrderGiftCardPayment: PropTypes.func.isRequired,
    deleteOrderStoreCreditPayment: PropTypes.func.isRequired,
    deleteOrderCreditCardPayment: PropTypes.func.isRequired,

    readOnly: PropTypes.bool,
  };

  static defaultProps = {
    readOnly: false,
  };

  get editContent() {
    const paymentMethods = this.props.order.currentOrder.paymentMethods;

    const paymentTypes = {
      giftCard: 'Gift Card',
      creditCard: 'Credit Card',
      storeCredit: 'Store Credit'
    };

    return (
      <TableView
        columns={editColumns}
        data={{rows: paymentMethods}}
        renderRow={this.renderRow(true)}
      />
    );
  }

  get viewContent() {
    const paymentMethods = this.props.order.currentOrder.paymentMethods;

    if (_.isEmpty(paymentMethods)) {
      return <div className="fc-content-box__empty-text">No payment method applied.</div>;
    } else {
      return (
        <TableView
          columns={viewColumns}
          data={{rows: paymentMethods}}
          renderRow={this.renderRow(false)}
        />
      );
    }
  }

  @autobind
  renderRow(isEditing) {
    return (row, index, isNew) => {
      switch(row.type) {
        case 'giftCard':
          return <GiftCard paymentMethod={row} isEditing={isEditing} {...this.props} />;
        case 'creditCard':
          return <CreditCard paymentMethod={row} isEditing={isEditing} {...this.props} />;
        case 'storeCredit':
          return <StoreCredit paymentMethod={row} isEditing={isEditing} {...this.props} />;
      }
    };
  }

  render() {
    const props = this.props;

    const title = <PanelHeader isCart={props.isCart} status={props.status} text="Payment Method" />;
    const PaymentsContentBox = props.readOnly ? ContentBox : EditableContentBox;
    return (
      <PaymentsContentBox
        className="fc-order-payment"
        title={title}
        isTable={true}
        editContent={this.editContent}
        isEditing={props.payments.isEditing}
        editAction={props.orderPaymentMethodStartEdit}
        doneAction={props.orderPaymentMethodStopEdit}
        indentContent={false}
        viewContent={this.viewContent}
      />
    );
  }
}

