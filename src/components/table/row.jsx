'use strict';

import React from 'react';

export default class TableRow extends React.Component {
  render() {
    return (
      <tr className="fc-table-tr">
        {this.props.children}
      </tr>
    );
  }
}

TableRow.propTypes = {
  children: React.PropTypes.any
};
