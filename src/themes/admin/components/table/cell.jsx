'use strict';

import React from 'react';

export default class TableCell extends React.Component {
  render() {
    return (
      <div className="fc-table-td">
        {this.props.children}
      </div>
    )
  }
}

TableCell.propTypes = {
  children: React.PropTypes.any
};
