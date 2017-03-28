// libs
import React, { PropTypes } from 'react';
import classNames from 'classnames';
import _ from 'lodash';

// components
import LoadingInputWrapper from '../forms/loading-input-wrapper';
import { INPUT_ATTRS } from 'paragons/common';

// styles
import s from './typeahead.css';

const TypeaheadInput = props => {
  const { isFetching } = props;
  const attrs = _.pick(props, INPUT_ATTRS);

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
