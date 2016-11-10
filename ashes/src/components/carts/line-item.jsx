/* @flow */

// libs
import _ from 'lodash';
import React, { Component, Element } from 'react';
import { autobind, debounce } from 'core-decorators';
import { connect } from 'react-redux';

// components
import ConfirmationDialog from 'components/modal/confirmation-dialog';
import Counter from 'components/forms/counter';
import { DeleteButton } from 'components/common/buttons';
import Currency from 'components/common/currency';

// actions
import { updateLineItemCount } from 'modules/carts/details';

type Props = {
  updateLineItemCount: Function,
  cart: {
    referenceNumber: string,
    lineItems: {
      skus: Array<any>,
    },
  },
  item: {
    imagePath: string,
    name: string,
    sku: string,
    price: number,
    totalPrice: number,
    quantity: number,
  },
};

type Target = {
  value: string|number,
};

type State = {
  isDeleting: boolean;
  lastSyncedQuantity: number;
  quantity: number;
};

type DefaultProps = {
  updateLineItemCount: Function,
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
    const { cart: { referenceNumber }, item: { sku } } = this.props;
    const { quantity, lastSyncedQuantity } = this.state;

    const quantityDiff = quantity - lastSyncedQuantity;

    this.setState({
      lastSyncedQuantity: quantity,
    }, () => this.props.updateLineItemCount(referenceNumber, sku, quantityDiff));

  }

  @autobind
  cancelDelete() {
    this.setState({ isDeleting: false });

    if (this.state.quantity == 0) {
      this.setState({ quantity: 1 });
      this.performUpdate();
    }
  }

  @autobind
  confirmDelete() {
    this.setState({
      isDeleting: false,
    }, this.performUpdate());
  }

  @autobind
  decreaseCount() {
    const quantity = this.state.quantity - 1;

    if (quantity > 0) {
      this.setState({ quantity });
      this.performUpdate();
    }
  }

  @autobind
  increaseCount() {
    this.setState({ quantity: this.state.quantity + 1 });

    this.performUpdate();
  }

  @autobind
  changeCount({ target: { value } }: {target: Target}) {
    const quantity = value ? parseInt(value, 10) : '';

    this.setState({ quantity });

    if (quantity === '') return;

    if (quantity == 0) {
      this.startDelete();
    } else {
      const { cart, item } = this.props;
      this.performUpdate(cart.referenceNumber, item.sku, quantity);
    }
  }

  render() {
    const { item } = this.props;
    const { isDeleting, quantity } = this.state;

    return (
      <tr>
        <td><img src={item.imagePath} /></td>
        <td>{item.name}</td>
        <td>{item.sku}</td>
        <td><Currency value={item.price} /></td>
        <td>
          <Counter
            id={`line-item-quantity-${item.sku}`}
            value={quantity}
            min={1}
            max={1000000}
            step={1}
            onChange={this.changeCount}
            decreaseAction={this.decreaseCount}
            increaseAction={this.increaseCount} />
        </td>
        <td><Currency value={item.totalPrice} /></td>
        <td>
          <DeleteButton onClick={this.startDelete} />
          <ConfirmationDialog
            isVisible={isDeleting}
            header="Confirm"
            body="Are you sure you want to delete this item?"
            cancel="Cancel"
            confirm="Yes, Delete"
            onCancel={this.cancelDelete}
            confirmAction={this.confirmDelete} />
        </td>
      </tr>
    );
  }
}
;

export default connect(null, { updateLineItemCount })(CartLineItem);
