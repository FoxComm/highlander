import React, { PropTypes } from 'react';
import TableHead from './head';
import TableBody from './body';

const Table = (props) => {
  const {data, setState, renderRow, ...rest} = props;

  return (
    <table className='fc-table'>
      <TableHead {...rest} sortBy={data.sortBy} setState={setState}/>
      <TableBody {...rest} rows={data.rows} renderRow={renderRow}/>
    </table>
  );
};

Table.propTypes = {
  data: PropTypes.object.isRequired,
  renderRow: PropTypes.func,
  setState: PropTypes.func,
  predicate: PropTypes.func,
  processRows: PropTypes.func,
  detectNewRows: PropTypes.bool
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
