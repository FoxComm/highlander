// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import classNames from 'classnames';
import { connect } from 'react-redux';

// components
import TableView from './tableview';
import MultiSelectHead, { selectionState } from './multi-select-head';

// redux
import * as ExtandableTableActions from '../../modules/expandable-tables';

const mapStateToProps = (state, props) => ({
  tableState: _.get(state, ['expandableTables', props.entity.entityType, props.entity.entityId], {})
});

const ExpandableTable = props => {
  const renderRow = (row, index) => {
    const {renderRow, entity, columns, params, tableState, idField, toggleDrawerState} = props;
    const id = _.get(row, idField).toString().replace(/ /g,'-');
    const state = _.get(tableState, [id], false);

    const hackedParams = {
      ...params,
      toggleDrawerState: () => toggleDrawerState(entity, id),
      isOpen: state,
    };

    return renderRow(row, index, columns, hackedParams);
  };

  return (
    <TableView
      {...props}
      className={classNames('fc-expandable-table', props.className)}
      columns={props.columns}
      renderRow={renderRow} />
  );
};

ExpandableTable.propTypes = {
  columns: PropTypes.array.isRequired,
  data: PropTypes.shape({
    rows: PropTypes.array,
    total: PropTypes.number,
    from: PropTypes.number,
    size: PropTypes.number,
  }),
  renderRow: PropTypes.func,
  emptyMessage: PropTypes.string.isRequired,
  className: PropTypes.string,
  params: PropTypes.object,
  tableState: PropTypes.object,
  idField: PropTypes.string.isRequired,
};

export default connect(mapStateToProps, {...ExtandableTableActions})(ExpandableTable);
