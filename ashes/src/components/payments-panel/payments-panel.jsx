/* @flow */

import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import _ from 'lodash';

import NewPayment from 'components/new-payment/new-payment';
import PaymentRow from 'components/payment-row/payment-row';
import TableView from 'components/table/tableview';

import { Cart, Order, PaymentMethod } from 'paragons/order';

type Props = {
  isAdding: boolean,
  isEditing: boolean,
  order: Cart|Order,
  paymentMethods: Array<PaymentMethod>,
  cancelAdding?: () => void,
};

type DefaultProps = {
  isAdding: boolean,
  isEditing: boolean,
};

type State = {
  showDetails: { [key:number|string]: boolean },
};

const viewColumns = [
  {field: 'name', text: 'Method'},
  {field: 'amount', text: 'Amount', type: 'currency'},
  {field: 'status', text: 'Status'},
  {field: 'createdAt', text: 'Date/Time', type: 'datetime'},
];

const editColumns = viewColumns.concat([
  {field: 'edit'},
]);

export default class PaymentsPanel extends Component {
  static defaultProps: DefaultProps = {
    isAdding: false,
    isEditing: false,
  };

  props: Props;
  state: State = { showDetails: {} };

  get newPayment() {
    const { order } = this.props;
    const customerId = order.customer.id;

    return (
      <tbody key="new-payment">
        <NewPayment
          order={order}
          customerId={customerId}
          cancelAction={this.props.cancelAdding}
        />
      </tbody>
    );
  }

  @autobind
  processRows(rows: Array<Object>): Array<Object> {
    if (this.props.isAdding) {
      return [
        this.newPayment,
        ...rows,
      ];
    }

    return rows;
  }

  @autobind
  renderRow(row: PaymentMethod) {
    const { order } = this.props;

    const customerId = order.customer.id;
    const referenceNumber = order.referenceNumber;

    return (
      <PaymentRow
        key={row.id || row.code}
        customerId={customerId}
        editMode={this.props.isEditing}
        orderReferenceNumber={referenceNumber}
        paymentMethod={row}
      />
    );
  }

  @autobind
  toggleDetails(id: number|string) {
    this.setState({
      showDetails: {
        [id]: !this.state.showDetails[id],
      },
    });
  }

  get viewContent(){
    const { isEditing, paymentMethods } = this.props;
    const editColumns = isEditing ? [{ field: 'edit' }] : [];

    if (!isEditing && _.isEmpty(paymentMethods)) {
      return (
        <div className="fc-content-box__empty-text">
          No payment method applied.
        </div>
      );
    } else {
      const columns = [...viewColumns, ...editColumns];

      return (
        <TableView
          columns={columns}
          wrapToTbody={false}
          renderRow={this.renderRow}
          processRows={this.processRows}
          data={{rows: paymentMethods}}
          emptyMessage="No payment method applied"
        />
      );
    }
  }

  render() {
    return this.viewContent;
  }
}
