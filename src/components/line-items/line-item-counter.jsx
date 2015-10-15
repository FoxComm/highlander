'use strict';

import React from 'react';
import Counter from '../forms/counter';
import ConfirmModal from '../modal/confirm';
import LineItemActions from '../../actions/line-items';

const confirmOptions = {
  header: 'Confirm',
  body: 'Are you sure you want to delete this item?',
  cancel: 'Cancel',
  proceed: 'Yes, Delete'
};

export default class LineItemCounter extends React.Component {
  static propTypes = {
    model: React.PropTypes.object,
    entityName: React.PropTypes.string,
    entity: React.PropTypes.object,
    stepUp: React.PropTypes.func,
    stepDown: React.PropTypes.func
  };

  render() {
    return (
      <Counter
        defaultValue='quantity'
        stepAmount={1}
        minValue={0}
        maxValue={1000000}
        model={this.props.model}
        stepUp={this.props.stepUp}
        stepDown={this.props.stepDown}
      />
    );
  }
}
