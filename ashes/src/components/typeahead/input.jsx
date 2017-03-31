/* @flow */

// libs
import classNames from 'classnames';
import React from 'react';

// styles
import s from './typeahead.css';

type Props = {
  autoFocus: boolean,
  className: string,
};

const TypeaheadInput = ({ className, ...rest }: Props) => {
  return (
    <div className={className}>
      <i className={classNames(s['input-icon'], 'icon-search')} />
      <input
        className={classNames('fc-input', s.input)}
        type="text"
        {...rest}
      />
    </div>
  );
};

export default TypeaheadInput;
