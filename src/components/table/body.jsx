'use strict';

import React from 'react';
import TableStore from '../../lib/table-store';
import TableRow from './row';

export default class TableBody extends React.Component {
  render() {
    return (
      <div className="fc-table-tbody">
        {this.props.store.rows.map(this.props.renderRow)}
      </div>
    );
  }
}

TableBody.propTypes = {
  store: React.PropTypes.instanceOf(TableStore),
  renderRow: React.PropTypes.func
};
