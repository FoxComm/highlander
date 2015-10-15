'use strict';

import React from 'react';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';
import * as actionCreators from '../../actions/order-line-items';

import { formatCurrency } from '../../lib/format';

function mapStateToProps(state) {
  return {
    lineItems: state.orderLineItems || {}
  };
}

function mapDispatchToProps(dispatch) {
  return { actions: bindActionCreators(actionCreators, dispatch) };
}

@connect(mapStateToProps, mapDispatchToProps)
export default class OrderLineItem extends React.Component {
  static propTypes = {
    item: React.PropTypes.object
  };

  constructor(props, context) {
    super(props, context);
  }

  render() {
    let item = this.props.item;

    return (
      <tr>
        <td><img src={item.imagePath} /></td>
        <td>{item.name}</td>
        <td>{item.sku}</td>
        <td>{formatCurrency(item.price)}</td>
        <td>
          <div className="fc-input-group fc-counter">
            <div className="fc-input-prepend">
              <button onClick={() => this.props.actions.orderLineItemDecrement(item.sku)}>
                <i className="icon-chevron-down"></i>
              </button>
            </div>
            <input
              type='number'
              name={`line-item-quantity-${this.props.item.sku}`}
              value={this.props.item.quantity}
              min={0}
              max={10000000}
              step={1} />
            <div className="fc-input-append">
              <button onClick={() => this.props.actions.orderLineItemIncrement(item.sku)}>
                <i className="icon-chevron-up"></i>
              </button>
            </div>
          </div>
        </td>
        <td>{formatCurrency(this.props.item.totalPrice)}</td>
        <td>
          <button className='fc-btn' onClick={() => this.props.actions.orderLineItemDelete(item.sku)}>
            <i className='icon-trash' />
          </button>
        </td>
      </tr>
    );
  }
}