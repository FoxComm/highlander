/* @flow */

import React from 'react';
import { findDOMNode } from 'react-dom';
import type { HTMLElement } from 'types';
import styles from './checkbox.css';
import _ from 'lodash';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import * as tracking from 'lib/analytics';

type Facet = {
  value: string,
  label: string,
  checked?: boolean,
  click: Function,
};

type State = {
  checked: boolean
};

class Checkbox extends React.Component {
  props: Facet;
  state: State = {
    checked: false
  };

  static defaultProps = {};

  @autobind
  click() {
    if(this.props.click) {
      this.props.click(this.props.value)
    }
    this.state.checked = !this.state.checked;
  }

  render(): HTMLElement {
    const {
      value,
      label,
      checked,
    } = this.props;

    const id = 'checkbox-' + value;

    return (<div styleName="facet-checkbox">
                <input
                  styleName="facet-checkbox-input"
                  id={id}
                  type="checkbox"
                  value={value}
                  defaultChecked={checked}
                  onChange={this.click()}
                />
                <div styleName="facet-checkbox-box">
                  <label htmlFor={id}>{"✔"}</label>
                </div>
              <label styleName="facet-checkbox-label" htmlFor={id}>{label} </label>
            </div>);
  }
}

export default Checkbox;
