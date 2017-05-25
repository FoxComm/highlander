// @flow

import React from 'react';
import PropTypes from 'prop-types';
import { MenuItem } from './menu';

import s from './live-search.css';

type Props = {
  clickAction: Function;
  option: {
    selectionValue: any;
  };
};

const SearchOption = (props: Props) => {
  const { option, clickAction, ...rest } = props;

  return (
    <MenuItem clickAction={() => clickAction(option.selectionValue)} {...rest}>
      <span className={s.term}>{option.displayTerm}</span>
      <span className={s.action}>{option.displayAction}</span>
    </MenuItem>
  );
};

export default SearchOption;
