/* @flow */

// styles
import styles from './accordion.css';

// libs
import { autobind } from 'core-decorators';
import classNames from 'classnames';
import React, { Component, Element } from 'react';

type Action = {
  name: string;
  handler: Function;
}

type Props = {
  actions: Array<Action>;
  title: string;
  placeholder: string;
  open: boolean;
  loading: boolean;
  editMode: boolean;
  onEditComplete: Function;
  onEditCancel: Function;
  titleWrapper?: (title: string) => Element<*>;
  resetOverflowTimeout: number;
  className?: string;
  contentClassName?: string;
  children?: Array<Element<*>>|Element<*>;
}

type State = {
  title: string;
  open: boolean;
}

export default class Accordion extends Component {

  props: Props;

  static defaultProps = {
    title: '',
    open: true,
    loading: false,
    editMode: false,
    placeholder: 'Enter text',
    actions: [],
    resetOverflowTimeout: 300,
  };

  state: State = {
    title: this.props.title,
    open: this.props.open,
  };

  mounted: bool;

  componentDidMount(): void {
    window.addEventListener('resize', this.handleResize);
    this.mounted = true;

    this.recalculateHeight();
  }

  componentWillUnmount(): void {
    window.removeEventListener('resize', this.handleResize);
    this.mounted = false;
  }

  componentDidUpdate(prevProps: Props, prevState: State): void {
    this.recalculateHeight(this.state.open !== prevState.open);
  }

  componentWillReceiveProps(nextProps: Props) {
    if (!this.state.open && nextProps.open) {
      this.setState({ open: nextProps.open });
    }
  }

  recalculateHeight(visibilityChanged: boolean = false): void {
    let maxHeight = 0;
    let overflow = 'hidden';

    if (visibilityChanged) {
      setTimeout(() => {
        if (!this.mounted) {
          return;
        }

        this.refs.content.style.overflow = 'visible';
      }, this.props.resetOverflowTimeout);
    }

    if (this.state.open) {
      maxHeight = this.refs.content.scrollHeight;

      if (!visibilityChanged) {
        overflow = 'visible';
      }
    }

    this.refs.content.style.maxHeight = `${maxHeight}px`;
    this.refs.content.style.overflow = overflow;
  }

  @autobind
  handleResize() {
    this.recalculateHeight();
  }

  @autobind
  toggle(): void {
    this.setState({
      open: !this.state.open,
    });
  }

  @autobind
  onClick(e: MouseEvent) {
    e.stopPropagation();
  }

  @autobind
  onFocus({ target }: { target: HTMLInputElement }): void {
    /* set cursor to the end of the text */
    if (target.setSelectionRange) {
      const length = target.value.length * 2;

      target.setSelectionRange(length, length);
    } else {
      target.value = target.value;
    }
  }

  @autobind
  onChange({ target }: { target: HTMLInputElement }): void {
    this.setState({ title: target.value });
  }

  @autobind
  endEdit(): void {
    this.props.onEditComplete(this.state.title);
  }

  @autobind
  cancelEdit() {
    this.props.onEditCancel();
  }

  @autobind
  keyDown(event: KeyboardEvent) {
    if (event.keyCode == 13) {
      this.endEdit();
    }
    if (event.keyCode == 27) {
      this.cancelEdit();
    }
  }

  get title(): ?Element<*> {
    const { title, editMode, placeholder, titleWrapper } = this.props;
    if (!title && !editMode) {
      return null;
    }

    let titleElement: Element<*>;

    if (editMode) {
      titleElement = (
        <div className="fc-form-field">
          <input className={classNames(styles.input, {[styles.loading]: this.props.loading})}
                 autoFocus
                 type="text"
                 onClick={this.onClick}
                 onFocus={this.onFocus}
                 // onBlur={this.endEdit}
                 onChange={this.onChange}
                 onKeyDown={this.keyDown}
                 placeholder={placeholder}
                 value={this.state.title}
          />
        </div>
      );
    } else {
      titleElement = <span>{titleWrapper ? titleWrapper(title) : title}</span>;
    }

    return (
      <div className={styles.title}>
        <div className={styles.titleWrapper} onClick={this.toggle}>
          {titleElement}
        </div>
      </div>
    );
  }

  get controls() {
    return (
      <div className={styles.controls}>
        <div className={styles.left}>
          <span className={styles.controlItem} onClick={this.toggle}>
            <i className="icon-down" />
            <i className="icon-up" />
          </span>
        </div>
        <div className={styles.right}>
          {this.props.actions.map(({ name, handler }) => {
            return (
              <span className={styles.controlItem} key={name}>
                <i className={`icon-${name}`} onClick={handler} />
              </span>
            );
          })}
        </div>
      </div>
    );
  }

  render() {
    const { className, contentClassName } = this.props;

    const cls = classNames(styles.accordion, {
      [styles._open]: this.state.open,
    }, className);

    return (
      <div className={cls}>
        <div className={styles.header}>
          {this.title}
          {this.controls}
        </div>
        <div className={classNames(styles.content, contentClassName)} ref="content">
          {this.props.children}
        </div>
      </div>
    );
  }
}
