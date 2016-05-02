import React, { Component, PropTypes } from 'react';
import { findDOMNode } from 'react-dom';
import { DragSource, DropTarget } from 'react-dnd';

// components
import { Checkbox } from '../checkbox/checkbox';

// styles
import styles from './column-selector.css';

const itemSource = {
  beginDrag(props) {
    return {
      id: props.id,
      index: props.index
    };
  },

  endDrag(props) {
    props.dropItem();
  }
};

const itemTarget = {
  hover(props, monitor, component) {
    const dragIndex = monitor.getItem().index;
    const hoverIndex = props.index;

    // Don't replace items with themselves
    if (dragIndex === hoverIndex) return;

    // Determine rectangle on screen
    const hoverBoundingRect = findDOMNode(component).getBoundingClientRect();

    // Get vertical middle
    const hoverMiddleY = (hoverBoundingRect.bottom - hoverBoundingRect.top) / 2;

    // Determine mouse position
    const clientOffset = monitor.getClientOffset();

    // Get pixels to the top
    const hoverClientY = clientOffset.y - hoverBoundingRect.top;

    // Only perform the move when the mouse has crossed half of the items height
    // When dragging downwards, only move when the cursor is below 50%
    // When dragging upwards, only move when the cursor is above 50%

    // Dragging downwards
    if (dragIndex < hoverIndex && hoverClientY < hoverMiddleY) {
      return;
    }

    // Dragging upwards
    if (dragIndex > hoverIndex && hoverClientY > hoverMiddleY) {
      return;
    }

    // Time to actually perform the action
    props.moveItem(dragIndex, hoverIndex);

    // Note: we're mutating the monitor item here!
    // Generally it's better to avoid mutations,
    // but it's good here for the sake of performance
    // to avoid expensive index searches.
    monitor.getItem().index = hoverIndex;
  }
};

type Props = {
  connectDragSource: Function,
  connectDragPreview: Function,
  connectDropTarget: Function,
  index: number,
  isDragging: boolean,
  id: any,
  text: string,
  moveItem: Function,
  dropItem: Function,
}

@DropTarget('item', itemTarget, connect => ({
  connectDropTarget: connect.dropTarget()
}))
@DragSource('item', itemSource, (connect, monitor) => ({
  connectDragSource: connect.dragSource(),
  connectDragPreview: connect.dragPreview(),
  isDragging: monitor.isDragging()
}))
export default class Card extends Component {
  props: Props;

  render() {
    const { text, isDragging, connectDragSource, connectDropTarget, connectDragPreview } = this.props;
    const styleName = isDragging ? 'isDragging' : '';

    return connectDragPreview(connectDropTarget(
      <li styleName={styleName}>
        {connectDragSource(
          <i className='fc-tab__icon icon-drag-drop' />
        )}
        <Checkbox
          id={`choose-column-${this.props.id}`}
          onChange={this.props.onChange}
          checked={this.props.checked}>
          {text}
        </Checkbox>
      </li>
    ));
  }
}