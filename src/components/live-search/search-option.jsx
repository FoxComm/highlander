import React, { PropTypes } from 'react';
import MenuItem from '../menu/menu-item';

const SearchOption = props => {
  const {option, clickAction, ...rest} = props;
  let action = null;

  if (option.displayAction) {
    action = ` : ${option.displayAction}`;
  }

  return (
    <MenuItem onClick={() => props.clickAction(option.selectionValue)} {...rest}>
      <span className='fc-search-option-term'>{option.displayTerm}</span>
      <span className='fc-search-option-action'>{action}</span>
    </MenuItem>
  );
};

SearchOption.propTypes = {
  clickAction: PropTypes.func,
  option: PropTypes.object.isRequired
};

export default SearchOption;
