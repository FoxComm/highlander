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
import { AddButton } from '../common/buttons';
import PanelHeader from './panel-header';

import NewPayment from './payments/new-payment';

const viewColumns = [
  {field: 'name', text: 'Method'},
  {field: 'amount', text: 'Amount', type: 'currency'},
  {field: 'status', text: 'Status'},
  {field: 'createdAt', text: 'Date/Time', type: 'datetime'},
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
      isAdding: PropTypes.bool.isRequired,
      isEditing: PropTypes.bool.isRequired,
    }),
    orderPaymentMethodStartAdd: PropTypes.func.isRequired,
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

  state = {
    showDetails: {},
  };

  get currentCustomerId() {
    return _.get(this.props, 'order.currentOrder.customer.id');
  }

  get paymentMethods() {
    return this.props.order.currentOrder.paymentMethods;
  }

  get viewContent() {
    if (_.isEmpty(this.paymentMethods)) {
      return <div className="fc-content-box__empty-text">No payment method applied.</div>;
    } else {
      return (
        <TableView
          columns={viewColumns}
          data={{rows: this.paymentMethods}}
          wrapToTbody={false}
          renderRow={this.renderRow(false)}
        />
      );
    }
  }

  get editContent() {
    return (
      <TableView
        columns={editColumns}
        wrapToTbody={false}
        renderRow={this.renderRow(true)}
        processRows={this.processRows}
        data={{rows: this.paymentMethods}}
        emptyMessage="No payment method applied."
      />
    );
  }

  get editingActions() {
    if (!this.props.payments.isAdding) {
      return <AddButton onClick={this.props.orderPaymentMethodStartAdd} />;
    }
  }

  @autobind
  doneAction() {
    this.props.orderPaymentMethodStopEdit();
  }

  get newPayment() {
    const order = _.get(this.props, 'order.currentOrder');

    return (
      <tbody>
        <NewPayment order={order} customerId={this.currentCustomerId} />
      </tbody>
    );
  }

  @autobind
  renderRow(editMode) {
    return row => {
      const id = row.id || row.code;
      const Renderer = this.getRowRenderer(row.type);

      const props = {
        paymentMethod: row,
        editMode,
        customerId: this.currentCustomerId,
        ...this.props,
        order: _.get(this.props, 'order.currentOrder'),
        showDetails: this.state.showDetails[id],
        toggleDetails: () => {
          this.setState({
            showDetails: {
              ...this.state.showDetails,
              [id]: !this.state.showDetails[id],
            }
          });
        }
      };

      return <Renderer {...props} key={id} />;
    };
  }

  @autobind
  processRows(rows) {
    if (this.props.payments.isAdding) {
      return [
        this.newPayment,
        ...rows
      ];
    }

    return rows;
  }

  getRowRenderer(type) {
    switch(type) {
      case 'giftCard':
        return GiftCard;
      case 'creditCard':
        return CreditCard;
      case 'storeCredit':
        return StoreCredit;
    }
  }

  render() {
    const props = this.props;
    const title = <PanelHeader isCart={props.isCart} status={props.status} text="Payment Method" />;

    const PaymentsContentBox = props.readOnly || !props.isCart
      ? ContentBox
      : EditableContentBox;

    const isCheckingOut = _.get(props, 'order.isCheckingOut', false);
    const editAction = isCheckingOut ? null : props.orderPaymentMethodStartEdit;

    return (
      <PaymentsContentBox
        className="fc-order-payment"
        title={title}
        editContent={this.editContent}
        isEditing={props.payments.isEditing}
        doneAction={this.doneAction}
        editingActions={this.editingActions}
        editAction={editAction}
        indentContent={false}
        viewContent={this.viewContent}
      />
    );
  }
}
