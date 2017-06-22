/* @flow */

// libs
import _ from 'lodash';
import React, { Element, Component } from 'react';
import { autobind } from 'core-decorators';
import classNames from 'classnames';

import Icon from 'components/core/icon';
import BodyPortal from 'components/body-portal/body-portal';

// styles
import s from './smart-list.css';

type Props = {
  /** Array of elements inside the list */
  children: Array<any>,
  /** Element which prepends the list */
  before?: Element<any> | string,
  /** Element which goes after the list */
  after?: Element<any> | string,
  align?: 'left' | 'right', // @todo
  /** Base element: list will try to stick around it. E.g. dropdown current value box.
   * Default value: previous sibling or parent. */
  pivot?: HTMLElement,
  /** If true, BodyPortal is used */
  detached: boolean,
  /** Additional className for block */
  className?: string,
  /** Callback on user actions which intends to close menu */
  onEsc: Function,
};

type State = {
  pointedValueIndex: number, // current hovered item, used for keyboard navigation
};

// looper for awwor keys navigation
function getNewItemIndex(itemsCount, currentIndex, increment = 1) {
  const startIndex = increment > 0 ? -1 : 0;
  const index = Math.max(currentIndex, startIndex);

  return (itemsCount + index + increment) % itemsCount;
}

/**
 * Smart List from dropdowns.
 * This component knows how to position itself around pivot element and fit to the screen.
 * Also component knows how to handle scroll and keyboard events.
 */
export default class SmartList extends Component {
  props: Props;

  static defaultProps = {
    align: 'right',
    onEsc: () => {},
    detached: false,
  };

  state: State = {
    pointedValueIndex: -1,
  };

  _items: HTMLElement;
  _block: HTMLElement;

  componentDidMount() {
    window.addEventListener('keydown', this.handleKeyPress, true);
    window.addEventListener('click', this.handleClickOutside, true);
    this.setPosition();
  }

  componentWillUnmount() {
    window.removeEventListener('keydown', this.handleKeyPress, true);
    window.removeEventListener('click', this.handleClickOutside, true);
  }

  componentDidUpdate(prevProps: Props, prevState: State) {
    this.setPosition();
  }

  @autobind
  handleClickOutside({ target }: { target: HTMLElement }) {
    const { onEsc } = this.props;
    const pivot = this.pivot;

    if (this._block && !this._block.contains(target) && pivot && !pivot.contains(target)) {
      onEsc();
    }
  }

  get pivot(): any {
    return this.props.pivot || (this._block && (this._block.previousElementSibling || this._block.parentElement));
  }

  setDetachedCoords() {
    const { detached } = this.props;
    const pivot = this.pivot;

    if (!detached) {
      return;
    }
    // `detached=true` – rare case, but we need to handle it

    if (!pivot || !this._block) {
      return;
    }

    const pivotDim = pivot.getBoundingClientRect();

    this._block.style.minWidth = `${pivot.offsetWidth}px`;
    this._block.style.top = `${pivotDim.top + pivotDim.height + window.scrollY}px`;
    this._block.style.left = `${pivotDim.left}px`;
  }

  setPosition() {
    const pivot = this.pivot;

    this.setDetachedCoords();

    if (!pivot || !this._block) {
      return;
    }

    const viewportHeight = window.innerHeight;

    const pivotDim = pivot.getBoundingClientRect();
    const spaceAtTop = pivotDim.top;
    const spaceAtBottom = viewportHeight - pivotDim.bottom;
    const listRect = this._block.getBoundingClientRect();

    if (spaceAtBottom < listRect.height && spaceAtBottom < spaceAtTop) {
      this._block.style.transform = `translateY(calc(-100% - ${pivotDim.height}px))`;
    } else {
      this._block.style.transform = '';
    }
  }

  scrollViewport(movingUp: boolean = false) {
    const newIndex = this.state.pointedValueIndex;
    const item = this._items.children[newIndex];

    const containerTop = this._items.scrollTop;
    const containerVisibleHeight = this._items.clientHeight;
    const itemTop = item.offsetTop;
    const itemHeight = item.offsetHeight;

    // shift height when compare to viewport top position - item height if moving up, zero otherwise
    const heightShift = movingUp ? itemHeight : 0;

    const elementBelowViewport = containerTop + containerVisibleHeight <= itemTop + itemHeight;
    const elementAboveViewport = containerTop > itemTop + heightShift;

    if (elementBelowViewport) {
      this._items.scrollTop = itemTop + itemHeight - containerVisibleHeight;
    }
    if (elementAboveViewport) {
      this._items.scrollTop = itemTop;
    }
  }

  @autobind
  handleKeyPress(e: KeyboardEvent) {
    const { pointedValueIndex } = this.state;
    const itemsCount = React.Children.count(this.props.children);

    switch (e.keyCode) {
      // enter
      case 13:
        e.stopPropagation();
        e.preventDefault();

        if (pointedValueIndex > -1) {
          this._items.children[pointedValueIndex].click();
        }

        break;
      // esc
      case 27:
        this.props.onEsc();

        break;
      // up
      case 38:
        e.preventDefault();

        this.setState(
          {
            pointedValueIndex: getNewItemIndex(itemsCount, pointedValueIndex, -1),
          },
          () => this.scrollViewport(true)
        );

        break;
      // down
      case 40:
        e.preventDefault();

        this.setState(
          {
            pointedValueIndex: getNewItemIndex(itemsCount, pointedValueIndex),
          },
          () => this.scrollViewport(false)
        );

        break;
    }
  }

  renderItems() {
    const { children } = this.props;

    // @todo only one child
    return React.Children.map(children, (item, index) => {
      const props: any = {
        className: classNames(item.props.className, s.item, { [s.active]: index === this.state.pointedValueIndex }),
      };

      return React.cloneElement(item, props);
    });
  }

  render() {
    const { before, after, className, detached } = this.props;
    const cls = classNames(s.block, className);

    return (
      <BodyPortal active={detached} className={cls} getRef={m => (this._block = m)}>
        {before}
        <div className={s.list} ref={i => (this._items = i)}>
          {this.renderItems()}
        </div>
        {after}
      </BodyPortal>
    );
  }
}
