// libs
import _ from 'lodash';
import React from 'react';
import PropTypes from 'prop-types';

// components
import { Dropdown } from '../dropdown';
import { TextDropdown } from 'components/core/dropdown';

// styles
import s from './actions-dropdown.css';

function getActionsHandler(actions, allChecked, toggledIds) {
  return (value) => {
    const handler = _.find(actions, ([label, handler]) => label === value)[1];
    handler(allChecked, toggledIds);
  };
}

const ActionsDropdown = ({actions, disabled, allChecked, toggledIds, total}) => {
  const totalSelected = allChecked ? total - toggledIds.length : toggledIds.length;
  const items = actions.map(([title]) => ({ value: title }));

  return (
    <div className={s.actions}>
      <TextDropdown
        className={s.dropdown}
        placeholder="Actions"
        disabled={disabled}
        onChange={getActionsHandler(actions, allChecked, toggledIds)}
        items={items}
        stateless
      />
      { totalSelected > 0 ? (
        <span>
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
