import React, { PropTypes } from 'react';

import _ from 'lodash';
import { transitionTo } from 'browserHistory';

import MultiSelectRow from '../table/multi-select-row';

const setCellContents = (group, field) => _.get(group, field);

const GroupRow = (props) => {
  const { group, columns, params } = props;
  const key = `group-${group.id}`;
  const clickAction = () => {
    transitionTo('customer-group', { groupId: group.id });
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

export default GroupRow;
