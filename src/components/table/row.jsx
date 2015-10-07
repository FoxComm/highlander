'use strict';

import React from 'react';

export default class TableRow extends React.Component {
  static propTypes = {
    children: React.PropTypes.any
  };

  render() {
    return (
      <tr className="fc-table-tr">
        {this.props.children}
      </tr>
    );
  }
}
