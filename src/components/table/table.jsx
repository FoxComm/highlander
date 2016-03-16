import React, { PropTypes } from 'react';
import TableHead from './head';
import TableBody from './body';

import classNames from 'classnames';

const Table = props => {
  const {data, setState, renderRow, className, ...rest} = props;

  return (
    <table className={classNames('fc-table', className)}>
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
  detectNewRows: PropTypes.bool,
  isLoading: PropTypes.bool,
  failed: PropTypes.bool,
  emptyMessage: PropTypes.string,
  errorMessage: PropTypes.string,
  className: PropTypes.string,
};

Table.defaultProps = {
  columns: [],
  data: {
    rows: [],
    from: 0,
    size: 0,
    total: 0
  },
  errorMessage: 'An error occurred. Try again later.',
  emptyMessage: 'No data found.',
};

export default Table;
