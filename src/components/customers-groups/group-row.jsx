import React, { PropTypes } from 'react';

import _ from 'lodash';
import { transitionTo } from '../../route-helpers';

import MultiSelectRow from '../table/multi-select-row';

const setCellContents = (group, field) => _.get(group, field);

const GroupRow = (props, context) => {
  const { group, columns, params } = props;
  const key = `group-${group.id}`;
  const clickAction = () => {
    transitionTo(context.history, 'customer-group', { groupId: group.id });
  };

  return (
    <MultiSelectRow
      cellKeyPrefix={key}
      columns={columns}
      onClick={clickAction}
      row={group}
      setCellContents={setCellContents}
      params={params} />
  );
};

GroupRow.propTypes = {
  group: PropTypes.object.isRequired,
  columns: PropTypes.array,
  params: PropTypes.object.isRequired,
};

GroupRow.contextTypes = {
  history: PropTypes.object.isRequired
};

export default GroupRow;
