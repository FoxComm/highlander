import _ from 'lodash';
import React, { PropTypes } from 'react';
import classNames from 'classnames';

import TableHead from './head';
import TableBody from './body';
import WaitAnimation from '../common/wait-animation';

export function tableMessage(message, props) {
  return (
    <TableBody {...props}>
      <tr>
        <td colSpan={props.columns.length}>
          <div className="fc-content-box__empty-row">
            {message}
          </div>
        </td>
      </tr>
    </TableBody>
  );
}

export function renderBodyDefault(props, rows) {
  const showLoading = props.showLoadingOnMount && props.isLoading === null || props.isLoading;

  if (showLoading) {
    return tableMessage(<WaitAnimation />, props);
  } else if (props.failed && props.errorMessage) {
    return tableMessage(props.errorMessage, props);
  } else if (_.isEmpty(rows) && props.emptyMessage) {
    return tableMessage(props.emptyMessage, props);
  } else {
    return <TableBody {...props} rows={rows} />;
  }
}

const Table = props => {
  const {data, setState, className, renderBody, ...rest} = props;

  const body = renderBody(rest, data.rows, renderBodyDefault);

  return (
    <table className={classNames('fc-table', className)}>
      <TableHead {...rest} sortBy={data.sortBy} setState={setState}/>
      {body}
    </table>
  );
};

Table.propTypes = {
  data: PropTypes.object.isRequired,
  renderRow: PropTypes.func,
  renderBody: PropTypes.func,
  setState: PropTypes.func,
  predicate: PropTypes.func,
  processRows: PropTypes.func,
  detectNewRows: PropTypes.bool,
  isLoading: PropTypes.bool,
  failed: PropTypes.bool,
  emptyMessage: PropTypes.string,
  errorMessage: PropTypes.string,
  className: PropTypes.string,
  showLoadingOnMount: PropTypes.bool,
};

Table.defaultProps = {
  columns: [],
  data: {
    rows: [],
    from: 0,
    size: 0,
    total: 0
  },
  showLoadingOnMount: true,
  isLoading: false,
  failed: false,
  errorMessage: 'An error occurred. Try again later.',
  emptyMessage: 'No data found.',
  renderBody: renderBodyDefault,
};

export default Table;
