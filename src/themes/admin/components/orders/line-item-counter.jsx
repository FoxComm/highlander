'use strict';

import React from 'react';
import Counter from '../forms/counter';
import Api from '../../lib/api';
import { dispatch } from '../../lib/dispatcher';

export default class LineItemCounter extends React.Component {
  onChange(newValue) {
    this.props.onChange([{'skuId': this.props.model.skuId, 'quantity': +newValue}]);
  }

  render() {
    return (
      <Counter defaultValue={this.props.model.quantity} stepAmount={1} minValue={0} maxValue={1000000} onChange={this.onChange.bind(this)} />
    );
  }
}

LineItemCounter.propTypes = {
  model: React.PropTypes.object,
  onChange: React.PropTypes.func
};
