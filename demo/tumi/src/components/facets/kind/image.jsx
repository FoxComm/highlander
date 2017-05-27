/* @flow */

import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import styles from './image.css';

import type { FacetElementProps } from 'types/facets';

type State = {
  checked: boolean,
};


class Image extends Component {
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

    const id = `${facet}-image-${label}`;

    return (
      <div styleName="image-checkbox">
        <input
          id={id}
          type="checkbox"
          checked={this.state.checked}
          onChange={this.click}
        />
        <div>
          <label htmlFor={id}>
            <img src={value.image} styleName="image" />
            <div styleName="sign-holder" />
          </label>
        </div>
      </div>
    );
  }
}

export default Image;
