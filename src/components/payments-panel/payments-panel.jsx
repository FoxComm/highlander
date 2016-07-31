/* @flow */

import React, { Component, Element } from 'react';
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

  get newPayment(): Element {
    const { order } = this.props;
    const customerId = order.customer.id;

    return (
      <tbody>
        <NewPayment order={order} customerId={customerId} />
      </tbody>
    );
  }

  @autobind
  processRows(rows: Array<Object>): Array<Object> {
    if (this.props.isAdding) {
      return [
        this.newPayment,
        ...rows,
      ]
    }

    return rows;
  }

  //getRowDetails(type: string): Function {
    //switch(type) {
      //case 'giftCard':
        //return GiftCard;
      //case 'creditCard':
        //return CreditCard;
      //case 'storeCredit':
        //return StoreCredit;
      //default:
        //throw 'Unexpected payment method type!';
    //}
  //}

  @autobind
  renderRow(row: PaymentMethod): Element {
    const { order } = this.props;

    const customerId = order.customer.id;
    const referenceNumber = order.referenceNumber;

    return (
      <PaymentRow
        customerId={customerId}
        editMode={this.props.isEditing}
        orderReferenceNumber={referenceNumber}
        paymentMethod={row} />
    );
    //const { order, paymentMethods } = this.props;
    //const customerId = order.customer.id;

    //const id = row.id || row.code;
    //if (!id) {
      //throw 'Unable to render payment method without code or ID';
    //}

    //const Renderer = this.getRowRenderer(row.type);
    //const props = {
      //key: `payments-panel-row-${id}`,
      //paymentMethod: row,
      //editMode: this.props.isEditing,
      //customerId: customerId,
      //order: order,
      //showDetails: this.state.showDetails[id],
      //toggleDetails: () => this.toggleDetails(id),
    //};

    //return <Renderer {...props} />;
  }

  @autobind
  toggleDetails(id: number|string) {
    this.setState({
      showDetails: {
        [id]: !this.state.showDetails[id],
      },
    });
  }

  get viewContent(): Element {
    const { isAdding, isEditing, paymentMethods } = this.props;
    const editColumns = isEditing ? [{ field: 'edit' }] : [];

    if (!isEditing && _.isEmpty(paymentMethods)) {
      return (
        <div className="fc-content-box__empty-text">
          No payment method applied.
        </div>
      );
    } else {
      const columns = [...viewColumns, ...editColumns];
      let processRows = _.identity;
      if (isAdding) {
        processRows = this.processRows;
      }

      return (
        <TableView
          columns={columns}
          wrapToTbody={false}
          renderRow={this.renderRow}
          processRows={processRows}
          data={{rows: paymentMethods}}
          emptyMessage="No payment method applied" />
      );
    }
  }

  render(): Element {
    return this.viewContent;
  }
}
