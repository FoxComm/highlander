'use strict';

import React from 'react';
import TableStore from '../../lib/table-store';
import TableRow from './row';

export default class TableBody extends React.Component {
  render() {
    return (
      <tbody className="fc-table-tbody">
        {this.props.store.rows.map((row) => {
          return this.props.renderRow(row).props.children;
        })}
      </tbody>
    );
  }
}

TableBody.propTypes = {
  store: React.PropTypes.instanceOf(TableStore),
  renderRow: React.PropTypes.func
};
