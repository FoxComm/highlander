// @flow

// libs
import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import _ from 'lodash';

// styles
import styles from './segment-control.css';

export type Props = {
  id?: number,
  isActive?: boolean,
  isDisabled?: boolean,
  title: string,
  onClick: Function,
};

class SegmentControl extends Component {

  props: Props;

  static defaultProps = {
    isActive: false,
    isDisabled: false,
    onClick: _.noop,
  };

  @autobind
  onClickHandler() {
    const { onClick, isActive, isDisabled } = this.props;

    if (!_.isNull(onClick) && !isActive && !isDisabled) {
      return onClick(this.props);
    }

    return null;
  }

  render() {
    const { title, isDisabled, isActive } = this.props;

    const styleNameSuffix = isDisabled ? 'disabled' : isActive ? 'active' : 'inactive';
    const styleName = styles[`segment-control-container-${styleNameSuffix}`];

    return (
      <div
        className={styleName} onClick={this.onClickHandler}>
        <div styleName="segment-control-title">
          {title}
        </div>
      </div>
    );
  }
}

export default SegmentControl;
