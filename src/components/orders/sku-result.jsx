'use strict';

import React, { PropTypes } from 'react';
import { formatCurrency } from '../../lib/format';

export default class SkuResult extends React.Component {
  render() {
    let model = this.props.model;
    return (
      <div className="fc-grid">
        <div className="fc-col-md-2-12"><img src={model.imagePath} /></div>
        <div className="fc-col-md-4-12">{model.name}</div>
        <div className="fc-col-md-3-12"><strong>Sku</strong><br />{model.sku}</div>
        <div className="fc-col-md-3-12"><strong>Price</strong><br />{formatCurrency(model.price)}</div>
      </div>
    );
  }
}

SkuResult.propTypes = {
  model: PropTypes.object
};
