// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';

// components
import {Dropdown, DropdownItem} from '../dropdown';


function getActionsHandler(actions, allChecked, toggledIds) {
  return (value) => {
    const handler = _.find(actions, ([label, handler]) => label === value)[1];
    handler(allChecked, toggledIds);
  };
}

const ActionsDropdown = ({actions, disabled, allChecked, toggledIds, total}) => {
  const totalSelected = allChecked ? total - toggledIds.length : toggledIds.length;

  return (
    <div className="fc-table-actions">
      <Dropdown className="fc-table-actions__dropdown"
                placeholder="Actions"
                changeable={false}
                disabled={disabled}
                onChange={getActionsHandler(actions, allChecked, toggledIds)}>
        {actions.map(([title, handler]) => (
          <DropdownItem key={title} value={title}>{title}</DropdownItem>
        ))}
      </Dropdown>
      { totalSelected > 0 ? (
        <span className="fc-table-actions__selected">
          {totalSelected} Selected
        </span>
      ) : null}
    </div>
  );
};

ActionsDropdown.propTypes = {
  actions: PropTypes.arrayOf(PropTypes.array),
  disabled: PropTypes.bool,
  allChecked: PropTypes.bool,
  toggledIds: PropTypes.array,
  total: PropTypes.number,
};

ActionsDropdown.defaultProps = {
  actions: [],
  disabled: false,
  allChecked: false,
  toggledIds: [],
  total: 0,
};

export default ActionsDropdown;
