'use strict';

import lineItemActions from '../../actions/line-items';
import React from 'react';
import ConfirmModal from '../modal/confirm';
import { dispatch, listenTo, stopListeningTo } from '../../lib/dispatcher';

const confirmOptions = {
  header: 'Confirm',
  body: 'Are you sure you want to delete this item?',
  cancel: 'Cancel',
  proceed: 'Yes, Delete'
};

export default class DeleteLineItem extends React.Component {
  onConfirmDelete() {
    lineItemActions.editLineItems(
      this.props.entityName,
      this.props.entity.referenceNumber,
      [{'sku': this.props.model.sku, 'quantity': 0 }]
    );
  }

  onClick() {
    dispatch('toggleModal', <ConfirmModal details={confirmOptions} callback={this.onConfirmDelete.bind(this)} />);
  }

  render() {
    return (
      <button
        onClick={this.onClick.bind(this)}
        className="fc-btn fc-btn-remove">
        <i className="fa fa-trash-o"></i>
      </button>
    );
  }
}

DeleteLineItem.propTypes = {
  model: React.PropTypes.object,
  entityName: React.PropTypes.string,
  entity: React.PropTypes.object
};
