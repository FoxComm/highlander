import React, { PropTypes } from 'react';
import _ from 'lodash';

import MultiSelectRow from '../table/multi-select-row';

const setCellContents = (group, field) => _.get(group, field);

const GroupRow = (props) => {
  const { group, columns, params } = props;
  const key = `group-${group.id}`;

  return (
    <MultiSelectRow
      columns={columns}
      linkTo="customer-group"
      linkParams={{ groupId: group.id }}
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
