'use strict';

import React from 'react';
import { listenTo, stopListeningTo, dispatch } from '../../lib/dispatcher';

const changeMethodEvent = 'shippingMethodChange';

export default class ShippingMethodItem extends React.Component {

  render() {
    let model = this.props.model;

    let input = this.props.isEditing ? <input type="radio" defaultChecked={model.isActive} name="shipping-method-active" /> : null;

    return (
      <div>
        <label>
          {input}
          {model.name}
        </label>
      </div>
    );
  }
}

ShippingMethodItem.propTypes = {
  model: React.PropTypes.object
};
