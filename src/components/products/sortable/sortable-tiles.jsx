/* @flow weak */

// libs
import _ from 'lodash';
import classNames from 'classnames';
import { autobind } from 'core-decorators';
import React, { Component, Children } from 'react';
import { Motion, spring } from 'react-motion';

// styles
import styles from './sortable-tiles.css';

// types
type Coords = [number, number];

type MousePosition = { pageX: number, pageY: number };

type Props = {
  itemWidth: number;
  itemHeight: number;
  gutterX: number;
  gutterY: number;
  children: ?Array<Element>;
  itemStyles: ?{[key: string]: number};
}

type State = {
  order: Array<number>;
  layout: Array<Coords>;
  columns: number;
  gutter: number;
  mouse: Coords;
  delta: Coords;
  activeIndex: number;
  isPressed: boolean;
  isResizing: boolean;
}

/** Calculate item column/row index fitted to bounds */
const clamp = (n, min, max) => Math.max(Math.min(n, max), min);

// define spring motion opts
const springSetting1 = { stiffness: 180, damping: 10 };
const springSetting2 = { stiffness: 150, damping: 16 };

class SortableTiles extends Component {

  static props: Props;

  static defaultProps = {
    itemStyles: {},
    gutterX: 10,
    gutterY: 20,
  };

  state: State = {
    order: _.range(Children.count(this.props.children)),
    layout: Array(Children.count(this.props.children)).fill([0, 0]),
    columns: 3,
    gutter: this.props.gutterX,
    mouse: [0, 0],
    delta: [0, 0], // difference between mouse and item position, for dragging
    activeIndex: 0, // key of the last pressed component
    isPressed: false,
    isResizing: false,
  };

  container: HTMLElement; // Container element

  items: Array<HTMLElement> = []; // Array of items nodes

  resizeTimeout: ?number = null;

  componentDidMount(): void {
    window.addEventListener('touchmove', this.handleTouchMove);
    window.addEventListener('mousemove', this.handleMouseMove);
    window.addEventListener('touchend', this.handleMouseUp);
    window.addEventListener('mouseup', this.handleMouseUp);
    window.addEventListener('resize', this.handleResize);

    this.recalculateLayout();
    this.forceUpdate();
  }

  componentWillUnmount(): void {
    window.removeEventListener('touchmove', this.handleTouchMove);
    window.removeEventListener('mousemove', this.handleMouseMove);
    window.removeEventListener('touchend', this.handleMouseUp);
    window.removeEventListener('mouseup', this.handleMouseUp);
    window.removeEventListener('resize', this.handleResize);
  }

  @autobind
  handleTouchStart(from: number, pressLocation: Coords, { touches }: { touches: Array<MousePosition> }) {
    this.handleMouseDown(from, pressLocation, touches[0]);
  }

  @autobind
  handleTouchMove({ touches }: { touches: Array<MousePosition> }) {
    this.handleMouseMove(touches[0]);
  }

  recalculateLayout(prevOrder: ?Array<number> = null): void {
    const { itemWidth, itemHeight, gutterX, gutterY } = this.props;

    const order = prevOrder ? prevOrder : this.state.order;
    const containerWidth = this.container ? this.container.clientWidth : 0;

    /** calculate max columns count for given width of container */
    const columns = Math.floor((containerWidth + gutterX) / (itemWidth + gutterX));
    const rows = Math.ceil(order.length / columns);
    const gutter = rows > 1 ? (containerWidth - itemWidth * columns) / (columns - 1) : gutterX;

    const layout = order.map((_: any, index: number) => {
      const column = index % columns;
      const row = Math.floor(index / columns);
      const offsetX = column * gutter;
      const offsetY = row * gutterY;

      return [Math.round(itemWidth * column + offsetX), Math.round(itemHeight * row + offsetY)];
    });
    this.container.style.height = `${rows * (itemHeight + gutterY)}px`;

    this.setState({
      layout,
      columns,
      gutter,
    });
  }

  recalculateOrder(from: number, to: number): void {
    if (from === to) {
      return;
    }

    const order = [...this.state.order];
    const movedValue = order[from];
    order.splice(from, 1);
    order.splice(to, 0, movedValue);

    this.setState({ order });
  }

  @autobind
  handleMouseMove({ pageX, pageY }: MousePosition) {
    const { columns, gutter, activeIndex, isPressed, delta: [dx, dy] } = this.state;
    const { itemWidth, itemHeight } = this.props;

    if (isPressed) {
      const mouse = [pageX - dx, pageY - dy];

      const colTo = clamp(Math.floor((mouse[0] + ((itemWidth + gutter) / 2)) / (itemWidth + gutter)), 0, columns);
      const rowTo = clamp(Math.floor((mouse[1] + (itemHeight / 2)) / itemHeight), 0, 100);

      const to = clamp(colTo + rowTo * columns, 0, this.state.order.length - 1);

      this.recalculateOrder(activeIndex, to);

      this.setState({
        mouse,
        activeIndex: to,
      });
    }
  }

  @autobind
  handleMouseDown(from: number, [itemX, itemY]: Coords, { pageX, pageY }: MousePosition) {

    this.setState({
      activeIndex: from,
      isPressed: true,
      delta: [pageX - itemX, pageY - itemY],
      mouse: [itemX, itemY],
    });
  }

  @autobind
  handleMouseUp() {
    this.setState({
      isPressed: false,
      delta: [0, 0]
    });
  }

  @autobind
  handleResize() {
    clearTimeout(this.resizeTimeout);
    this.applyResizingState(true);

    this.container.style.height = `${this.container.scrollHeight}px`;
    // resize one last time after resizing stops, as sometimes this can be a little janky sometimes...
    this.resizeTimeout = setTimeout(() => this.applyResizingState(false), 100);
  }

  @autobind
  applyResizingState(isResizing: boolean) {
    this.recalculateLayout(this.state.order);

    this.setState({ isResizing });
  }

  render() {
    const { order, activeIndex, isPressed, mouse, isResizing, layout } = this.state;
    const children = Children.toArray(this.props.children);

    return (
      <div className={styles.items} ref={element => this.container = element}>
        {order.map((item, index) => {
          let style,
            x,
            y,
            isActive = (index === activeIndex && isPressed);

          if (isActive) {
            [x, y] = mouse;
            style = {
              translateX: x,
              translateY: y,
              scale: spring(1.1, springSetting1)
            };
          } else if (isResizing) {
            [x, y] = layout[index];
            style = {
              translateX: spring(x, springSetting2),
              translateY: spring(y, springSetting2),
              scale: 1
            };
          } else {
            [x, y] = layout[index];
            style = {
              translateX: spring(x, springSetting2),
              translateY: spring(y, springSetting2),
              scale: spring(1, springSetting1)
            };
          }

          return (
            <Motion key={item} style={style}>
              {({ translateX, translateY, scale }) => {

                const transitionStyles = {
                  WebkitTransform: `translate3d(${translateX}px, ${translateY}px, 0) scale(${scale})`,
                  transform: `translate3d(${translateX}px, ${translateY}px, 0) scale(${scale})`,
                  zIndex: (index === activeIndex) ? 99 : 1,
                };

                const itemStyles = {
                  ...transitionStyles,
                  ...this.props.itemStyles,
                };

                return (
                  <div
                    onMouseDown={this.handleMouseDown.bind(null, index, [x, y])}
                    onTouchStart={this.handleTouchStart.bind(null, index, [x, y])}
                    className={classNames(styles.item, { [styles.isActive]: isActive })}
                    style={itemStyles}
                    ref={(item => this.items.push(item))}
                  >
                    {children[item]}
                  </div>
                );
              }}
            </Motion>
          );
        })}
      </div>
    );
  }
}

export default SortableTiles;
