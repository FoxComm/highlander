/* @flow */

import React from 'react';
import { findDOMNode } from 'react-dom';
import type { HTMLElement } from 'types';
import styles from './circle.css';
import _ from 'lodash';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import * as tracking from 'lib/analytics';

type Facet = {
  facet: string,
  value: string,
  label: string,
  checked?: boolean,
  click: Function,
};

type State = {
  checked: boolean
};

class Circle extends React.Component {
  props: Facet;
  state: State = {
    checked: false
  };

  static defaultProps = {};

  @autobind
  click(event) {
    if(this.props.click) {
      this.props.click(this.props.facet, this.props.value, event.target.checked);
    }
    this.setState({checked: event.target.checked});
  }

  render(): HTMLElement {
    const {
      facet,
      value,
      label,
    } = this.props;

    let id = facet + '-check-' + label

    return (
             <div styleName="circle-checkbox">
              <input
                  id={id}
                  type="checkbox"
                  value={this.state.checked}
                  defaultChecked={this.state.checked}
                  onChange={this.click}
                />
              <div>
                <label htmlFor={id}>
                  {label}
                </label>
              </div>
           </div>);
  }
}

export default Circle;
