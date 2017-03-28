/* @flow */

import React, { Component, Element } from 'react';
import styles from './checkbox.css';
import { autobind } from 'core-decorators';

import type { FacetElementProps } from 'types/facets';

type State = {
  checked: boolean,
};

class Checkbox extends Component {
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
      value,
      label,
    } = this.props;

    const id = `${facet}-checkbox-${value}`;

    return (
      <div styleName="facet-checkbox">
        <input
          styleName="facet-checkbox-input"
          id={id}
          type="checkbox"
          checked={this.state.checked}
          onChange={this.click}
        />
        <div styleName="facet-checkbox-box">
          <label htmlFor={id}>{'✔'}</label>
        </div>
        <label styleName="facet-checkbox-label" htmlFor={id}>{label} </label>
      </div>
    );
  }
}

export default Checkbox;
