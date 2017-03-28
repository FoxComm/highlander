/* @flow */

import React, { Component, Element } from 'react';
import styles from './colorcircle.css';
import _ from 'lodash';
import { autobind } from 'core-decorators';

import type { FacetElementProps } from 'types/facets';

type State = {
  checked: boolean
};

const lightColors = [
  'white',
  'yellow',
  'light',
];

function isLight(c) {
  return _.some(lightColors, (lc) => {
    return c.includes(lc);
  });
}

class ColorCircle extends Component {
  props: FacetElementProps;
  state: State = {
    checked: false,
  };

  @autobind
  click(event: SyntheticInputEvent) {
    if (this.props.click) {
      this.props.click(this.props.facet, this.props.value.value, event.target.checked);
    }
    this.setState({checked: event.target.checked});
  }

  render(): Element<*> {
    const {
      facet,
      value,
      label,
    } = this.props;

    const id = `${facet}-color-${label}`;

    // @TODO: get rid of this weird approach
    const colorStyle = `_lib_components_facets_kind_colorcircle__${value.color}`;
    const checkboxStyle = isLight(value.color) ?
      ' _lib_components_facets_kind_colorcircle__color-checkbox-light' :
      ' _lib_components_facets_kind_colorcircle__color-checkbox';

    const style = colorStyle + checkboxStyle;

    return (
      <div className={style}>
        <input
          id={id}
          type="checkbox"
          value={this.state.checked}
          defaultChecked={this.state.checked}
          onChange={this.click}
        />
        <div>
          <label htmlFor={id}>{'✔'}</label>
        </div>
      </div>
    );
  }
}

export default ColorCircle;
