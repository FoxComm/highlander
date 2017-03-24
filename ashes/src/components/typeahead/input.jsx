//  libs
import React, { PropTypes } from 'react';
import classNames from 'classnames';

// styles
import s from './typeahead.css';

const TypeaheadInput = props => {
  return (
    <div>
      <i className={classNames(s['input-icon'], 'icon-search')} />
      <input
        className="fc-input fc-typeahead__input"
        type="text"
        {...props}
      />
    </div>
  );
};

export default TypeaheadInput;
