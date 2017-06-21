/* @flow */

// libs
import React, { Element } from 'react';
import s from './category-item.css';

function wrapper(str: string, key: string, rex: RegExp): string | Element<*> {
  if (str && rex && str.search(rex) != -1) {
    return <span className={s.wrapper} key={key}>{str}</span>;
  }

  return str;
}

function highlight(str: string = '', query: string): Array<string | Element<*>> {
  if (!query) {
    return [str];
  }

  // To achieve case insensitive replacement, we user RegExp and 'i' flag
  const rex = new RegExp(`(${query})`, 'gi');

  return str.split(rex).filter(v => v).map((str, i) => wrapper(str, `${str}${i}`, rex));
}

type Model = {
  prefix: string,
  text: string,
};

type Props = {
  query: string,
  model: Model,
};

export const CategoryItem = ({ query, model }: Props): Element<*> => {
  const prefixElements = highlight(model.prefix, query);
  const valElements = highlight(model.text, query);

  return (
    <div className={s.item}>
      <div className={s.itemPrefix}>{prefixElements}</div>
      <div className={s.itemValue}>{valElements}</div>
    </div>
  );
};
