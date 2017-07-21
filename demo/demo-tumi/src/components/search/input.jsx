/* @flow */

// libs
import _ from 'lodash';
import React from 'react';
import { responsiveConnect } from 'css/responsive-connect';

// components
import LoadingInputWrapper from 'components/forms/loading-input-wrapper';

// styles
import styles from './search.css';

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

const SearchInput = ({ className, isFetching = false, isMedium, ...rest }: Props) => {
  const attrs = _.pick(rest, INPUT_ATTRS);

  return (
    <LoadingInputWrapper inProgress={isFetching} animationSize={isMedium ? 'm' : 26}>
      <button type="submit" styleName="m-search-button">
        <span styleName="m-search-icon" />
      </button>
      <input
        className={className}
        type="text"
        name="text"
        {...attrs}
      />
    </LoadingInputWrapper>
  );
};

export default responsiveConnect(['isMedium'])(SearchInput);
