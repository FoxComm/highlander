/* @flow */

// libs
import React from 'react';
import classNames from 'classnames';
import _ from 'lodash';

// components
import LoadingInputWrapper from 'components/forms/loading-input-wrapper';

// styles
import s from './typeahead.css';

type Props = {
  autoFocus?: boolean,
  className?: string,
  isFetching?: boolean,
};

export const INPUT_ATTRS = [
  'name',
  'value',
  'placeholder',
  'disabled',
  'autoComplete',
  'onBlur',
  'onChange',
  'onFocus',
  'onKeyUp',
];

const TypeaheadInput = ({ className, isFetching = false, ...rest }: Props) => {
  const attrs = _.pick(rest, INPUT_ATTRS);

  return (
    <LoadingInputWrapper inProgress={isFetching}>
      <i className={classNames(s['input-icon'], 'icon-search')} />
      <input
        className={classNames('fc-input', s.input, className)}
        type="text"
        {...attrs}
      />
    </LoadingInputWrapper>
  );
};

export default TypeaheadInput;
