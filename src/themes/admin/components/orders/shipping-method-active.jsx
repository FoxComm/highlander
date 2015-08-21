'use strict';

import React from 'react';

export default class ShippingMethodActive extends React.Component {
  render() {
    return (
      <input type="radio" checked={this.props.model.isActive} name="shipping-method-active" readOnly />
    );
  }
}

ShippingMethodActive.propTypes = {
  model: React.PropTypes.object
};
