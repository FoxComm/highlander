/* @flow */

// libs
import _ from 'lodash';
import React, { Component } from 'react';
import { autobind } from 'core-decorators';

// styles
import styles from './line-item.css';

// localization
import localized from 'lib/i18n';

// components
import Currency from 'ui/currency';
import Select from 'ui/select/select';
import ActionLink from 'ui/action-link/action-link';
import ProductImage from 'components/image/image';

const QUANTITY_ITEMS = _.range(1, 1 + 10, 1).map(x => x.toString());

type Props = {
  sku: string,
  name: string,
  imagePath: string,
  price: number,
  totalPrice: number,
  quantity: number,
  deleteLineItem: Function,
  updateLineItemQuantity: Function,
};

class LineItem extends Component {
  props: Props;

  @autobind
  changeQuantity(quantity) {
    this.props.updateLineItemQuantity(this.props, quantity);
  }

  @autobind
  deleteItem() {
    this.props.deleteLineItem(this.props);
  }

  quantityItems() {
    if (this.props.quantity > 8) {
      return _.range(1, this.props.quantity + 3, 1).map(x => x.toString());
    }

    return QUANTITY_ITEMS;
  }

  render() {
    return (
      <div styleName="box">
        <div styleName="image">
          <ProductImage src={this.props.imagePath} width={50} height={50} />
        </div>
        <div styleName="container">
          <div styleName="top">
            {this.props.quantity} X {this.props.name}
          </div>
          <div styleName="details">
            <div styleName="price-qty">
              <div styleName="price">
                <Currency value={this.props.price} />
              </div>
            </div>
          </div>
        </div>
        <div styleName="delete">
          <ActionLink
            action={this.deleteItem}
            styleName="action-link-remove"
          >
            &nbsp;
          </ActionLink>
        </div>
      </div>
    );
  }
}

export default localized(LineItem);
