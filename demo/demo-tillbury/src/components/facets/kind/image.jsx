/* @flow */

import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import classnames from 'classnames';
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
  click() {
    const selected = !this.state.checked;
    if (this.props.click) {
      this.props.click(this.props.facet, this.props.value.value, selected);
    }
    if (this.props.checked == null) {
      this.setState({checked: selected});
    }
  }

  render(): Element<*> {
    const {
      value,
    } = this.props;

    const className = classnames(styles['facet-image'], {
      _selected: this.state.checked,
    });

    return (
      <div className={className} onClick={this.click}>
        <div styleName="wrapper">
          <img src={value.image} styleName="image" />
        </div>
      </div>
    );
  }
}

export default Image;
