/* @flow */

// libs
import { isEmpty } from 'lodash';
import classNames from 'classnames';
import React, { Element } from 'react';

// styles
import s from './content-box.css';

type Props = {
  id: string,
  title: string | Element<any>,
  className: string,
  bodyClassName: string,
  actionBlock: Element<any>,
  children: Element<any>,
  footer: Element<any>,
  indentContent: boolean,
  renderContent: Function,
  viewContent: Element<any>,
}

export default (props: Props) => {
  let body = props.children;

  if (isEmpty(body)) {
    if (props.renderContent) {
      body = props.renderContent();
    } else if (props.viewContent) {
      body = props.viewContent;
    }
  }

  return (
    <div id={props.id} className={classNames(s.box, props.className)}>
      <header className={s.header}>
        <div className={s.title}>{props.title}</div>
        <div className={s.controls}>{props.actionBlock}</div>
      </header>
      <div className={classNames(s.body, props.bodyClassName)}>
        {body}
      </div>
      {props.footer}
    </div>
  );
};
