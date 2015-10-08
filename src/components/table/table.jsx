'use strict';

import React from 'react';
import ClassNames from 'classnames';
import TableStore from '../../lib/table-store';
import TableHead from './head';
import TableBody from './body';

export default class Table extends React.Component {
  static propTypes = {
    children: React.PropTypes.any,
    store: React.PropTypes.instanceOf(TableStore),
    renderRow: React.PropTypes.func
  };

  render() {
    return (
      <table className='fc-table'>
        <TableHead store={this.props.store}/>
        <TableBody store={this.props.store} renderRow={this.props.renderRow}>
          {this.props.children}
        </TableBody>
      </table>
    );
  }
}
