/* @flow */

import React from 'react';
import { findDOMNode } from 'react-dom';
import type { HTMLElement } from 'types';
import styles from './colorcircle.css';
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

class ColorCircle extends React.Component {
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
    } = this.props;

    let id = 'color-' + label
    let style = "_lib_components_facets_kind_colorcircle__" + value + " _lib_components_facets_kind_colorcircle__color-checkbox";

    return (
             <div className={style}>
              <input
                  id={id}
                  type="checkbox"
                  value={value}
                  defaultChecked={this.state.checked}
                  onChange={this.click()}
                />
              <div>
                <label htmlFor={id}>{"✔"}</label>
              </div>
           </div>);
  }
}

export default ColorCircle;
