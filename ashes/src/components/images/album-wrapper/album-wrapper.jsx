/* @flow */

// libs
import { autobind } from 'core-decorators';
import classNames from 'classnames';
import React, { Component, Element } from 'react';

// components
import ButtonWithMenu from 'components/core/button-with-menu';
import Icon from 'components/core/icon';

// styles
import s from './album-wrapper.css';

type Action = {
  name: string;
  handler: Function;
};

type Props = {
  actions: Array<Action>;
  title: string;
  onAddFile: Function;
  onAddUrl: Function;
  titleWrapper?: (title: string) => Element<*>;
  className?: string;
  contentClassName: ?string;
  alert: ?Element<any>,
  children?: Array<Element<*>>|Element<*>;
};

const ddItems = [
  ['fromDesk', 'Upload from desktop'],
  ['fromLink', 'Upload from Link']
];

export default class AlbumWrapper extends Component {

  props: Props;

  static defaultProps = {
    title: '',
    actions: [],
    onAddFile: () => {},
  };

  get title() {
    const { title, titleWrapper } = this.props;

    return (
      <div className={s.title}>
        <div className={s.titleWrapper}>
          <span>{titleWrapper ? titleWrapper(title) : title}</span>
        </div>
      </div>
    );
  }

  get controls() {
    return (
      <div className={s.controls}>
        <div className={s.right}>
          {this.props.actions.map(({ name, handler }) => {
            return (
              <span className={s.controlItem} key={name}>
                <Icon name={name} onClick={handler} />
              </span>
            );
          })}
        </div>
      </div>
    );
  }

  @autobind
  handleAdd(actionName: 'fromDesk' | 'fromLink'): void {
    switch (actionName) {
      case 'fromDesk':
        this.props.onAddFile();
        break;
      case 'fromLink':
        this.props.onAddUrl();
        break;
    }
  }

  render() {
    const { className, contentClassName, alert } = this.props;
    const cls = classNames(s.accordion, className);

    return (
      <div className={cls}>
        <div className={s.header}>
          {this.title}
          {this.controls}
        </div>
        {alert}
        <div className={classNames(s.content, contentClassName)}>
          <div className={s.menu}>
            <ButtonWithMenu
              title="Upload Media"
              icon="upload"
              items={ddItems}
              onSelect={this.handleAdd}
              onPrimaryClick={this.props.onAddFile}
            />
          </div>
          {this.props.children}
        </div>
      </div>
    );
  }
}
