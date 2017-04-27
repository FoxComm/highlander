/* @flow */

import React, { Component, Element } from 'react';
import classNames from 'classnames';
import styles from './checkbox.css';
import { autobind } from 'core-decorators';

// components
import CheckboxBase from 'ui/checkbox/checkbox';

import type { FacetElementProps } from 'types/facets';

type State = {
  checked: boolean,
};

class Checkbox extends Component {
  props: FacetElementProps;
  state: State = {
    checked: !!this.props.checked,
  };

  static defaultProps = {
    available: true,
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
      available,
    } = this.props;

    const className = classNames(
      styles['facet-checkbox'],
      {
        [styles.disabled]: !available,
      }
    );

    return (
      <CheckboxBase
        className={className}
        id={reactKey}
        checked={this.state.checked}
        onChange={this.click}
      >
        {label}
      </CheckboxBase>
    );
  }
}

export default Checkbox;
