'use strict';

import React from 'react';

export default class DeleteLineItem extends React.Component {
  onClick() {
    let success = confirm('Do you want to delete the item?');
    if (success) {
      this.props.onDelete([{'skuId': this.props.model.skuId, 'qty': 0}]);
    }
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
