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
  onConfirmDelete() {
    this.props.onDelete([{'sku': this.props.model.sku, 'quantity': 0}]);
  }

  onClick() {
    dispatch('toggleModal', <ConfirmModal details={confirmOptions} callback={this.onConfirmDelete.bind(this)} />);
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
