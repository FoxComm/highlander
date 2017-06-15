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
  isFetching?: boolean,
};

const TypeaheadInput = ({ className, isFetching = false, ...rest }: Props) => {
  const attrs = _.pick(rest, INPUT_ATTRS);

  return (
    <LoadingInputWrapper className={className} inProgress={isFetching}>
      <input
        className={classNames('fc-input', s.input)}
        type="text"
        {...attrs}
      />
    </LoadingInputWrapper>
  );
};

export default TypeaheadInput;
