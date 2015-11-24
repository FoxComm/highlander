import React, { PropTypes } from 'react';
import MenuItem from '../menu/menu-item';

const SearchOption = props => {
  const {option, ...rest} = props;
  const action = option.action || 'Search';

  return (
    <MenuItem {...rest}>
      <span className='fc-search-option-term'>{option.display}</span>
      <span className='fc-search-option-action'> : {action}</span>
    </MenuItem>
  );
};

SearchOption.propTypes = {
  option: PropTypes.object.isRequired
};

export default SearchOption;
