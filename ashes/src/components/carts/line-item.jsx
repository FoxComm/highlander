/* @flow */

// libs
import _ from 'lodash';
import classNames from 'classnames';
import React, { Component, Element } from 'react';
import { autobind, debounce } from 'core-decorators';
import { connect } from 'react-redux';

// components
import { Link } from 'components/link';
import ConfirmationModal from 'components/core/confirmation-modal';
import Counter from 'components/core/counter';
import { DeleteButton } from 'components/core/button';
import Currency from 'components/common/currency';
import ProductImage from 'components/imgix/product-image';

// actions
import { updateLineItemCount } from 'modules/carts/details';

import type { SkuItem } from 'paragons/order';

type Props = {
  updateLineItemCount: Function,
  cart: {
    referenceNumber: string,
    lineItems: {
      skus: Array<any>,
    },
  },
  item: SkuItem,
  className?: string,
};

type State = {
  isDeleting: boolean;
  lastSyncedQuantity: number;
  quantity: number;
};

export class CartLineItem extends Component {
  props: Props;

  state: State = {
    isDeleting: false,
    quantity: this.props.item.quantity,
    lastSyncedQuantity: this.props.item.quantity,
  };

  @autobind
  startDelete() {
    this.setState({ isDeleting: true });
  }

  @autobind
  @debounce(300)
  performUpdate() {
    const { cart: { referenceNumber }, item: { sku, attributes } } = this.props;
    const { quantity, lastSyncedQuantity } = this.state;

    const quantityDiff = quantity - lastSyncedQuantity;

    this.setState({
      lastSyncedQuantity: quantity,
    }, () => this.props.updateLineItemCount(referenceNumber, sku, quantityDiff, attributes));

  }

  @autobind
  cancelDelete() {
    this.setState({ isDeleting: false });
  }

  @autobind
  confirmDelete() {
    this.setState({
      isDeleting: false,
      quantity: 0,
    }, this.performUpdate());
  }

  @autobind
  handleQuantityChange(quantity: number) {
    this.setState({ quantity }, this.performUpdate);
  }

  render() {
    const { item, className } = this.props;
    const { isDeleting, quantity } = this.state;
    const skuQtyInput = _.kebabCase(item.sku);

    return (
      <tr className={classNames('line-item', className)}>
        <td>
          <ProductImage src={item.imagePath} width={50} height={50} />
        </td>
        <td className="line-item-name">{item.name}</td>
        <td><Link to="sku-details" params={{ skuCode: item.sku }}>{item.sku}</Link></td>
        <td><Currency className="item-price" value={item.price} /></td>
        <td class="line-item-quantity">
          <Counter
            id={`fct-counter-input__${skuQtyInput}`}
            value={quantity}
            min={1}
            step={1}
            onChange={this.handleQuantityChange}
          />
        </td>
        <td><Currency className="item-total-price" value={item.totalPrice} /></td>
        <td>
          <DeleteButton onClick={this.startDelete} />
          <ConfirmationModal
            isVisible={isDeleting}
            label="Are you sure you want to delete this item?"
            confirmLabel="Yes, Delete"
            onConfirm={this.confirmDelete}
            onCancel={this.cancelDelete}
          />
        </td>
      </tr>
    );
  }
}

export default connect(null, { updateLineItemCount })(CartLineItem);
