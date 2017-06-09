/* @flow */

// libs
import React from 'react';
import classNames from 'classnames';
import _ from 'lodash';

// components
import LoadingInputWrapper from 'components/forms/loading-input-wrapper';
import { INPUT_ATTRS } from 'paragons/common';
import TextInput from 'components/core/text-input';

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
      <i className={classNames(s['input-icon'], 'icon-search')} />
      <TextInput
        className={classNames('fc-input', s.input)}
        type="text"
        {...attrs}
      />
    </LoadingInputWrapper>
  );
};

export default TypeaheadInput;
