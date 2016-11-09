/* @flow */

// libs
import _ from 'lodash';
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
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
  isDeleting: boolean,
  quantity: number|string,
};

type DefaultProps = {
  updateLineItemCount: Function,
};

export class CartLineItem extends Component {
  props: Props;

  state: State = {
    isDeleting: false,
    quantity: this.props.item.quantity,
  };

  defaultProps: DefaultProps = {
    updateLineItemCount: () => {},
  };

  @autobind
  startDelete() {
    this.setState({ isDeleting: true });
  }

  @autobind
  updateCartPayload(sku: string, qtt: number) {
    const { skus } = this.props.cart.lineItems;

    return _.map(skus, (item) => {
      const quantity = sku === item.sku ? qtt : item.quantity;
      return {
        sku: item.sku,
        quantity,
      };
    });
  }

  @autobind
  updateLineItemCount(referenceNumber: string, sku: string, qtt: number) {
    const payload = this.updateCartPayload(sku, qtt);
    this.props.updateLineItemCount(referenceNumber, payload);
  }

  @autobind
  cancelDelete() {
    this.setState({ isDeleting: false });
    if (this.state.quantity == 0) {
      const { cart, item } = this.props;
      this.setState({quantity: 1});
      this.updateLineItemCount(cart.referenceNumber, item.sku, 1);
    }
  }

  @autobind
  confirmDelete() {
    const { cart, item } = this.props;

    this.setState({
      isDeleting: false,
    }, this.updateLineItemCount(cart.referenceNumber, item.sku, 0));
  }

  @autobind
  decreaseCount() {
    const { cart, item } = this.props;
    const { quantity } = this.state;

    if (quantity == 1 || !quantity) {
      this.startDelete();
    } else {
      const decreased = parseInt(quantity, 10) - 1;
      this.setState({quantity: decreased});

      this.updateLineItemCount(cart.referenceNumber, item.sku, decreased);
    }
  }

  @autobind
  increaseCount() {
    const { cart, item } = this.props;
    const { quantity } = this.state;

    const increased = quantity ? item.quantity + 1 : 1;

    this.setState({quantity: increased});

    this.updateLineItemCount(cart.referenceNumber, item.sku, increased);
  }

  @autobind
  changeCount({ target: { value }}: {target: Target}) {
    const quantity = value ? parseInt(value, 10) : '';

    this.setState({quantity});

    if (quantity === '') return;

    if (quantity == 0) {
      this.startDelete();
    } else {
      const { cart, item } = this.props;
      this.updateLineItemCount(cart.referenceNumber, item.sku, quantity);
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

export default connect(null, { updateLineItemCount })(CartLineItem);
