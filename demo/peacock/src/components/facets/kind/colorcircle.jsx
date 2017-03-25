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
  facet: string,
  value: Object,
  label: string,
  checked?: boolean,
  click: Function,
};

type State = {
  checked: boolean
};

const lightColors = [
  'white',
  'yellow',
  'light'
];

function isLight(c) {
  return _.some(lightColors, (lc) => {
    return c.includes(lc);
  });
}

class ColorCircle extends React.Component {
  props: Facet;
  state: State = {
    checked: false
  };

  static defaultProps = {};

  @autobind
  click(event) {
    if(this.props.click) {
      this.props.click(this.props.facet, this.props.value.value, event.target.checked);
    }
    this.setState({checked : event.target.checked});
  }

  render(): HTMLElement {
    const {
      facet,
      value,
      label,
    } = this.props;
    console.log('CORLOR');
    console.log(value);

    let id = facet + '-color-' + label

    let colorStyle = "_lib_components_facets_kind_colorcircle__" + value.color;
    let checkboxStyle = isLight(value.color) ? 
      " _lib_components_facets_kind_colorcircle__color-checkbox-light": 
      " _lib_components_facets_kind_colorcircle__color-checkbox";

    let style = colorStyle + checkboxStyle;

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
                <label htmlFor={id}>{"✔"}</label>
              </div>
           </div>);
  }
}

export default ColorCircle;
