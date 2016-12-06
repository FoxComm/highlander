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
    attributes: ?Object,
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
  handleButtonClick(diff: number) {
    const quantity = this.state.quantity + diff;

    if (quantity > 0) {
      this.setState({ quantity }, this.performUpdate);
    }
  }

  @autobind
  handleInputChange({ target: { value } }: {target: Target}) {
    const quantity = value ? parseInt(value, 10) : null;

    if (!quantity || quantity < 1) {
      return;
    }

    this.setState({ quantity }, this.performUpdate);
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
            onChange={this.handleInputChange}
            decreaseAction={() => this.handleButtonClick(-1)}
            increaseAction={() => this.handleButtonClick(1)}
          />
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
