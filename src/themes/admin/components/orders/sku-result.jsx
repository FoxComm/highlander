'use strict';

import React from 'react';
import { formatCurrency } from '../../lib/format';

export default class SkuResult extends React.Component {
  render() {
    let model = this.props.model;
    return (
      <div>
        <span><img src={model.image} /></span>
        <span>{model.name}</span>
        <span>{model.skuId}</span>
        <span>{formatCurrency(model.price)}</span>
      </div>
    );
  }
}

SkuResult.propTypes = {
  model: React.PropTypes.object
};
