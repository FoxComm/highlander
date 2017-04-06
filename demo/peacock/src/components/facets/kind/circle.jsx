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
    const { facet, value } = this.props;

    if (this.props.click) {
      this.props.click(facet, value, event.target.checked);
    }
    if (this.props.checked == null) {
      this.setState({checked: event.target.checked});
    }
  }

  render(): Element<*> {
    const {
      reactKey,
      label,
    } = this.props;

    return (
      <div styleName="circle-checkbox">
        <input
          id={reactKey}
          type="checkbox"
          checked={this.state.checked}
          onChange={this.click}
        />
        <div>
          <label htmlFor={reactKey}>
            {label}
          </label>
        </div>
      </div>
    );
  }
}

export default Circle;
