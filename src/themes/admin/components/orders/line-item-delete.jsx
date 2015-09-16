'use strict';

import React from 'react';
import ConfirmModal from '../modal/confirm';
import { dispatch, listenTo, stopListeningTo } from '../../lib/dispatcher';

const confirmOptions = {
  header: 'Confirm',
  body: 'Are you sure you want to delete the line item?',
  cancel: 'No, Don\'t Delete',
  proceed: 'Yes, Delete Line Item'
};

export default class DeleteLineItem extends React.Component {
  constructor(props) {
    super(props);
    if (this.props.model) {
      this.state = {
        confirmEvent: `confirm-order-line-item-delete-${this.props.model.sku}`
      };
    }
  }
  componentDidMount() {
    listenTo(this.state.confirmEvent, this, this.onConfirmDelete);
  }

  componentWillUnmount() {
    stopListeningTo(this.state.confirmEvent, this);
  }

  onConfirmDelete() {
    dispatch('toggleModal', null);
    this.props.onDelete([{'sku': this.props.model.sku, 'quantity': 0}]);
  }

  onClick() {
    dispatch('toggleModal', <ConfirmModal event={this.state.confirmEvent} details={confirmOptions} />);
  }

  render() {
    return (
      <button onClick={this.onClick.bind(this)} ><i className="fa fa-trash-o"></i></button>
    );
  }
}

DeleteLineItem.propTypes = {
  model: React.PropTypes.object,
  onDelete: React.PropTypes.func
};
