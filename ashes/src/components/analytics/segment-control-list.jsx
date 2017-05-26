// @flow

// libs
import React, { Component } from 'react';
import _ from 'lodash';

// styles
import styles from './segment-control-list.css';

// components
import SegmentControl from './segment-control';
import type { Props as SegmentControlType } from './segment-control';

// types
type Props = {
  items: Array<SegmentControlType>,
  disabledItems: Array<SegmentControlType>,
  onSelect: Function,
  activeSegment: SegmentControlType,
  legend: string,
};

class SegmentControlList extends Component {

  props: Props;

  static defaultProps = {
    items: [],
    disabledItems: [],
    onSelect: _.noop,
    activeSegment: _.noop,
    legend: 'X axis',
  };

  get legend() {
    const { legend } = this.props;
    return <div styleName="segment-control-list-legend">{legend}</div>;
  }

  isDisabled(item: SegmentControlType): boolean {
    const { disabledItems } = this.props;
    return _.find(disabledItems, disabledItem => disabledItem.id === item.id);
  }

  get segments(): Array<HTMLElement> {
    const { items, activeSegment, onSelect } = this.props;

    const activeSegmentId = !_.isNil(activeSegment)
      ? activeSegment.id : false;

    return _.map(items, (item, index) => (
      <SegmentControl
        id={index}
        title={item.title}
        isActive={(activeSegmentId === index)}
        isDisabled={this.isDisabled(item)}
        onClick={onSelect}
        key={`segment-control-list-${index}`}
        />
    ));
  }

  render() {
    return (
      <div styleName="segment-control-list-container">
        {this.legend}
        {this.segments}
      </div>
    );
  }
}

export default SegmentControlList;
