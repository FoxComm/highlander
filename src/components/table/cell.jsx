'use strict';

import React from 'react';

export default class TableCell extends React.Component {
  static propTypes = {
    children: React.PropTypes.any,
    colspan: React.PropTypes.number
  };

  render() {
    return (
      <td className="fc-table-td" colSpan={this.props.colspan}>
        {this.props.children}
      </td>
    );
  }
}
