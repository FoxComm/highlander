/* @flow */

import _ from 'lodash';
import classNames from 'classnames';
import { autobind } from 'core-decorators';
import React, { Component, Element } from 'react';
import { trackEvent } from 'lib/analytics';

import EditableContentBox from 'components/content-box/editable-content-box';
import CartLineItem from './line-item';
import CartLineItemAttributes from './line-item-attributes';
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

const attributesColumns = {
  'giftCard': [
    {
      field: 'code',
      text: 'Gift Card Number',
      render: code => !_.isEmpty() ? <Link to="giftcard" params={{ giftCard: code }}>{code}</Link> : 'N/A'
    },
    { field: 'recipientName', text: 'Recipient Name' },
    { field: 'recipientEmail', text: 'Recipient Email' },
    { field: 'senderName', text: 'Sender Name' },
    { field: 'message', text: 'Message', type: 'html' },
  ],
};

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

  lineItemAttributes(item: Object): Array<Element> {
    const attributes = _.get(item, 'attributes', {});

    if (!_.isEmpty(attributes)) {
      return Object.keys(attributes).map((name: string) => (
        _.get(attributesColumns, name) ?
          <CartLineItemAttributes
            spanNumber={columns.length}
            columns={attributesColumns[name]}
            data={{rows: [attributes[name]]}}
          /> : null
      ));
    }

    return [];
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
      const attributes = _.get(item, 'attributes');
      const cls = classNames({ '_with-attributes': !_.isEmpty(attributes) });

      return [
        <CartLineItem className={cls} key={key} item={item} cart={cart} />,
        ...this.lineItemAttributes(item),
      ];
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
