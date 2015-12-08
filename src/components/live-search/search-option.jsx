import React, { PropTypes } from 'react';
import MenuItem from '../menu/menu-item';

const SearchOption = props => {
  const {option, clickAction, ...rest} = props;
  const action = option.displayAction ? ` : ${option.displayAction}` : null;

  const click = (event) => {
    event.preventDefault();
    event.stopPropagation();
    clickAction(option.selectionValue);
  };

  const preventClickSelection = (event) => {
    event.preventDefault();
    event.stopPropagation();
  };

  return (
    <MenuItem onClick={click} onMouseDown={preventClickSelection} onMouseUp={preventClickSelection} {...rest}>
      <span className='fc-search-option__term'>{option.displayTerm}</span>
      <span className='fc-search-option__action'>{action}</span>
    </MenuItem>
  );
};


SearchOption.propTypes = {
  clickAction: PropTypes.func,
  option: PropTypes.object.isRequired
};

export default SearchOption;
