'use strict';

import React from 'react';
import Counter from '../forms/counter';
import Api from '../../lib/api';
import { dispatch } from '../../lib/dispatcher';
import ConfirmModal from '../modal/confirm';
import LineItemActions from '../../actions/line-items';

const confirmOptions = {
  header: 'Confirm',
  body: 'Are you sure you want to delete this item?',
  cancel: 'Cancel',
  proceed: 'Yes, Delete'
};

export default class LineItemCounter extends React.Component {
  onConfirmDelete() {
    this.callback(true);
    this.callback = undefined;
  }

  onChange(oldValue, newValue) {
    LineItemActions.editLineItems(
      this.props.entityName,
      this.props.entity.referenceNumber,
      [{'sku': this.props.model.sku, 'quantity': +newValue}]
    );
  }

  onBeforeChange(oldValue, newValue, callback) {
    if (+newValue === 0) {
      dispatch('toggleModal', <ConfirmModal details={confirmOptions} callback={this.onConfirmDelete.bind(this)} />);
      this.callback = callback;
    } else {
      callback(true);
    }
  }

  render() {
    return (
      <Counter
        defaultValue='quantity'
        stepAmount={1}
        minValue={0}
        maxValue={1000000}
        onBeforeChange={this.onBeforeChange.bind(this)}
        onChange={this.onChange.bind(this)}
        model={this.props.model}
      />
    );
  }
}

LineItemCounter.propTypes = {
  model: React.PropTypes.object,
  entityName: React.PropTypes.string,
  entity: React.PropTypes.object
};
