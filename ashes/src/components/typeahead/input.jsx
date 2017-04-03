/* @flow */

// libs
import React from 'react';
import classNames from 'classnames';
import _ from 'lodash';

// components
import LoadingInputWrapper from 'components/forms/loading-input-wrapper';
import { INPUT_ATTRS } from 'paragons/common';

// styles
import s from './typeahead.css';

type Props = {
  autoFocus: boolean,
  className: string,
};

const TypeaheadInput = ({ className, isFetching, ...rest }: Props) => {
  const attrs = _.pick(rest, INPUT_ATTRS);

  return (
    <LoadingInputWrapper inProgress={isFetching}>
      <i className={classNames(s['input-icon'], 'icon-search')} />
      <input
        className={classNames('fc-input', s.input)}
        type="text"
        {...attrs}
      />
    </LoadingInputWrapper>
  );
};

export default TypeaheadInput;
