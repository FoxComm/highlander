/* @flow */

import _ from 'lodash';
import { autobind } from 'core-decorators';
import React, { Component, Element } from 'react';
import { trackEvent } from 'lib/analytics';

import EditableContentBox from 'components/content-box/editable-content-box';
import CartLineItem from './line-item';
import CartLineItemMore from './line-item-more';
import CartLineItemsFooter from './line-items-footer';
import PanelHeader from 'components/panel-header/panel-header';
import SkuLineItems from 'components/sku-line-items/sku-line-items';

import type { SkuItem } from 'paragons/order';

const columns = [
  { field: 'imagePath', text: 'Image', type: 'image' },
  { field: 'name', text: 'Name' },
  { field: 'sku', text: 'SKU' },
  { field: 'price', text: 'Price', type: 'currency' },
  { field: 'lineItem', text: 'Qty', component: 'LineItemCounter' },
  { field: 'totalPrice', text: 'Total', type: 'currency' },
  { field: 'delete', text: '', component: 'DeleteLineItem' }
];

type Props = {
  cart: {
    referenceNumber: string,
    lineItems: Array<Object>,
    isCheckingOut: boolean,
  },
  status: string,
};

type State = {
  isEditing: boolean,
};

const giftCardColumns = [
    { field: 'code', text: 'Gift Card Number' },
    { field: 'recipientName', text: 'Recipient Name' },
    { field: 'recipientEmail', text: 'Recipient Email' },
    { field: 'senderName', text: 'Sender Name' },
    { field: 'recipientEmail', text: 'Message' },
];

export default class CartLineItems extends Component {
  props: Props;
  state: State = { isEditing: false };

  get editContent(): Element {
    const { cart } = this.props;

    const renderRow = (item: SkuItem) => {
      const key = `sku-line-item-${item.sku}`;
      return <CartLineItem key={key} item={item} cart={cart} />;
    };

    return <SkuLineItems items={this.skus} columns={columns} renderRow={renderRow} />;
  }

  get skus(): Array<Object> {
    return _.get(this.props, 'cart.lineItems.skus', []);
  }

  render() {
    const { cart, status } = this.props;

    const title = <PanelHeader showStatus={true} status={status} text="Items" />;
    const isCheckingOut = cart.isCheckingOut;
    const editAction = isCheckingOut ? null : () => {
      trackEvent('Orders', 'edit_line_items');
      this.setState({ isEditing: true });
    };

    const doneAction = () => {
      trackEvent('Orders', 'edit_line_items_done');
      this.setState({ isEditing: false });
    };

    const renderRow = (item: SkuItem) => {
      const key = `sku-line-item-${item.sku}`;
      return [
        <CartLineItem key={key} item={item} cart={cart} />,
        <CartLineItemMore spanNumber={columns.length} columns={giftCardColumns} data={[]} />
      ]
    };

    const editFooter = <CartLineItemsFooter cart={cart} />;
    const viewContent = <SkuLineItems columns={columns} items={this.skus} renderRow={renderRow} />;

    return (
      <EditableContentBox
        className='fc-line-items'
        title={title}
        isEditing={this.state.isEditing}
        editAction={editAction}
        doneAction={doneAction}
        editContent={this.editContent}
        editFooter={editFooter}
        indentContent={false}
        viewContent={viewContent} />
    );
  }
}
