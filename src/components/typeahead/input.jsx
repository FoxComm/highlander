
import React, { PropTypes } from 'react';

const TypeaheadInput = props => {
  return (
    <div>
      <i className="fc-typeahead__input-icon icon-search"></i>
      <input
        className="fc-input fc-typeahead__input"
        type="text"
        {...props}
      />
    </div>
  );
};

export default TypeaheadInput;
