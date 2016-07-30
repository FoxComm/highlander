/* @flow */

import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';

import ConfirmationDialog from 'components/modal/confirmation-dialog';
import Counter from 'components/forms/counter';
import { DeleteButton } from 'components/common/buttons';
import Currency from 'components/common/currency';

import { updateLineItemCount, deleteLineItem } from 'modules/carts/details';

type Props = {
  updateLineItemCount: Function,
  deleteLineItem: Function,
  cart: {
    referenceNumber: string,
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

type State = {
  isDeleting: boolean,
};

type DefaultProps = {
  updateLineItemCount: Function,
  deleteLineItem: Function,
};

export class CartLineItem extends Component {
  props: Props;
  state: State = { isDeleting: false };
  defaultProps: DefaultProps = {
    updateLineItemCount: () => {},
    deleteLineItem: () => {},
  };

  @autobind
  startDelete() {
    this.setState({ isDeleting: true });
  }

  @autobind
  cancelDelete() {
    this.setState({ isDeleting: false });
  }

  @autobind
  confirmDelete() {
    const { cart, item, deleteLineItem } = this.props;
    this.setState({
      isDeleting: false,
    }, deleteLineItem(cart.referenceNumber, item.sku));
  }

  @autobind
  decreaseCount() {
    const { cart, item, updateLineItemCount } = this.props;
    if (item.quantity == 1) {
      this.startDelete();
    } else {
      updateLineItemCount(cart.referenceNumber, item.sku, item.quantity - 1);
    }
  }

  @autobind
  increaseCount() {
    const { cart, item, updateLineItemCount } = this.props;
    updateLineItemCount(cart.referenceNumber, item.sku, item.quantity + 1);
  }

  @autobind
  changeCount(count: string) {
    const countNum = parseInt(count, 10);
    if (countNum == 0) {
      this.startDelete();
    } else {
      const { cart, item, updateLineItemCount } = this.props;
      updateLineItemCount(cart.referenceNumber, item.sku, countNum);
    }
  }

  render() {
    const { cart, item, updateLineItemCount, deleteLineItem } = this.props;
    const { isDeleting } = this.state;

    const toNumber = value => {
      return value ? parseInt(value, 10) : 1;
    };

    return (
      <tr>
        <td><img src={item.imagePath} /></td>
        <td>{item.name}</td>
        <td>{item.sku}</td>
        <td><Currency value={item.price}/></td>
        <td>
          <Counter
            id={`line-item-quantity-${item.sku}`}
            value={item.quantity}
            min={0}
            max={1000000}
            step={1}
            onChange={this.changeCount}
            decreaseAction={this.decreaseCount}
            increaseAction={this.increaseCount} />
        </td>
        <td><Currency value={item.totalPrice}/></td>
        <td>
          <DeleteButton onClick={this.startDelete} />
          <ConfirmationDialog
            isVisible={isDeleting}
            header="Confirm"
            body="Are you sure you want to delete this item?"
            cancel="Cancel"
            confirm="Yes, Delete"
            cancelAction={this.cancelDelete}
            confirmAction={this.confirmDelete} />
        </td>
      </tr>
    );
  }
};

export default connect(null, { updateLineItemCount, deleteLineItem })(CartLineItem);
