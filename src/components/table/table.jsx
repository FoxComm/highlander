'use strict';

import React, { PropTypes } from 'react';
import TableHead from './head';
import TableBody from './body';

const Table = (props) => {
  return (
    <table className='fc-table'>
      <TableBody data={props.data} renderRow={props.renderRow}/>
    </table>
  );
};

Table.propTypes = {
  data: PropTypes.object,
  renderRow: PropTypes.func
};

export default Table;
