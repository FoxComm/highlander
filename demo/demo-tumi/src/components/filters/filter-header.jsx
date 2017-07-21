// @flow

import React, { Element } from 'react';
import styles from './filter-header.css';

type Props = {
  children?: Element<*>|Array<Element<*>>,
  count: number,
  expanded: boolean,
  onClear: Function,
  onClick: Function,
};

const FilterHeader = (props: Props): Element<*> => {
  const { children, count, expanded, onClear, onClick } = props;
  const iconStyle = expanded ? 'icon-minus' : 'icon-plus';

  return (
    <div styleName="header">
      <div styleName={iconStyle} />
      <div styleName="title-block">
        <span>
          <a
            styleName="title"
            onClick={onClick}
          >
            {children}
            {count > 0 && (
              <span styleName="count">&nbsp;({count})</span>
            )}
          </a>
        </span>
        {count > 0 && (
          <span>
            <a
              styleName="clear"
              onClick={onClear}
            >Clear</a>
          </span>
        )}
      </div>
    </div>
  );
};

export default FilterHeader;
