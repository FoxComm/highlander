/* @flow */

import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import _ from 'lodash';

import CreditCard from './credit-card';
import GiftCard from './gift-card';
import StoreCredit from './store-credit';
import TableView from 'components/table/tableview';

import { Cart, Order, PaymentMethod } from 'paragons/order';

type Props = {
  order: Cart|Order,
  paymentMethods: Array<PaymentMethod>,
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

export default class PaymentsPanel extends Component {
  props: Props;
  state: State = { showDetails: {} };

  getRowRenderer(type: string): Object {
    switch(type) {
      case 'giftCard':
        return GiftCard;
      case 'creditCard':
        return CreditCard;
      case 'storeCredit':
        return StoreCredit;
      default:
        throw 'Unexpected payment method type!';
    }
  }

  @autobind
  renderRow(row: PaymentMethod): Element {
    const { order, paymentMethods } = this.props;
    const customerId = order.customer.id;

    const id = row.id || row.code;
    if (!id) {
      throw 'Unable to render payment method without code or ID';
    }

    const Renderer = this.getRowRenderer(row.type);
    const props = {
      key: `payments-panel-row-${id}`,
      paymentMethod: row,
      editMode: false,
      customerId: customerId,
      order: order,
      showDetails: this.state.showDetails[id],
      toggleDetails: () => this.toggleDetails(id),
    };

    return <Renderer {...props} />;
  }

  @autobind
  toggleDetails(id: number|string) {
    this.setState({
      [id]: !this.state.showDetails[id],
    });
  }

  render(): Element {
    const { paymentMethods } = this.props;

    if (_.isEmpty(paymentMethods)) {
      return (
        <div className="fc-content-box__empty-text">
          No payment method applied.
        </div>
      );
    } else {
      return (
        <TableView
          columns={viewColumns}
          data={{rows: paymentMethods}}
          wrapToTbody={false}
          renderRow={this.renderRow} />
      );
    }
  }
}
