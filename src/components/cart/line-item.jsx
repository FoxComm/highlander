
/* @flow weak */

// libs
import _ from 'lodash';
import React, { Component } from 'react';

// styles
import styles from './line-item.css';

// localization
import localized from 'lib/i18n';

// components
import Icon from 'ui/icon';
import Currency from 'ui/currency';
import Autocomplete from 'ui/autocomplete';

const quantity = _.range(1, 1 + 10, 1).map(x => x.toString());

type Props = {
  sku: string,
  name: string,
  imagePath: string,
  price: number,
  totalPrice: number,
  quantity: number,
  deleteLineItem: Function,
};

class LineItem extends Component {
  props: Props;

  render() {
    const { t } = this.props;
    const click = () => {
      this.props.deleteLineItem(this.props.sku);
    };
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
            <div styleName="quantity">
              <Autocomplete
                inputProps={{
                  type: 'number',
                }}
                getItemValue={item => item}
                items={quantity}
                onSelect={this.changeQuantity}
                selectedItem={this.props.quantity}
                sortItems={false}
              />
            </div>
          </div>
          <div styleName="price">
            <Currency value={this.props.totalPrice}/>
          </div>
        </div>
        <div styleName="controls">
          <a styleName="close-button" onClick={click}>
            <Icon name="fc-close" styleName="replace-icon" />
          </a>
        </div>
      </div>
    );
  }
};

export default localized(LineItem);
