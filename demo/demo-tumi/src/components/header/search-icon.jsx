
// @flow

import React from 'react';
import styles from './search-icon.css';

type Props = {
  onClick: (e: SyntheticEvent) => void,
};

const SearchIcon = (props: Props) => {
  return (
    <div styleName="search-icon" onClick={props.onClick} />
  );
};

export default SearchIcon;
