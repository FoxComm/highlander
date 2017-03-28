/* @flow */

import classnames from 'classnames';
import React, { Component, Element } from 'react';
import styles from './colorcircle.css';
import _ from 'lodash';
import { autobind } from 'core-decorators';

import type { FacetElementProps } from 'types/facets';

type State = {
  checked: boolean,
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
    checked: !!this.props.checked,
  };

  componentWillReceiveProps(nextProps: FacetElementProps) {
    if (nextProps.checked != this.props.checked) {
      this.setState({
        checked: nextProps.checked,
      });
    }
  }

  @autobind
  click(event: SyntheticInputEvent) {
    if (this.props.click) {
      this.props.click(this.props.facet, this.props.value.value, event.target.checked);
    }
    if (this.props.checked == null) {
      this.setState({checked: event.target.checked});
    }
  }

  render(): Element<*> {
    const {
      facet,
      value,
      label,
    } = this.props;

    const id = `${facet}-color-${label}`;

    const className = classnames(
      styles[value.color],
      isLight(value.color) ? styles['color-checkbox-light'] : styles['color-checkbox']
    );

    return (
      <div className={className}>
        <input
          id={id}
          type="checkbox"
          checked={this.state.checked}
          onChange={this.click}
        />
        <div>
          <label htmlFor={id}>{''}</label>
        </div>
      </div>
    );
  }
}

export default ColorCircle;
