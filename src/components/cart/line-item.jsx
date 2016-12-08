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
import Icon from 'ui/icon';
import Currency from 'ui/currency';
import Select from 'ui/select/select';

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
    this.props.updateLineItemQuantity(this.props.sku, quantity);
  }

  @autobind
  deleteItem() {
    this.props.deleteLineItem(this.props.sku);
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
          <img src={this.props.imagePath} />
        </div>
        <div styleName="container">
          <div styleName="details">
            <div styleName="product-name">
              {this.props.name}
            </div>
            <div styleName="price">
              <Currency value={this.props.totalPrice}/>
            </div>
          </div>
          <div styleName="quantity">
            <Select
              inputProps={{
                type: 'number',
              }}
              getItemValue={item => item}
              items={this.quantityItems()}
              onSelect={this.changeQuantity}
              selectedItem={this.props.quantity}
              sortItems={false}
            />
          </div>
        </div>
        <div styleName="controls">
          <a styleName="delete-button" onClick={this.deleteItem}>
            <Icon name="fc-close" styleName="delete-icon" />
          </a>
        </div>
      </div>
    );
  }
}

export default localized(LineItem);
