/* @flow */

import React, { Component, Element } from 'react';
import styles from './circle.css';
import { autobind } from 'core-decorators';

import type { FacetElementProps } from 'types/facets';

type State = {
  checked: boolean,
};

class Circle extends Component {
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
      this.props.click(this.props.facet, this.props.value, event.target.checked);
    }
    if (this.props.checked == null) {
      this.setState({checked: event.target.checked});
    }
  }

  render(): Element<*> {
    const {
      facet,
      label,
    } = this.props;

    const id = `${facet}-check-${label}`;

    return (
      <div styleName="circle-checkbox">
        <input
          id={id}
          type="checkbox"
          checked={this.state.checked}
          onChange={this.click}
        />
        <div>
          <label htmlFor={id}>
            {label}
          </label>
        </div>
      </div>
    );
  }
}

export default Circle;
