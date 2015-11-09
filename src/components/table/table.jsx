'use strict';

import React, { PropTypes } from 'react';
import TableHead from './head';
import TableBody from './body';

const Table = (props) => {
  return (
    <table className='fc-table'>
      <TableHead columns={props.columns} sortBy={props.data.sortBy} setState={props.setState}/>
      <TableBody columns={props.columns} rows={props.data.rows} renderRow={props.renderRow}/>
    </table>
  );
};

Table.propTypes = {
  data: PropTypes.object.isRequired,
  renderRow: PropTypes.func
};

Table.defaultProps = {
  columns: [],
  data: {
    rows: [],
    from: 0,
    size: 0,
    total: 0
  }
};

export default Table;
