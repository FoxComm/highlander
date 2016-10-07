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

type Target = {
  value: string|number,
};

type State = {
  isDeleting: boolean,
  quantity: number|string,
};

type DefaultProps = {
  updateLineItemCount: Function,
  deleteLineItem: Function,
};

export class CartLineItem extends Component {
  props: Props;

  state: State = {
    isDeleting: false,
    quantity: this.props.item.quantity,
  };

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
    if (this.state.quantity == 0) {
      const { cart, item, updateLineItemCount } = this.props;
      this.setState({quantity: 1});
      updateLineItemCount(cart.referenceNumber, item.sku, 1);
    }
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
    const { quantity } = this.state;

    if (quantity == 1 || !quantity) {
      this.startDelete();
    } else {
      const decreased = parseInt(quantity, 10) - 1;
      this.setState({quantity: decreased});
      updateLineItemCount(cart.referenceNumber, item.sku, decreased);
    }
  }

  @autobind
  increaseCount() {
    const { cart, item, updateLineItemCount } = this.props;
    const { quantity } = this.state;

    const increased = quantity ? item.quantity + 1 : 1;

    this.setState({quantity: increased});

    updateLineItemCount(cart.referenceNumber, item.sku, increased);
  }

  @autobind
  changeCount({ target: { value }}: {target: Target}) {
    const quantity = value ? parseInt(value, 10) : '';

    this.setState({quantity});

    if (quantity === '') return;

    if (quantity == 0) {
      this.startDelete();
    } else {
      const {cart, item, updateLineItemCount} = this.props;
      updateLineItemCount(cart.referenceNumber, item.sku, quantity);
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
        <td><Currency value={item.price}/></td>
        <td>
          <Counter
            id={`line-item-quantity-${item.sku}`}
            value={quantity}
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
