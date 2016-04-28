/* @flow */

// styles
import styles from './accordion.css';

// libs
import _ from 'lodash';
import { autobind } from 'core-decorators';
import classNames from 'classnames';
import React, { Component, Element } from 'react';

type Action = {
  name: string;
  handler: Function;
}

type Props = {
  children?: Array<Element>|Element;
  actions?: Array<Action>;
  title?: string;
  placeholder?: string;
  open?: boolean;
  editMode?: boolean;
  onEditComplete: Function;
  onEditCancel: Function;
  titleWrapper?: (title: string) => Element;
}

type State = {
  title: string;
  open: boolean;
}

export default class Accordion extends Component {

  static props: Props;

  static defaultProps = {
    open: true,
    editMode: false,
    placeholder: 'Enter text',
    actions: [],
  };

  state: State = {
    title: this.props.title,
    open: this.props.open,
  };

  componentWillReceiveProps(nextProps) {
    this.setState({
      title: nextProps.title,
    });
  }

  componentDidUpdate(): void {
    let maxHeight = 0;
    if (this.state.open) {
      maxHeight = this.refs.content.scrollHeight;
    }

    this.refs.content.style.maxHeight = `${maxHeight}px`;
  }

  @autobind
  toggle(): void {
    this.setState({
      open: !this.state.open,
    });
  }

  @autobind
  onFocus({ target }): void {
    /* set cursor to the end of the text */
    if (target.setSelectionRange) {
      const length = target.value.length * 2;

      target.setSelectionRange(length, length);
    } else {
      target.value = target.value;
    }
  }

  @autobind
  changeInput({ target }): void {
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
  keyDown(event) {
    if (event.keyCode == 13) {
      this.endEdit();
    }
    if (event.keyCode == 27) {
      this.cancelEdit();
    }
  }

  get title(): ?Element {
    const { title, editMode, placeholder, titleWrapper } = this.props;
    if (!title && !editMode) {
      return null;
    }

    let titleElement:Element;

    if (editMode) {
      titleElement = <div className="fc-form-field">
        <input
          autoFocus
          type="text"
          onFocus={this.onFocus}
          onBlur={this.endEdit}
          onChange={this.changeInput}
          onKeyDown={this.keyDown}
          placeholder={placeholder}
          value={this.state.title}
        />
      </div>;
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

  get controls(): Element {
    return (
      <div className={styles.controls}>
        <div className={styles.left}>
          <span className={styles.controlItem} onClick={this.toggle}>
            <i className="icon-up" />
            <i className="icon-down" />
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

  render(): Element {
    const cls = classNames(styles.accordion, {
      [styles._open]: this.state.open,
    });

    return (
      <div className={cls}>
        <div className={styles.header}>
          {this.title}
          {this.controls}
        </div>
        <div className={styles.content} ref="content">
          {this.props.children}
        </div>
      </div>
    );
  }
}
