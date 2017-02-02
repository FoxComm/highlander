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
  title: string,
  onClick: Function,
};

class SegmentControl extends Component {

  props: Props;

  static defaultProps = {
    isActive: false,
    onClick: _.noop,
  };

  @autobind
  onClickHandler() {
    const { onClick, isActive } = this.props;

    if (!_.isNull(onClick) && !isActive) {
      return onClick(this.props);
    }

    return null;
  }

  render() {
    const { title, isActive } = this.props;

    const isActiveStyle = isActive ? 'active' : 'inactive';

    return (
      <div 
        styleName={`segment-control-container-${isActiveStyle}`} onClick={this.onClickHandler}>
        <div styleName="segment-control-title">
          {title}
        </div>
      </div>
    );
  }
}

export default SegmentControl;
