// @flow

import React, { Component, Element } from 'react';
import type { Filter, FilterGroupProps } from './types';

type Props = {
  children?: Array<Element<FilterGroupProps>>,
  filters: Array<Filter>,
  onClearFacet: Function,
  onSelectFacet: Function,
};

export default class Filters extends Component {
  props: Props;

  filterMap = () => {
    const filters = this.props.filters || [];
    return filters.reduce((acc, filter) => {
      const filterKey = filter.key.toUpperCase();
      return { ...acc, [filterKey]: filter.values };
    }, {});
  };

  visibleChildren = () => {
    const { children, onClearFacet, onSelectFacet } = this.props;
    const filterMap = this.filterMap();

    return React.Children.toArray(children).reduce((acc, child) => {
      const term = child.props.term.toUpperCase();
      const filterValues = filterMap[term];

      if (filterValues) {
        return [
          ...acc,
          React.cloneElement(child, {
            onClearFacet: () => onClearFacet(term),
            onSelectFacet,
            values: filterValues,
          }),
        ];
      }

      return acc;
    }, []);
  };

  render() {
    return (
      <div>
        {this.visibleChildren()}
      </div>
    );
  }
}
