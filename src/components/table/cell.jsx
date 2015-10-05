'use strict';

import React from 'react';

export default class TableCell extends React.Component {
  render() {
    return (
      <td className="fc-table-td">
        {this.props.children}
      </td>
    )
  }
}

TableCell.propTypes = {
  children: React.PropTypes.any
};
