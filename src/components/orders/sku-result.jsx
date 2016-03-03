import React, { PropTypes } from 'react';
import Currency from '../common/currency';

export default class SkuResult extends React.Component {
  render() {
    let model = this.props.model;
    const imagePath = 'http://lorempixel.com/75/75/fashion/'; //TODO use model.imagePath when it is available
    return (
      <div className="fc-grid">
        <div className="fc-col-md-2-12"><img src={imagePath} /></div>
        <div className="fc-col-md-4-12">{model.title}</div>
        <div className="fc-col-md-3-12"><strong>SKU</strong><br />{model.code}</div>
        <div className="fc-col-md-3-12"><strong>Price</strong><br /><Currency value={model.price}/></div>
      </div>
    );
  }
}

SkuResult.propTypes = {
  model: PropTypes.object
};
