// @flow

import React, { Element } from 'react';
import type { FilterTypeProps } from './types';
import styles from './filter-colors.css';


const colorStringToNumber = (color: string) => {
  if (color.length != 7 || color[0] != '#') {
    console.error(`Invalid format for color: ${color}`);
    return [ 255, 255, 255 ];
  }

  return [
    parseInt(`0x${color.substring(1, 3)}`),
    parseInt(`0x${color.substring(3, 5)}`),
    parseInt(`0x${color.substring(5, 7)}`),
  ]
};
  
/**
 * Programmatically computes the foreground color (black vs white) based on
 * the backgound color based on a formula the W3C recommends. Details of the
 * algorithm: http://stackoverflow.com/questions/3942878/how-to-decide-font-color-in-white-or-black-depending-on-background-color.
 */
const computeTextColor = (backgroundColor: string): string => {
  const [ red, green, blue ] = colorStringToNumber(backgroundColor);
  if ((red * 0.299 + green * 0.587 + blue * 0.114) > 186) {
    return '#000000';
  } else {
    return '#FFFFFF';
  }
};

const FilterColors = (props: FilterTypeProps): Element<*> => {
  const term = (props.term || '').toUpperCase();
  const {
    onSelectFacet = (a, b, c) => {},
    values = [],
  } = props;

  const swatches = values.map((filterValue) => {
    const { label, selected, value } = filterValue;
    const onSelect = () => onSelectFacet(term, value.value, !selected);

    const backgroundColor = value.color;
    const color = selected
      ? computeTextColor(backgroundColor)
      : backgroundColor;

    const style = {
      backgroundColor,
      color,
    };
    
    return (
      <input
        styleName="filter-checkbox"
        style={style}
        type="checkbox"
        name={label}
        checked={selected}
        value={selected}
        onChange={onSelect}
      />
    );
  });
    
  return (
    <form styleName="filter-block">
      {swatches}
    </form>
  );
};

export default FilterColors;
