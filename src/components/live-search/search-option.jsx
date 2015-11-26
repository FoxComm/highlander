import React, { PropTypes } from 'react';
import MenuItem from '../menu/menu-item';

const SearchOption = props => {
  const {option, clickAction, ...rest} = props;
  let term = null;
  let action = null;
  let clickTerm = null;

  if (option.action) {
    term = `${option.display} : ${option.action}`; 
    clickTerm = term;
  } else {
    term = option.display;
    action = ' : Search';
    clickTerm = `${option.display} : `;
  }

  return (
    <MenuItem onClick={() => props.clickAction(clickTerm)} {...rest}>
      <span className='fc-search-option-term'>{term}</span>
      <span className='fc-search-option-action'>{action}</span>
    </MenuItem>
  );
};

SearchOption.propTypes = {
  clickAction: PropTypes.func,
  option: PropTypes.object.isRequired
};

export default SearchOption;
