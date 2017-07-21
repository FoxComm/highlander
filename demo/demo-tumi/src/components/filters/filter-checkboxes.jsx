// @flow

import React, { Element } from 'react';
import type { FilterValue, FilterTypeProps } from './types';
import styles from './filter-checkboxes.css';

const FilterCheckboxes = (props: FilterTypeProps): Element<*> => {
  const term = (props.term || '').toUpperCase();
  const {
    onSelectFacet = (a, b, c) => {},
    values = [],
  } = props;

  const controls = values.reduce((acc, facetValue) => {
    const { count, label, selected, value } = facetValue;
    if (count == 0) {
      return acc;
    }
    
    const onSelect = () => onSelectFacet(term, value, !selected);

    const checkboxControl = (
      <div styleName="filter-value" key={label}>
        <label>
          <input
            styleName="filter-checkbox"
            type="checkbox"
            name={label}
            value={selected}
            checked={selected}
            onChange={onSelect}
          />
          <div styleName="filter-label">
            {label}
            <span styleName="count">&nbsp;({count})</span>
          </div>
        </label>
      </div>
    );

    return [...acc, checkboxControl];
  }, []);

  return (
    <form styleName="filter-block">
      {controls}
    </form>
  );
};

export default FilterCheckboxes;
