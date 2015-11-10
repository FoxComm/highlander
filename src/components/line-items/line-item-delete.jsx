import React, { PropTypes } from 'react';
import lineItemActions from '../../actions/line-items';
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
        <i className="icon-trash"></i>
      </button>
    );
  }
}

DeleteLineItem.propTypes = {
  model: PropTypes.object,
  entityName: PropTypes.string,
  entity: PropTypes.object
};
