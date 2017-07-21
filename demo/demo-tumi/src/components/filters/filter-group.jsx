// @flow

import React, { Component } from 'react';
import FilterHeader from './filter-header';
import FilterCheckboxes from './filter-checkboxes';

import styles from './filter-group.css';
import type { Filter, FilterValue, FilterGroupProps } from './types';

type State = {
  expanded: boolean,
};

export default class FilterGroup extends Component {
  props: FilterGroupProps;
  state: State = { expanded: false };

  selectedCount = () => {
    const { values = [] } = this.props;
    return values.reduce((acc, val) => {
      const increment = val.selected ? 1 : 0;
      return acc + increment;
    }, 0);
  };

  toggleExpansion = () => this.setState({ expanded: !this.state.expanded });

  render() {
    const { children, label, term, values } = this.props;
    const {
      onClearFacet = () => {},
      onSelectFacet = () => {},
    } = this.props;
    const { expanded } = this.state;

    const updatedChildren = React.Children.map(children, (child) => {
      return React.cloneElement(child, { onSelectFacet, term, values });
    });

    return (
      <div styleName="group">
        <FilterHeader
          count={this.selectedCount()}
          expanded={expanded}
          onClear={onClearFacet}
          onClick={this.toggleExpansion}
        >
          {label}
        </FilterHeader>
        {expanded && updatedChildren}
      </div>
    );
  }
}
