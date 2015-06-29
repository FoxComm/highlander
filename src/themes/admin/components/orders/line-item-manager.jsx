'use strict';

import React from 'react';
import { dispatch } from '../../lib/dispatcher';

export default class LineItemManager extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      quantity: props.model.quantity
    };
  }

  setItemQuantity(event) {
    clearInterval(this.timeout);
    let value = event.target.value;
    this.setState({
      quantity: value
    });
    this.timeout = setTimeout(() => {
      dispatch('updateLineItem', [{'skuId': this.props.model.skuId, 'quantity': value}]);
    }, 500);
  }

  itemChange(amount) {
    let value = this.state.quantity + amount;
    this.setState({
      quantity: value
    });
    dispatch('updateLineItem', [{'skuId': this.props.model.skuId, 'quantity': value}]);
  }

  render() {
    return (
      <div>
        <button onClick={() => { this.itemChange(-1); }} className='btn'><i className='icon-down-open'></i></button>
        <input type='number' onChange={this.setItemQuantity.bind(this)} value={this.state.quantity} />
        <button onClick={() => { this.itemChange(1); }} className='btn'><i className='icon-up-open'></i></button>
      </div>
    );
  }
}

LineItemManager.propTypes = {
  model: React.PropTypes.object
};
