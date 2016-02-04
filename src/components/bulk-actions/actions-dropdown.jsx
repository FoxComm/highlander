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

const ActionsDropdown = ({actions, allChecked, toggledIds, total}) => {
  const totalSelected = allChecked ? total - toggledIds.length : toggledIds.length;

  return (
    <div className="fc-table-actions">
      <Dropdown className="fc-table-actions__dropdown"
                placeholder="Actions"
                changeable={false}
                onChange={getActionsHandler(actions, allChecked, toggledIds)}>
        {actions.map(([title, handler]) => (
          <DropdownItem key={title} value={title}>{title}</DropdownItem>
        ))}
      </Dropdown>
      <span className="fc-table-actions__selected">
        {totalSelected} Selected
      </span>
    </div>
  )
};

ActionsDropdown.propTypes = {};

export default ActionsDropdown;
