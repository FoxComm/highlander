// @flow

import _ from 'lodash';
import { autobind } from 'core-decorators';
import React, { Component } from 'react';

import SelectBox from 'ui/selectbox/selectbox';

import type { Facet as TFacet, FacetValue as BaseFacetValue } from 'types/facets';

type FacetValue = BaseFacetValue & {
  value: string,
}

type Props = {
  facet: TFacet,
  onChange: (facet: string, value: string, selected: boolean) => void,
}

const NOT_SELECTED_VALUE = 'NOT_SELECTED_VALUE';

class FacetSelectBox extends Component {
  props: Props;
  lastSelectedValue: any = null;

  get facetItems(): Array<[string, string]> {
    const { facet } = this.props;

    const values = _.map(facet.values, (v: FacetValue, i: number) => {
      return [String(i), v.label];
    });

    return [
      [NOT_SELECTED_VALUE, `Select a ${facet.key.toLowerCase()}`],
      ...values,
    ];
  }

  get selectedValue(): string {
    const selectedIndex = _.findIndex(this.props.facet.values, (v: FacetValue) => v.selected);
    return String(selectedIndex);
  }

  @autobind
  handleChange(offset: string) {
    // we don't care about unselect previous value
    // because product-variants logic doesn't support multiple values selected for one kind
    let value = offset;
    let selected = true;

    if (value === NOT_SELECTED_VALUE) {
      value = this.lastSelectedValue;
      selected = false;
    } else {
      value = this.props.facet.values[Number(value)].value;
    }
    if (value !== null) {
      if (selected) this.lastSelectedValue = value;
      this.props.onChange(
        this.props.facet.key,
        value,
        selected
      );
    }
  }


  render() {
    return (
      <SelectBox
        value={this.selectedValue}
        items={this.facetItems}
        onChange={this.handleChange}
      />
    );
  }
}

export default FacetSelectBox;
