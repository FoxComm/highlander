// libs
import React from 'react';
import s from './category-item.css';

function wrapper(str, key, rex) {
  if (rex && str.search(rex) != -1) {
    return <span style={{color: '#3fc1ab'}} key={key}>{str}</span>;
  }

  return str;
}

function highlight(str = '', query) {
  // To achieve case insensitive replacement, we user RegExp and 'i' flag
  const rex = new RegExp(`(${query})`, 'gi');

  return str.split(rex).filter(v => v).map((str, i) => wrapper(str, `${str}${i}`, rex));
}

export const CategoryItem = ({ query, model }) => {
  const prefixElements = highlight(model.prefix, query);
  const valElements = highlight(model.text, query);

  return (
    <div className={s.item}>
      <div className={s.itemPrefix}>{prefixElements}</div>
      <div className={s.itemValue}>{valElements}</div>
    </div>
  );
};
